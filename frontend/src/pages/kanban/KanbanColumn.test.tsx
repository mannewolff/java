import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { DndContext } from '@dnd-kit/core';

import KanbanColumnView from './KanbanColumn';
import type { KanbanItem } from '../../api/kanban';

function makeItem(id: number, title: string): KanbanItem {
  return {
    id,
    title,
    body: '',
    column: 'BACKLOG',
    position: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    movedToDoneAt: null,
    archived: false,
    number: id,
    type: 'ITEM' as const,
    parentId: null,
  };
}

function renderColumn(
  column: 'BACKLOG' | 'READY' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE',
  label: string,
  items: KanbanItem[] = [],
) {
  return render(
    <DndContext>
      <KanbanColumnView
        column={column}
        label={label}
        items={items}
        retentionDays={5}
        onCreate={vi.fn()}
        onOpenDetail={vi.fn()}
        onEdit={vi.fn()}
        onArchive={vi.fn()}
        onRestore={vi.fn()}
        onForceDelete={vi.fn()}
        onMove={vi.fn()}
      />
    </DndContext>,
  );
}

describe('KanbanColumnView Kit-Header (#281)', () => {
  afterEach(() => cleanup());

  it('zeigt Label und Item-Anzahl im Header, kein Status-Icon mehr', () => {
    renderColumn('READY', 'Ready', [makeItem(1, 'A'), makeItem(2, 'B')]);

    expect(screen.getByText('Ready')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.queryByTestId('FlagIcon')).not.toBeInTheDocument();
    expect(screen.queryByTestId('InboxIcon')).not.toBeInTheDocument();
  });

  it('zeigt 0 als Anzahl bei leerer Spalte', () => {
    renderColumn('BACKLOG', 'Backlog');
    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('rendert weiterhin den Hinzufügen-Button', () => {
    renderColumn('BACKLOG', 'Backlog');
    expect(screen.getByRole('button', { name: 'Neues Item in Backlog' })).toBeInTheDocument();
  });

  it('behält das aria-label der Spalte für Screenreader/Tests', () => {
    renderColumn('IN_REVIEW', 'In Review');
    expect(screen.getByLabelText('Spalte In Review')).toBeInTheDocument();
  });
});
