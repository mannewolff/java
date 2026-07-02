import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanListView from './KanbanListView';
import type { KanbanBoard, KanbanItem } from '../../api/kanban';

vi.mock('../../api/kanban', async () => {
  const actual = await vi.importActual<typeof import('../../api/kanban')>('../../api/kanban');
  return { ...actual, listKanbanItems: vi.fn() };
});

import { listKanbanItems } from '../../api/kanban';

// Leichtgewichtiger Modal-Stub — wir testen nur die Verdrahtung aus der Liste.
vi.mock('./KanbanDetailModal', () => ({
  default: ({ open, item }: { open: boolean; item: KanbanItem }) =>
    open ? <div role="dialog" aria-label="Detail-Stub">Detail: {item.title} #{item.number}</div> : null,
}));

const listItems = listKanbanItems as ReturnType<typeof vi.fn>;

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
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('zeigt einen Ladezustand, bis die Items da sind', async () => {
    let resolve!: (b: KanbanBoard) => void;
    listItems.mockReturnValue(new Promise<KanbanBoard>((r) => (resolve = r)));

    render(<KanbanListView retentionDays={5} />);

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    resolve(boardOf());
    await waitFor(() => expect(screen.queryByRole('progressbar')).not.toBeInTheDocument());
  });

  it('zeigt eine Fehler-Alert, wenn das Laden scheitert', async () => {
    listItems.mockRejectedValue(new Error('boom'));
    render(<KanbanListView retentionDays={5} />);
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  it('zeigt Zeile mit Nummer, Badge, Titel und gestripptem Excerpt', async () => {
    listItems.mockResolvedValue(
      boardOf({ READY: [makeItem({ id: 7, number: 7, column: 'READY', title: 'Startklar', body: '# H **fett**' })] }),
    );
    render(<KanbanListView retentionDays={5} />);

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
    render(<KanbanListView retentionDays={5} />);

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
    render(<KanbanListView retentionDays={5} />);

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
    render(<KanbanListView retentionDays={5} />);

    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Detail öffnen: Klickbar/ }));

    expect(screen.getByRole('dialog', { name: 'Detail-Stub' })).toHaveTextContent('Detail: Klickbar #3');
  });

  it('zeigt einen Empty-State, wenn der Filter nichts übrig lässt', async () => {
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
    );
    render(<KanbanListView retentionDays={5} />);

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
    const { container } = render(<KanbanListView retentionDays={5} />);

    await screen.findByText('X');
    const view = container.querySelector('[data-excerpt-width]');
    expect(view).toHaveAttribute('data-excerpt-width', '75');
  });
});
