import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DndContext } from '@dnd-kit/core';
import { SortableContext } from '@dnd-kit/sortable';

import KanbanCard from './KanbanCard';
import type { KanbanItem } from '../../api/kanban';
import { ARCHIVED_STATUS_COLOR } from './statusColors';

function makeItem(overrides: Partial<KanbanItem> = {}): KanbanItem {
  return {
    id: 1,
    title: 'Mein Titel',
    body: 'Beschreibung',
    column: 'BACKLOG',
    position: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    movedToDoneAt: null,
    archived: false,
    number: 1,
    ...overrides,
  };
}

function renderCard(item: KanbanItem, handlers: {
  onOpenDetail?: (i: KanbanItem) => void;
  onEdit?: (i: KanbanItem) => void;
  onArchive?: (i: KanbanItem) => void;
  onRestore?: (i: KanbanItem) => void;
  onForceDelete?: (i: KanbanItem) => void;
  onMove?: (i: KanbanItem, c: KanbanItem['column']) => void;
} = {}) {
  return render(
    <DndContext>
      <SortableContext items={[item.id]}>
        <KanbanCard
          item={item}
          retentionDays={5}
          onOpenDetail={handlers.onOpenDetail ?? vi.fn()}
          onEdit={handlers.onEdit ?? vi.fn()}
          onArchive={handlers.onArchive ?? vi.fn()}
          onRestore={handlers.onRestore ?? vi.fn()}
          onForceDelete={handlers.onForceDelete ?? vi.fn()}
          onMove={handlers.onMove ?? vi.fn()}
        />
      </SortableContext>
    </DndContext>,
  );
}

describe('KanbanCard', () => {
  afterEach(() => cleanup());

  it('zeigt die Item-Nummer vor dem Titel (#188)', () => {
    renderCard(makeItem({ number: 42, title: 'Mein Titel' }));
    const heading = screen.getByRole('button', { name: 'Detail öffnen: Mein Titel' });
    expect(heading).toHaveTextContent('#42');
    expect(heading).toHaveTextContent('Mein Titel');
  });

  it('blendet die Nummer aus, wenn sie 0 ist (Legacy/unvergeben)', () => {
    renderCard(makeItem({ number: 0, title: 'Ohne Nummer' }));
    const heading = screen.getByRole('button', { name: 'Detail öffnen: Ohne Nummer' });
    expect(heading).not.toHaveTextContent('#');
  });

  it('öffnet das Detail-Modal (onOpenDetail) bei Klick auf den Titel', async () => {
    const onOpenDetail = vi.fn();
    const item = makeItem();
    renderCard(item, { onOpenDetail });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Detail öffnen: Mein Titel' }));

    expect(onOpenDetail).toHaveBeenCalledWith(item);
  });

  it('öffnet das Detail-Modal auch per Tastatur (Enter)', async () => {
    const onOpenDetail = vi.fn();
    const item = makeItem();
    renderCard(item, { onOpenDetail });

    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Detail öffnen: Mein Titel' }).focus();
    await user.keyboard('{Enter}');

    expect(onOpenDetail).toHaveBeenCalledWith(item);
  });

  it('Titel-Klick löst nicht den Edit-Drawer aus (kein Drag/Edit)', async () => {
    const onOpenDetail = vi.fn();
    const onEdit = vi.fn();
    renderCard(makeItem(), { onOpenDetail, onEdit });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Detail öffnen: Mein Titel' }));

    expect(onOpenDetail).toHaveBeenCalledTimes(1);
    expect(onEdit).not.toHaveBeenCalled();
  });

  it('nicht-archiviertes Item: Menü bietet Bearbeiten und Archivieren', async () => {
    const onEdit = vi.fn();
    const onArchive = vi.fn();
    const item = makeItem({ archived: false });
    renderCard(item, { onEdit, onArchive });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Item-Menü' }));
    await user.click(screen.getByRole('menuitem', { name: 'Bearbeiten' }));
    expect(onEdit).toHaveBeenCalledWith(item);

    await user.click(screen.getByRole('button', { name: 'Item-Menü' }));
    await user.click(screen.getByRole('menuitem', { name: 'Archivieren' }));
    expect(onArchive).toHaveBeenCalledWith(item);
  });

  it('Menü bietet tastaturbedienbaren Statuswechsel in andere Spalten (#316)', async () => {
    const onMove = vi.fn();
    const item = makeItem({ archived: false, column: 'BACKLOG' });
    renderCard(item, { onMove });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Item-Menü' }));
    // Aktuelle Spalte (Backlog) darf nicht als Ziel erscheinen.
    expect(screen.queryByRole('menuitem', { name: 'Nach Backlog' })).not.toBeInTheDocument();
    // Andere Spalten schon — hier nach Ready verschieben.
    await user.click(screen.getByRole('menuitem', { name: 'Nach Ready' }));
    expect(onMove).toHaveBeenCalledWith(item, 'READY');
  });

  it('archiviertes Item: Menü bietet Wiederherstellen und Endgültig löschen', async () => {
    const onRestore = vi.fn();
    const onForceDelete = vi.fn();
    const item = makeItem({ archived: true });
    renderCard(item, { onRestore, onForceDelete });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Item-Menü' }));
    expect(screen.getByRole('menuitem', { name: 'Wiederherstellen' })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: 'Endgültig löschen' })).toBeInTheDocument();

    await user.click(screen.getByRole('menuitem', { name: 'Wiederherstellen' }));
    expect(onRestore).toHaveBeenCalledWith(item);
  });

  it('archiviertes Item: zeigt Chip "Archiviert"', () => {
    renderCard(makeItem({ archived: true }));
    expect(screen.getByText('Archiviert')).toBeInTheDocument();
  });

  it('archiviertes Item: Chip ist in der Archiv-Farbe eingefärbt (#289)', () => {
    renderCard(makeItem({ archived: true }));
    const chip = screen.getByText('Archiviert').closest('.MuiChip-root');
    expect(chip).toHaveStyle({ backgroundColor: ARCHIVED_STATUS_COLOR.bg });
  });

  it('nicht-archiviertes Item: kein Chip "Archiviert"', () => {
    renderCard(makeItem({ archived: false }));
    expect(screen.queryByText('Archiviert')).not.toBeInTheDocument();
  });
});

describe('KanbanCard Kit-Look (#281)', () => {
  afterEach(() => cleanup());

  it('zeigt nur Nummer und Titel, keinen Body-Auszug mehr', () => {
    renderCard(makeItem({ number: 3, title: 'Nur Titel', body: 'Sollte nicht erscheinen' }));

    expect(screen.getByText(/#3/)).toBeInTheDocument();
    expect(screen.getByText('Nur Titel')).toBeInTheDocument();
    expect(screen.queryByText('Sollte nicht erscheinen')).not.toBeInTheDocument();
  });

  it('zeigt den Done-Countdown weiterhin, wenn das Item in DONE liegt', () => {
    const moved = new Date(Date.now() - 2 * 86_400_000).toISOString();
    renderCard(makeItem({ column: 'DONE', movedToDoneAt: moved }));

    expect(screen.getByText(/wird in 3 Tagen gelöscht/)).toBeInTheDocument();
  });
});
