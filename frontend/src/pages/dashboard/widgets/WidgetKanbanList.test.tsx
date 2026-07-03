import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetKanbanList from './WidgetKanbanList';
import type { WidgetDto } from '../../../api/dashboard';
import type { KanbanBoard, KanbanItem } from '../../../api/kanban';
import { STATUS_COLORS } from '../../kanban/statusColors';

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
  return { BACKLOG: [], READY: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] };
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
    number: id,
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

  it('sortiert absteigend nach Issue-Nummer und begrenzt auf limit (#221)', async () => {
    const board = emptyBoard();
    // number = id; bewusst verdrehte Array-Reihenfolge. Höhere Nummer muss oben stehen.
    board.BACKLOG = [makeItem(1, 'Niedrig', 0), makeItem(2, 'Hoch', 1)];
    listItems.mockResolvedValue(board);

    render(<WidgetKanbanList widget={widget({ column: 'BACKLOG', limit: 1 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(await screen.findByRole('button', { name: 'Hoch' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Niedrig' })).not.toBeInTheDocument();
  });

  it('ordnet Spalten In Review → In Progress → Backlog, je Spalte Nummer absteigend (#221)', async () => {
    const board = emptyBoard();
    board.BACKLOG = [
      { ...makeItem(10, 'Backlog-10', 0), column: 'BACKLOG', number: 10 },
      { ...makeItem(20, 'Backlog-20', 1), column: 'BACKLOG', number: 20 },
    ];
    board.IN_PROGRESS = [{ ...makeItem(5, 'Progress-5', 0), column: 'IN_PROGRESS', number: 5 }];
    board.IN_REVIEW = [{ ...makeItem(1, 'Review-1', 0), column: 'IN_REVIEW', number: 1 }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG', 'IN_PROGRESS', 'IN_REVIEW'], limit: 10 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    await screen.findByRole('button', { name: 'Review-1' });
    const titles = screen
      .getAllByRole('button')
      .map((b) => b.textContent)
      .filter((t): t is string => t != null && /(Review|Progress|Backlog)-/.test(t));
    // In Review zuerst, dann In Progress, dann Backlog (innerhalb Backlog: 20 vor 10).
    expect(titles).toEqual(['Review-1', 'Progress-5', 'Backlog-20', 'Backlog-10']);
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

describe('WidgetKanbanList 4-Spalten-Layout (#191)', () => {
  beforeEach(() => {
    listItems.mockReset();
    settings.mockReset();
    updateItem.mockReset();
    listItems.mockResolvedValue(emptyBoard());
    settings.mockResolvedValue({ doneRetentionDays: 5 });
    updateItem.mockResolvedValue(makeItem(1, 'x', 0));
  });
  afterEach(() => cleanup());

  it('zeigt Status-Icon, Nummer, Titel und Body je Item', async () => {
    const board = emptyBoard();
    board.BACKLOG = [{ ...makeItem(1, 'Erstes', 0, 'Mein Body'), column: 'BACKLOG', number: 7 }];
    board.IN_REVIEW = [{ ...makeItem(2, 'Zweites', 0), column: 'IN_REVIEW', number: 8 }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG', 'IN_REVIEW'], limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    const li = (await screen.findByRole('button', { name: 'Erstes' })).closest('li')!;
    // Status-Icon (Backlog) + Nummer + Body in derselben Zeile.
    expect(li.querySelector('[data-testid="InboxIcon"]')).not.toBeNull();
    expect(li.textContent).toContain('#7');
    expect(li.textContent).toContain('Mein Body');

    const li2 = screen.getByRole('button', { name: 'Zweites' }).closest('li')!;
    expect(li2.querySelector('[data-testid="VisibilityIcon"]')).not.toBeNull();
    expect(li2.textContent).toContain('#8');
  });

  it('blendet die Nummer aus, wenn sie 0 ist', async () => {
    const board = emptyBoard();
    board.BACKLOG = [{ ...makeItem(1, 'Ohne', 0), column: 'BACKLOG', number: 0 }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList widget={widget({ columns: ['BACKLOG'], limit: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />,
    );

    const li = (await screen.findByRole('button', { name: 'Ohne' })).closest('li')!;
    expect(li.textContent).not.toContain('#');
  });

  it('färbt Spalten-Icons mit STATUS_COLORS.dot (konsistent mit Board-Header, #288)', async () => {
    const board = emptyBoard();
    board.BACKLOG = [{ ...makeItem(1, 'B', 0), column: 'BACKLOG' }];
    board.READY = [{ ...makeItem(2, 'R', 0), column: 'READY' }];
    board.DONE = [{ ...makeItem(3, 'D', 0), column: 'DONE' }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG', 'READY', 'DONE'], limit: 10 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    await screen.findByRole('button', { name: 'B' });
    expect(screen.getByTestId('InboxIcon')).toHaveStyle({ color: STATUS_COLORS.BACKLOG.dot });
    expect(screen.getByTestId('FlagIcon')).toHaveStyle({ color: STATUS_COLORS.READY.dot });
    expect(screen.getByTestId('CheckCircleIcon')).toHaveStyle({ color: STATUS_COLORS.DONE.dot });
  });
});

describe('WidgetKanbanList Inline-Layout (#172)', () => {
  beforeEach(() => {
    listItems.mockReset();
    settings.mockReset();
    updateItem.mockReset();
    listItems.mockResolvedValue(emptyBoard());
    settings.mockResolvedValue({ doneRetentionDays: 5 });
    updateItem.mockResolvedValue(makeItem(1, 'x', 0));
  });
  afterEach(() => cleanup());

  it('zeigt das Spalten-Label inline vor dem Titel', async () => {
    const board = emptyBoard();
    board.BACKLOG = [{ ...makeItem(1, 'Erstes', 0), column: 'BACKLOG' }];
    board.IN_REVIEW = [{ ...makeItem(2, 'Zweites', 0), column: 'IN_REVIEW' }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG', 'IN_REVIEW'], limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    const titleLink = await screen.findByRole('button', { name: 'Erstes' });
    const li = titleLink.closest('li');
    expect(li).not.toBeNull();
    // Seit #191: Spalte wird per Status-Icon (mit aria-label) statt Textlabel dargestellt.
    expect(li?.querySelector('[aria-label="Backlog"]')).not.toBeNull();
    expect(li?.textContent).toContain('Erstes');
    // Zweites Item zeigt sein eigenes Status-Icon.
    const li2 = screen.getByRole('button', { name: 'Zweites' }).closest('li');
    expect(li2?.querySelector('[aria-label="In Review"]')).not.toBeNull();
  });

  it('der Titel bleibt die zugängliche Beschriftung des Links (nicht das Spalten-Label)', async () => {
    const board = emptyBoard();
    board.BACKLOG = [{ ...makeItem(1, 'Nur Titel', 0), column: 'BACKLOG' }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG'], limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    // Accessible name = Titel, nicht "Backlog – Nur Titel".
    expect(await screen.findByRole('button', { name: 'Nur Titel' })).toBeInTheDocument();
  });
});

describe('WidgetKanbanList Striped-Rows (#173)', () => {
  beforeEach(() => {
    listItems.mockReset();
    settings.mockReset();
    updateItem.mockReset();
    listItems.mockResolvedValue(emptyBoard());
    settings.mockResolvedValue({ doneRetentionDays: 5 });
    updateItem.mockResolvedValue(makeItem(1, 'x', 0));
  });
  afterEach(() => cleanup());

  it('rendert benachbarte Items unterschiedlich, jedes zweite gleich (Zebra)', async () => {
    const board = emptyBoard();
    board.BACKLOG = [
      makeItem(1, 'A', 0),
      makeItem(2, 'B', 1),
      makeItem(3, 'C', 2),
      makeItem(4, 'D', 3),
    ];
    listItems.mockResolvedValue(board);

    const { container } = render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG'], limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    await screen.findByRole('button', { name: 'A' });
    const lis = Array.from(container.querySelectorAll('li'));
    expect(lis).toHaveLength(4);
    // Gerade vs. ungerade Zeilen tragen unterschiedliche Hintergrund-Klassen (Emotion-Hash),
    // jede zweite Zeile teilt sich die Klasse → alternierendes Muster.
    expect(lis[0].className).toBe(lis[2].className);
    expect(lis[1].className).toBe(lis[3].className);
    expect(lis[0].className).not.toBe(lis[1].className);
  });

  it('rendert ein einzelnes Item ohne Crash', async () => {
    const board = emptyBoard();
    board.BACKLOG = [makeItem(1, 'Solo', 0)];
    listItems.mockResolvedValue(board);

    const { container } = render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG'], limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    await screen.findByRole('button', { name: 'Solo' });
    expect(container.querySelectorAll('li')).toHaveLength(1);
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

  it('zeigt Items aus mehreren Spalten, sortiert nach Anzeige-Reihenfolge + Nummer, auf Gesamt-Limit (#221)', async () => {
    const board = emptyBoard();
    board.BACKLOG = [{ ...makeItem(1, 'B0', 0), column: 'BACKLOG', number: 1 }];
    board.IN_REVIEW = [
      { ...makeItem(2, 'R0', 0), column: 'IN_REVIEW', number: 2 },
      { ...makeItem(3, 'R1', 1), column: 'IN_REVIEW', number: 3 },
    ];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['BACKLOG', 'IN_REVIEW'], limit: 2 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    // Gesamt-Limit 2: IN_REVIEW zuerst, Nummer absteigend → R1, R0; B0 (Backlog) fällt raus.
    expect(await screen.findByRole('button', { name: 'R1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'R0' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'B0' })).not.toBeInTheDocument();
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

  it('zeigt Items aus der READY-Spalte mit Flag-Icon (#280)', async () => {
    const board = emptyBoard();
    board.READY = [{ ...makeItem(1, 'Startklar', 0), column: 'READY', number: 3 }];
    listItems.mockResolvedValue(board);

    render(
      <WidgetKanbanList
        widget={widget({ columns: ['READY'], limit: 5 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    const li = (await screen.findByRole('button', { name: 'Startklar' })).closest('li')!;
    expect(li.querySelector('[data-testid="FlagIcon"]')).not.toBeNull();
  });

  it('Drawer: READY ist als Spalte auswählbar', async () => {
    listItems.mockResolvedValue(emptyBoard());
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetKanbanList widget={widget({ columns: ['BACKLOG'], limit: 5 })} onChange={onChange} onDelete={vi.fn()} />,
    );
    await user.click(screen.getByRole('button', { name: 'Kanban-Liste bearbeiten' }));
    await user.click(screen.getByRole('checkbox', { name: 'Ready' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as { columns: string[] };
    expect(parsed.columns).toEqual(['BACKLOG', 'READY']);
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
