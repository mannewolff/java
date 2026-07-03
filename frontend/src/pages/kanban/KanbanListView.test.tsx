import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanListView from './KanbanListView';
import type { KanbanBoard, KanbanItem } from '../../api/kanban';
import { NotifyProvider } from '../../notify/NotifyProvider';

vi.mock('../../api/kanban', async () => {
  const actual = await vi.importActual<typeof import('../../api/kanban')>('../../api/kanban');
  return { ...actual, listKanbanItems: vi.fn(), moveKanbanItem: vi.fn(), updateKanbanItem: vi.fn() };
});

import { listKanbanItems, moveKanbanItem, updateKanbanItem } from '../../api/kanban';

// Leichtgewichtiger Modal-Stub — wir testen nur die Verdrahtung aus der Liste.
// Der "Speichern"-Button ruft onSubmit mit Test-Werten auf, damit
// handleDetailSubmit (inkl. Fehlerbehandlung) getestet werden kann.
vi.mock('./KanbanDetailModal', () => ({
  default: ({
    open,
    item,
    onSubmit,
  }: {
    open: boolean;
    item: KanbanItem;
    onSubmit: (title: string, body: string) => Promise<void> | void;
  }) =>
    open ? (
      <div role="dialog" aria-label="Detail-Stub">
        Detail: {item.title} #{item.number}
        <button onClick={() => onSubmit('Neuer Titel', 'Neuer Body')}>Speichern</button>
      </div>
    ) : null,
}));

const listItems = listKanbanItems as ReturnType<typeof vi.fn>;
const moveItem = moveKanbanItem as ReturnType<typeof vi.fn>;
const updateItem = updateKanbanItem as ReturnType<typeof vi.fn>;

function dragEvent(): { dataTransfer: { setData: ReturnType<typeof vi.fn>; effectAllowed: string } } {
  return { dataTransfer: { setData: vi.fn(), effectAllowed: '' } };
}

function installMemoryLocalStorage(): void {
  const store = new Map<string, string>();
  const mock = {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => void store.set(k, String(v)),
    removeItem: (k: string) => void store.delete(k),
    clear: () => store.clear(),
    key: (i: number) => [...store.keys()][i] ?? null,
    get length() {
      return store.size;
    },
  };
  vi.stubGlobal('localStorage', mock);
}

function makeItem(overrides: Partial<KanbanItem> = {}): KanbanItem {
  return {
    id: 1,
    title: 'Titel',
    body: '',
    column: 'BACKLOG',
    position: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    archived: false,
    number: 1,
    ...overrides,
  };
}

function boardOf(input: Partial<Record<string, KanbanItem[]>> = {}): KanbanBoard {
  return {
    BACKLOG: [],
    READY: [],
    IN_PROGRESS: [],
    IN_REVIEW: [],
    DONE: [],
    ...input,
  } as KanbanBoard;
}

describe('KanbanListView', () => {
  beforeEach(() => {
    installMemoryLocalStorage();
    listItems.mockReset();
    listItems.mockResolvedValue(boardOf());
    moveItem.mockReset();
    moveItem.mockResolvedValue(makeItem());
    updateItem.mockReset();
    updateItem.mockResolvedValue(makeItem());
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('zeigt einen Ladezustand, bis die Items da sind', async () => {
    let resolve!: (b: KanbanBoard) => void;
    listItems.mockReturnValue(new Promise<KanbanBoard>((r) => (resolve = r)));

    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    resolve(boardOf());
    await waitFor(() => expect(screen.queryByRole('progressbar')).not.toBeInTheDocument());
  });

  it('zeigt eine Fehler-Alert, wenn das Laden scheitert', async () => {
    listItems.mockRejectedValue(new Error('boom'));
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  it('zeigt Zeile mit Nummer, Badge, Titel und gestripptem Excerpt', async () => {
    listItems.mockResolvedValue(
      boardOf({ READY: [makeItem({ id: 7, number: 7, column: 'READY', title: 'Startklar', body: '# H **fett**' })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const row = await screen.findByRole('button', { name: /Detail öffnen: Startklar/ });
    expect(within(row).getByText('#7')).toBeInTheDocument();
    expect(within(row).getByText('Ready')).toBeInTheDocument();
    expect(within(row).getByText('Startklar')).toBeInTheDocument();
    expect(within(row).getByText('H fett')).toBeInTheDocument();
  });

  it('blendet Status über Filter-Chips aus', async () => {
    listItems.mockResolvedValue(
      boardOf({
        BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })],
        DONE: [makeItem({ id: 2, number: 2, column: 'DONE', title: 'Done-Item' })],
      }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    await screen.findByText('Backlog-Item');
    expect(screen.getByText('Done-Item')).toBeInTheDocument();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Filter Done' }));

    expect(screen.queryByText('Done-Item')).not.toBeInTheDocument();
    expect(screen.getByText('Backlog-Item')).toBeInTheDocument();
  });

  it('lädt archivierte Items nach, wenn der Archiv-Chip aktiviert wird', async () => {
    listItems.mockResolvedValueOnce(boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Aktiv' })] }));
    listItems.mockResolvedValueOnce(
      boardOf({
        BACKLOG: [makeItem({ id: 1, number: 1, title: 'Aktiv' })],
        DONE: [makeItem({ id: 9, number: 9, column: 'DONE', title: 'Archiviert-Item', archived: true })],
      }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    await screen.findByText('Aktiv');
    expect(listItems).toHaveBeenLastCalledWith(false);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Filter Archiv' }));

    await waitFor(() => expect(listItems).toHaveBeenLastCalledWith(true));
    expect(await screen.findByText('Archiviert-Item')).toBeInTheDocument();
  });

  it('öffnet das Detail-Modal beim Klick auf eine Zeile', async () => {
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 3, number: 3, title: 'Klickbar' })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Detail öffnen: Klickbar/ }));

    expect(screen.getByRole('dialog', { name: 'Detail-Stub' })).toHaveTextContent('Detail: Klickbar #3');
  });

  it('zeigt eine Fehler-Notification, wenn das Speichern aus dem Detail-Modal fehlschlägt (#292)', async () => {
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 3, number: 3, title: 'Klickbar' })] }),
    );
    updateItem.mockRejectedValue(new Error('boom'));
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Detail öffnen: Klickbar/ }));
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'Detail-Stub' })).toBeInTheDocument();
  });

  it('zeigt einen Empty-State, wenn der Filter nichts übrig lässt', async () => {
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    await screen.findByText('Backlog-Item');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Filter Backlog' }));

    expect(screen.getByText('Keine Items')).toBeInTheDocument();
  });

  it('liest die Excerpt-Breite aus localStorage und klemmt sie auf 25–75 %', async () => {
    localStorage.setItem('kanban.listExcerptWidth', '999');
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'X' })] }),
    );
    const { container } = render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    await screen.findByText('X');
    const view = container.querySelector('[data-excerpt-width]');
    expect(view).toHaveAttribute('data-excerpt-width', '75');
  });

  it('verschiebt ein Item per Drag&Drop innerhalb desselben Status (#283)', async () => {
    listItems.mockResolvedValue(
      boardOf({
        BACKLOG: [
          makeItem({ id: 1, number: 1, title: 'Erstes', position: 0 }),
          makeItem({ id: 2, number: 2, title: 'Zweites', position: 1 }),
          makeItem({ id: 3, number: 3, title: 'Drittes', position: 2 }),
        ],
      }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const first = await screen.findByRole('button', { name: /Detail öffnen: Erstes/ });
    const third = screen.getByRole('button', { name: /Detail öffnen: Drittes/ });

    fireEvent.dragStart(first, dragEvent());
    fireEvent.dragOver(third, dragEvent());
    fireEvent.drop(third, dragEvent());

    await waitFor(() => expect(moveItem).toHaveBeenCalledWith(1, 'BACKLOG', 2));
  });

  it('Drop auf fremden Status löst keinen API-Call aus (#283)', async () => {
    listItems.mockResolvedValue(
      boardOf({
        BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item', position: 0 })],
        READY: [makeItem({ id: 2, number: 2, column: 'READY', title: 'Ready-Item', position: 0 })],
      }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const backlogRow = await screen.findByRole('button', { name: /Detail öffnen: Backlog-Item/ });
    const readyRow = screen.getByRole('button', { name: /Detail öffnen: Ready-Item/ });

    fireEvent.dragStart(backlogRow, dragEvent());
    fireEvent.dragOver(readyRow, dragEvent());
    fireEvent.drop(readyRow, dragEvent());

    expect(moveItem).not.toHaveBeenCalled();
  });

  it('archivierte Zeilen sind nicht per Drag verschiebbar (#283)', async () => {
    listItems.mockResolvedValueOnce(boardOf());
    listItems.mockResolvedValueOnce(
      boardOf({ DONE: [makeItem({ id: 9, number: 9, column: 'DONE', title: 'Archiv-Item', archived: true })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Filter Archiv' }));

    const row = await screen.findByRole('button', { name: /Detail öffnen: Archiv-Item/ });
    expect(row).toHaveAttribute('draggable', 'false');
  });

  it('Klick nach einem Drag öffnet nicht versehentlich das Modal', async () => {
    listItems.mockResolvedValue(
      boardOf({
        BACKLOG: [
          makeItem({ id: 1, number: 1, title: 'Erstes', position: 0 }),
          makeItem({ id: 2, number: 2, title: 'Zweites', position: 1 }),
        ],
      }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const first = await screen.findByRole('button', { name: /Detail öffnen: Erstes/ });
    const second = screen.getByRole('button', { name: /Detail öffnen: Zweites/ });

    fireEvent.dragStart(first, dragEvent());
    fireEvent.dragOver(second, dragEvent());
    fireEvent.drop(second, dragEvent());
    fireEvent.dragEnd(first);

    expect(screen.queryByRole('dialog', { name: 'Detail-Stub' })).not.toBeInTheDocument();
  });
});
