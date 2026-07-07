import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanListView from './KanbanListView';
import type { KanbanBoard, KanbanItem } from '../../api/kanban';
import { NotifyProvider } from '../../notify/NotifyProvider';

vi.mock('../../api/kanban', async () => {
  const actual = await vi.importActual<typeof import('../../api/kanban')>('../../api/kanban');
  return {
    ...actual,
    listKanbanItems: vi.fn(),
    moveKanbanItem: vi.fn(),
    updateKanbanItem: vi.fn(),
    restoreKanbanItem: vi.fn(),
    forceDeleteKanbanItem: vi.fn(),
    getKanbanEpics: vi.fn(),
    getKanbanSettings: vi.fn(),
    updateKanbanSettings: vi.fn(),
  };
});

import {
  forceDeleteKanbanItem,
  getKanbanEpics,
  getKanbanSettings,
  listKanbanItems,
  moveKanbanItem,
  restoreKanbanItem,
  updateKanbanItem,
  updateKanbanSettings,
} from '../../api/kanban';

// Leichtgewichtiger Modal-Stub — wir testen nur die Verdrahtung aus der Liste.
// Der "Speichern"-Button ruft onSubmit mit Test-Werten auf, damit
// handleDetailSubmit (inkl. Fehlerbehandlung) getestet werden kann. Restore/Löschen
// werden nur angeboten, wenn der Parent die Callbacks durchreicht (#341).
vi.mock('./KanbanDetailModal', () => ({
  default: ({
    open,
    item,
    onSubmit,
    onRestore,
    onForceDelete,
  }: {
    open: boolean;
    item: KanbanItem;
    onSubmit: (title: string, body: string, parentId: number | null) => Promise<void> | void;
    onRestore?: () => Promise<void> | void;
    onForceDelete?: () => Promise<void> | void;
  }) =>
    open ? (
      <div role="dialog" aria-label="Detail-Stub">
        Detail: {item.title} #{item.number}
        <button onClick={() => onSubmit('Neuer Titel', 'Neuer Body', null)}>Speichern</button>
        {onRestore && <button onClick={() => void onRestore()}>Wiederherstellen</button>}
        {onForceDelete && <button onClick={() => void onForceDelete()}>Endgültig löschen</button>}
      </div>
    ) : null,
}));

const listItems = listKanbanItems as ReturnType<typeof vi.fn>;
const moveItem = moveKanbanItem as ReturnType<typeof vi.fn>;
const updateItem = updateKanbanItem as ReturnType<typeof vi.fn>;
const restoreItem = restoreKanbanItem as ReturnType<typeof vi.fn>;
const forceDeleteItem = forceDeleteKanbanItem as ReturnType<typeof vi.fn>;
const listEpics = getKanbanEpics as ReturnType<typeof vi.fn>;
const getSettings = getKanbanSettings as ReturnType<typeof vi.fn>;
const putSettings = updateKanbanSettings as ReturnType<typeof vi.fn>;

const ALL_COLUMNS = ['BACKLOG', 'READY', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];

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

/**
 * Spült die Promise-Microtasks der Initial-Loads (Board, Epics, Settings) durch, ohne die
 * gefakten Timer zu bewegen. Unter `vi.useFakeTimers()` hängt RTLs `findByText`-Polling, weil es
 * echte Timer erwartet — deshalb warten wir die Effekte hier explizit ab.
 */
async function flushEffects(): Promise<void> {
  await act(async () => {
    for (let i = 0; i < 5; i += 1) await Promise.resolve();
  });
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
    movedToDoneAt: null,
    archived: false,
    number: 1,
    type: 'ITEM',
    parentId: null,
    dependencies: [],
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
    restoreItem.mockReset();
    restoreItem.mockResolvedValue(makeItem());
    forceDeleteItem.mockReset();
    forceDeleteItem.mockResolvedValue(undefined);
    listEpics.mockReset();
    listEpics.mockResolvedValue([]);
    getSettings.mockReset();
    getSettings.mockResolvedValue({ doneRetentionDays: 5, activeFilters: ALL_COLUMNS });
    putSettings.mockReset();
    putSettings.mockResolvedValue({ doneRetentionDays: 5, activeFilters: ALL_COLUMNS });
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

  it('zeigt neben dem Status-Badge das Epic-Badge, wenn das Item einem Epic zugeordnet ist (#342)', async () => {
    listEpics.mockResolvedValue([
      {
        id: 42,
        number: 5,
        title: 'Workshop',
        body: '',
        type: 'EPIC' as const,
        shortcode: 'WS',
        progress: { done: 0, total: 0 },
      },
    ]);
    listItems.mockResolvedValue(
      boardOf({ READY: [makeItem({ id: 7, number: 7, column: 'READY', title: 'Zugeordnet', parentId: 42 })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const row = await screen.findByRole('button', { name: /Detail öffnen: Zugeordnet/ });
    expect(await within(row).findByLabelText('Epic WS')).toBeInTheDocument();
  });

  it('zeigt kein Epic-Badge, wenn das Item keinem Epic zugeordnet ist (#342)', async () => {
    listItems.mockResolvedValue(
      boardOf({ READY: [makeItem({ id: 7, number: 7, column: 'READY', title: 'Frei', parentId: null })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const row = await screen.findByRole('button', { name: /Detail öffnen: Frei/ });
    expect(within(row).queryByLabelText(/^Epic /)).not.toBeInTheDocument();
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

  it('stellt ein archiviertes Item aus dem Detail-Modal wieder her und entfernt es aus dem Archiv (#341)', async () => {
    const archived = makeItem({ id: 9, number: 9, column: 'DONE', title: 'Archiv-Item', archived: true });
    listItems.mockResolvedValueOnce(boardOf()); // initial (nicht-archiviert)
    listItems.mockResolvedValueOnce(boardOf({ DONE: [archived] })); // Archiv-Filter aktiv
    listItems.mockResolvedValue(boardOf()); // nach dem Wiederherstellen: Archiv leer
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Filter Archiv' }));
    await user.click(await screen.findByRole('button', { name: /Detail öffnen: Archiv-Item/ }));
    await user.click(screen.getByRole('button', { name: 'Wiederherstellen' }));

    expect(restoreItem).toHaveBeenCalledWith(9);
    await waitFor(() => expect(screen.queryByText('Archiv-Item')).not.toBeInTheDocument());
  });

  it('löscht ein archiviertes Item aus dem Detail-Modal endgültig (#341)', async () => {
    const archived = makeItem({ id: 9, number: 9, column: 'DONE', title: 'Archiv-Item', archived: true });
    listItems.mockResolvedValueOnce(boardOf()); // initial (nicht-archiviert)
    listItems.mockResolvedValueOnce(boardOf({ DONE: [archived] })); // Archiv-Filter aktiv
    listItems.mockResolvedValue(boardOf()); // nach dem Löschen: Archiv leer
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Filter Archiv' }));
    await user.click(await screen.findByRole('button', { name: /Detail öffnen: Archiv-Item/ }));
    await user.click(screen.getByRole('button', { name: 'Endgültig löschen' }));

    expect(forceDeleteItem).toHaveBeenCalledWith(9);
    await waitFor(() => expect(screen.queryByText('Archiv-Item')).not.toBeInTheDocument());
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

  it('lädt neu, wenn der reloadKey-Prop sich ändert (#308)', async () => {
    // Anfangs leer — die Liste zeigt "Keine Items".
    listItems.mockResolvedValueOnce(boardOf());
    const { rerender } = render(
      <NotifyProvider>
        <KanbanListView retentionDays={5} reloadKey={0} />
      </NotifyProvider>,
    );
    expect(await screen.findByText('Keine Items')).toBeInTheDocument();
    expect(listItems).toHaveBeenCalledTimes(1);

    // Nach einer Parent-Mutation liefert die API ein neues Item; der reloadKey-Wechsel
    // erzwingt das Neuladen ohne manuellen location.reload().
    listItems.mockResolvedValueOnce(
      boardOf({ BACKLOG: [makeItem({ id: 9, number: 9, title: 'Frisch angelegt' })] }),
    );
    rerender(
      <NotifyProvider>
        <KanbanListView retentionDays={5} reloadKey={1} />
      </NotifyProvider>,
    );

    expect(await screen.findByText('Frisch angelegt')).toBeInTheDocument();
    expect(listItems).toHaveBeenCalledTimes(2);
  });

  it('lädt die gespeicherte Filter-Auswahl und wendet sie an (#346)', async () => {
    // Server merkt sich: nur BACKLOG aktiv. Das Done-Item muss danach ausgeblendet sein.
    getSettings.mockResolvedValue({ doneRetentionDays: 5, activeFilters: ['BACKLOG'] });
    listItems.mockResolvedValue(
      boardOf({
        BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })],
        DONE: [makeItem({ id: 2, number: 2, column: 'DONE', title: 'Done-Item' })],
      }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    expect(await screen.findByText('Backlog-Item')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText('Done-Item')).not.toBeInTheDocument());
    // Der Done-Chip ist nach dem Laden nicht mehr aktiv.
    expect(screen.getByRole('button', { name: 'Filter Done' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('persistiert eine Filter-Änderung entprellt via updateKanbanSettings (#346)', async () => {
    vi.useFakeTimers();
    try {
      listItems.mockResolvedValue(
        boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
      );
      render(<NotifyProvider><KanbanListView retentionDays={7} /></NotifyProvider>);
      await flushEffects();

      // Zwei schnelle Klicks (Done aus, dann wieder an) -> nur EIN PUT durch die Entprellung.
      fireEvent.click(screen.getByRole('button', { name: 'Filter Done' }));
      fireEvent.click(screen.getByRole('button', { name: 'Filter Done' }));

      // Vor Ablauf des Debounce-Fensters ist noch nichts gespeichert.
      expect(putSettings).not.toHaveBeenCalled();
      act(() => {
        vi.advanceTimersByTime(500);
      });
      // PUT-Promise settlen lassen, sonst act-Warnung.
      await flushEffects();

      expect(putSettings).toHaveBeenCalledTimes(1);
      const [days, filters] = putSettings.mock.calls[0];
      expect(days).toBe(7);
      // Nach dem zweiten Klick ist Done wieder aktiv -> alle fünf Spalten gespeichert.
      expect(filters).toEqual(expect.arrayContaining(ALL_COLUMNS));
      expect(filters).not.toContain('archived');
    } finally {
      vi.useRealTimers();
    }
  });

  it('User-Auswahl gewinnt gegen einen spät eintreffenden Settings-Load (Race, #347)', async () => {
    // getKanbanSettings kommt bewusst erst NACH der User-Interaktion zurück.
    let resolveSettings!: (s: { doneRetentionDays: number; activeFilters: string[] }) => void;
    getSettings.mockReturnValue(
      new Promise((r) => {
        resolveSettings = r;
      }),
    );
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);
    await screen.findByText('Backlog-Item');

    const user = userEvent.setup();
    // User schaltet Ready aus, bevor der Load ankommt.
    await user.click(screen.getByRole('button', { name: 'Filter Ready' }));
    expect(screen.getByRole('button', { name: 'Filter Ready' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );

    // Spät eintreffender Load will alle fünf Spalten aktivieren — darf die Auswahl NICHT überschreiben.
    resolveSettings({ doneRetentionDays: 5, activeFilters: ALL_COLUMNS });
    await waitFor(() => expect(getSettings).toHaveBeenCalled());

    expect(screen.getByRole('button', { name: 'Filter Ready' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('ignoriert eine Antwort ohne activeFilters-Array ohne Crash und ohne Warnung (#347)', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    getSettings.mockResolvedValue({ doneRetentionDays: 5 });
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    await screen.findByText('Backlog-Item');
    await waitFor(() => expect(getSettings).toHaveBeenCalled());
    // Default: alle fünf Spalten-Chips aktiv, Archiv aus.
    for (const label of ['Backlog', 'Ready', 'In Progress', 'In Review', 'Done']) {
      expect(screen.getByRole('button', { name: `Filter ${label}` })).toHaveAttribute(
        'aria-pressed',
        'true',
      );
    }
    expect(screen.getByRole('button', { name: 'Filter Archiv' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    expect(warn).not.toHaveBeenCalled();
    warn.mockRestore();
  });

  it('wendet eine gespeicherte Auswahl mit Archiv an und lädt archivierte Items (#347)', async () => {
    getSettings.mockResolvedValue({
      doneRetentionDays: 5,
      activeFilters: ['BACKLOG', 'archived'],
    });
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    await screen.findByText('Backlog-Item');
    await waitFor(() => expect(listItems).toHaveBeenLastCalledWith(true));
    expect(screen.getByRole('button', { name: 'Filter Archiv' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('persistiert den Archiv-Filter (#347)', async () => {
    vi.useFakeTimers();
    try {
      listItems.mockResolvedValue(
        boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
      );
      render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);
      await flushEffects();

      fireEvent.click(screen.getByRole('button', { name: 'Filter Archiv' }));
      act(() => {
        vi.advanceTimersByTime(500);
      });
      // Reload (listItems(true)) und PUT-Promise settlen lassen, sonst act-Warnung.
      await flushEffects();

      expect(putSettings).toHaveBeenCalledTimes(1);
      expect(putSettings.mock.calls[0][1]).toContain('archived');
    } finally {
      vi.useRealTimers();
    }
  });

  it('räumt den Debounce-Timer bei Unmount ab (kein PUT, keine Warnung, #347)', async () => {
    vi.useFakeTimers();
    try {
      listItems.mockResolvedValue(
        boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
      );
      const { unmount } = render(
        <NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>,
      );
      await flushEffects();

      fireEvent.click(screen.getByRole('button', { name: 'Filter Done' }));
      unmount();
      act(() => {
        vi.runAllTimers();
      });

      expect(putSettings).not.toHaveBeenCalled();
    } finally {
      vi.useRealTimers();
    }
  });

  it('fängt einen Persistenz-Fehler still ab (kein Störer) (#346)', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    putSettings.mockRejectedValue(new Error('boom'));
    listItems.mockResolvedValue(
      boardOf({ BACKLOG: [makeItem({ id: 1, number: 1, title: 'Backlog-Item' })] }),
    );
    render(<NotifyProvider><KanbanListView retentionDays={5} /></NotifyProvider>);

    await screen.findByText('Backlog-Item');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Filter Ready' }));

    await waitFor(() => expect(putSettings).toHaveBeenCalled());
    // Keine Fehler-Alert/Snackbar, die Liste bleibt bedienbar.
    await waitFor(() => expect(warn).toHaveBeenCalled());
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByText('Backlog-Item')).toBeInTheDocument();
    warn.mockRestore();
  });

  it('lädt NICHT neu, wenn der reloadKey-Prop unverändert bleibt (keine Endlosschleife)', async () => {
    listItems.mockResolvedValue(boardOf());
    const { rerender } = render(
      <NotifyProvider>
        <KanbanListView retentionDays={5} reloadKey={3} />
      </NotifyProvider>,
    );
    await screen.findByText('Keine Items');
    expect(listItems).toHaveBeenCalledTimes(1);

    // Re-Render mit identischem reloadKey (z. B. anderer Parent-State) darf nicht nachladen.
    rerender(
      <NotifyProvider>
        <KanbanListView retentionDays={7} reloadKey={3} />
      </NotifyProvider>,
    );
    await Promise.resolve();
    expect(listItems).toHaveBeenCalledTimes(1);
  });
});
