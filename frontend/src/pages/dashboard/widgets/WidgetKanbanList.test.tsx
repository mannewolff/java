import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetKanbanList from './WidgetKanbanList';
import type { WidgetDto } from '../../../api/dashboard';
import type { KanbanBoard, KanbanItem } from '../../../api/kanban';

vi.mock('../../../api/kanban', async () => {
  const actual = await vi.importActual<typeof import('../../../api/kanban')>('../../../api/kanban');
  return {
    ...actual,
    listKanbanItems: vi.fn(),
    getKanbanSettings: vi.fn(),
    updateKanbanItem: vi.fn(),
  };
});

import { getKanbanSettings, listKanbanItems, updateKanbanItem } from '../../../api/kanban';

// Leichtgewichtiger Stub des Detail-Modals (#119) — wir testen hier nur die
// Verdrahtung des Widgets, nicht das Modal selbst.
vi.mock('../../kanban/KanbanDetailModal', () => ({
  default: ({
    open,
    item,
    onSubmit,
    onClose,
  }: {
    open: boolean;
    item: KanbanItem;
    retentionDays: number;
    onClose: () => void;
    onSubmit: (title: string, body: string) => void | Promise<void>;
  }) =>
    open ? (
      <div role="dialog" aria-label="Detail-Stub">
        <span>Detail: {item.title}</span>
        <button onClick={() => void onSubmit('Geänderter Titel', 'Body')}>stub-save</button>
        <button onClick={onClose}>stub-close</button>
      </div>
    ) : null,
}));

const listItems = listKanbanItems as ReturnType<typeof vi.fn>;
const settings = getKanbanSettings as ReturnType<typeof vi.fn>;
const updateItem = updateKanbanItem as ReturnType<typeof vi.fn>;

function emptyBoard(): KanbanBoard {
  return { BACKLOG: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] };
}

function makeItem(id: number, title: string, position: number, body = ''): KanbanItem {
  return {
    id,
    title,
    body,
    column: 'BACKLOG',
    position,
    createdAt: '2026-05-29T00:00:00Z',
    updatedAt: '2026-05-29T00:00:00Z',
    archived: false,
  };
}

function widget(config: object): WidgetDto {
  return {
    id: 1,
    type: 'KANBAN_LIST',
    posX: 0,
    posY: 0,
    width: 3,
    height: 4,
    config: JSON.stringify(config),
  };
}

describe('WidgetKanbanList', () => {
  beforeEach(() => {
    listItems.mockReset();
    settings.mockReset();
    updateItem.mockReset();
    listItems.mockResolvedValue(emptyBoard());
    settings.mockResolvedValue({ doneRetentionDays: 5 });
    updateItem.mockResolvedValue(makeItem(1, 'x', 0));
  });

  afterEach(() => cleanup());

  it('zeigt die Titel der konfigurierten Spalte', async () => {
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'Erstes', 0), makeItem(2, 'Zweites', 1)];
    listItems.mockResolvedValue(board);

    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(await screen.findByRole('button', { name: 'Erstes' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Zweites' })).toBeInTheDocument();
  });

  it('sortiert nach position und begrenzt auf limit', async () => {
    const board = emptyBoard();
    // Absichtlich verdrehte Array-Reihenfolge: position 1 vor position 0.
    board.BACKLOG = [makeItem(2, 'Zweites', 1), makeItem(1, 'Erstes', 0)];
    listItems.mockResolvedValue(board);

    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 1 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(await screen.findByRole('button', { name: 'Erstes' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Zweites' })).not.toBeInTheDocument();
  });

  it('zeigt "Keine Einträge" bei leerer Spalte', async () => {
    listItems.mockResolvedValue(emptyBoard());

    render(<WidgetKanbanList widget={widget({ column: 'IN_REVIEW', limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(await screen.findByText('Keine Einträge')).toBeInTheDocument();
  });

  it('zeigt eine Fehler-Alert, wenn das Laden scheitert', async () => {
    listItems.mockRejectedValue(new Error('boom'));

    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(await screen.findByText('Laden fehlgeschlagen')).toBeInTheDocument();
  });

  it('öffnet das Detail-Modal beim Klick auf einen Titel', async () => {
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'Erstes', 0)];
    listItems.mockResolvedValue(board);

    const user = userEvent.setup();
    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    await user.click(await screen.findByRole('button', { name: 'Erstes' }));
    expect(screen.getByText('Detail: Erstes')).toBeInTheDocument();
  });

  it('speichert über das Modal und lädt die Liste neu', async () => {
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'Erstes', 0)];
    listItems.mockResolvedValue(board);

    const user = userEvent.setup();
    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    await user.click(await screen.findByRole('button', { name: 'Erstes' }));
    await user.click(screen.getByText('stub-save'));

    await waitFor(() => expect(updateItem).toHaveBeenCalledWith(1, 'Geänderter Titel', 'Body'));
    await waitFor(() => expect(listItems).toHaveBeenCalledTimes(2));
  });

  it('öffnet Detail-Modal auch im Read-Modus, blendet aber Edit/Delete-Icons aus', async () => {
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'Erstes', 0)];
    listItems.mockResolvedValue(board);

    const user = userEvent.setup();
    render(
      <WidgetKanbanList
        widget={widget({ column: 'BACKLOG', limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
        readOnly
      />,
    );

    expect(screen.queryByRole('button', { name: 'Kanban-Liste bearbeiten' })).not.toBeInTheDocument();
    await user.click(await screen.findByRole('button', { name: 'Erstes' }));
    expect(screen.getByText('Detail: Erstes')).toBeInTheDocument();
  });

  it('speichert Spalte und Anzahl aus dem Konfigurationsdrawer', async () => {
    listItems.mockResolvedValue(emptyBoard());
    const onChange = vi.fn();

    const user = userEvent.setup();
    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 5 })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Kanban-Liste bearbeiten' }));
    // Backlog (Default) abwählen, Done anwählen.
    await user.click(screen.getByRole('checkbox', { name: 'Done' }));
    await user.click(screen.getByRole('checkbox', { name: 'Backlog' }));

    const limitInput = screen.getByLabelText('Anzahl');
    await user.clear(limitInput);
    await user.type(limitInput, '8');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { columns: string[]; limit: number };
    expect(parsed.columns).toEqual(['DONE']);
    expect(parsed.limit).toBe(8);
  });

  it('begrenzt die Anzahl beim Speichern auf den gültigen Bereich (max 20)', async () => {
    listItems.mockResolvedValue(emptyBoard());
    const onChange = vi.fn();

    const user = userEvent.setup();
    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 5 })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Kanban-Liste bearbeiten' }));
    const limitInput = screen.getByLabelText('Anzahl');
    await user.clear(limitInput);
    await user.type(limitInput, '50');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { limit: number };
    expect(parsed.limit).toBe(20);
  });

  it('speichert Rahmen und Hintergrundfarbe aus dem Darstellung-Abschnitt', async () => {
    listItems.mockResolvedValue(emptyBoard());
    const onChange = vi.fn();

    const user = userEvent.setup();
    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 5 })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Kanban-Liste bearbeiten' }));
    await user.click(screen.getByRole('checkbox', { name: 'Rahmen anzeigen' }));
    await user.type(screen.getByLabelText('Hintergrundfarbe (leer = transparent)'), '#1e1e1e');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { showBorder: boolean; backgroundColor?: string };
    expect(parsed.showBorder).toBe(true);
    expect(parsed.backgroundColor).toBe('#1e1e1e');
  });

  it('rendert mit Default-Spalte bei invalider Config ohne Crash', async () => {
    listItems.mockResolvedValue(emptyBoard());
    const w: WidgetDto = {
      type: 'KANBAN_LIST',
      posX: 0,
      posY: 0,
      width: 3,
      height: 4,
      config: 'kein-valides-json',
    };

    render(<WidgetKanbanList widget={w} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(await screen.findByText('Backlog')).toBeInTheDocument();
  });
});

describe('WidgetKanbanList Body-Vorschau (#167)', () => {
  beforeEach(() => {
    listItems.mockReset();
    settings.mockReset();
    updateItem.mockReset();
    listItems.mockResolvedValue(emptyBoard());
    settings.mockResolvedValue({ doneRetentionDays: 5 });
    updateItem.mockResolvedValue(makeItem(1, 'x', 0));
  });
  afterEach(() => cleanup());

  it('zeigt Title und Body-Vorschau des Items', async () => {
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'Mein Titel', 0, 'Eine kurze Beschreibung des Items')];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ column: 'BACKLOG', limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(await screen.findByRole('button', { name: 'Mein Titel' })).toBeInTheDocument();
    expect(screen.getByText('Eine kurze Beschreibung des Items')).toBeInTheDocument();
  });

  it('langer Body (>300 Zeichen) wird gerendert (Ellipsis via CSS)', async () => {
    const longBody = 'L'.repeat(350);
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'Titel', 0, longBody)];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ column: 'BACKLOG', limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(await screen.findByText(longBody)).toBeInTheDocument();
  });

  it('zeigt keinen Body-Knoten wenn der Body leer ist', async () => {
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'NurTitel', 0, '')];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ column: 'BACKLOG', limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    const title = await screen.findByRole('button', { name: 'NurTitel' });
    // Das <li> enthält nur den Titel-Link, keinen zusätzlichen Body-Text.
    const li = title.closest('li');
    expect(li).not.toBeNull();
    expect(li?.querySelectorAll('p').length).toBe(0);
  });
});

describe('WidgetKanbanList Multi-Spalten (#168)', () => {
  beforeEach(() => {
    listItems.mockReset();
    settings.mockReset();
    updateItem.mockReset();
    listItems.mockResolvedValue(emptyBoard());
    settings.mockResolvedValue({ doneRetentionDays: 5 });
    updateItem.mockResolvedValue(makeItem(1, 'x', 0));
  });
  afterEach(() => cleanup());

  it('Legacy-Config {column} wird migriert und lädt die Spalte', async () => {
    const board = emptyBoard();
    board.DONE = [{ ...makeItem(1, 'Fertig', 0), column: 'DONE' }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList widget={widget({ column: 'DONE', limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />,
    );

    expect(await screen.findByRole('button', { name: 'Fertig' })).toBeInTheDocument();
  });

  it('zeigt Items aus mehreren Spalten, sortiert nach Spalte+Position, auf Gesamt-Limit', async () => {
    const board = emptyBoard();
    board.BACKLOG = [{ ...makeItem(1, 'B0', 0), column: 'BACKLOG' }];
    board.IN_REVIEW = [
      { ...makeItem(2, 'R0', 0), column: 'IN_REVIEW' },
      { ...makeItem(3, 'R1', 1), column: 'IN_REVIEW' },
    ];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG', 'IN_REVIEW'], limit: 2 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    // Gesamt-Limit 2: BACKLOG vor IN_REVIEW (Spalten-Reihenfolge) → B0, R0; R1 fällt raus.
    expect(await screen.findByRole('button', { name: 'B0' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'R0' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'R1' })).not.toBeInTheDocument();
  });

  it('Drawer: mehrere Spalten ankreuzen wird als columns-Array gespeichert', async () => {
    listItems.mockResolvedValue(emptyBoard());
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetKanbanList widget={widget({ columns: ['BACKLOG'], limit: 5 })} onChange={onChange} onDelete={vi.fn()} />,
    );
    await user.click(screen.getByRole('button', { name: 'Kanban-Liste bearbeiten' }));
    await user.click(screen.getByRole('checkbox', { name: 'In Review' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as { columns: string[] };
    expect(parsed.columns).toEqual(['BACKLOG', 'IN_REVIEW']);
  });

  it('Drawer: ohne ausgewählte Spalte ist Übernehmen deaktiviert', async () => {
    listItems.mockResolvedValue(emptyBoard());
    const user = userEvent.setup();
    render(
      <WidgetKanbanList widget={widget({ columns: ['BACKLOG'], limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />,
    );
    await user.click(screen.getByRole('button', { name: 'Kanban-Liste bearbeiten' }));
    // Die einzige Spalte abwählen → Übernehmen disabled.
    await user.click(screen.getByRole('checkbox', { name: 'Backlog' }));
    expect(screen.getByRole('button', { name: 'Übernehmen' })).toBeDisabled();
  });
});
