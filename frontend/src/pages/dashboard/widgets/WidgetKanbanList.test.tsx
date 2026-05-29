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

function makeItem(id: number, title: string, position: number): KanbanItem {
  return {
    id,
    title,
    body: '',
    column: 'BACKLOG',
    position,
    createdAt: '2026-05-29T00:00:00Z',
    updatedAt: '2026-05-29T00:00:00Z',
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
    await user.click(screen.getByRole('combobox', { name: 'Spalte' }));
    await user.click(screen.getByRole('option', { name: 'Done' }));

    const limitInput = screen.getByLabelText('Anzahl');
    await user.clear(limitInput);
    await user.type(limitInput, '8');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { column: string; limit: number };
    expect(parsed.column).toBe('DONE');
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
