import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { DndContext } from '@dnd-kit/core';

import KanbanColumnView from './KanbanColumn';

function renderColumn(column: 'BACKLOG' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE', label: string) {
  return render(
    <DndContext>
      <KanbanColumnView
        column={column}
        label={label}
        items={[]}
        retentionDays={5}
        onCreate={vi.fn()}
        onOpenDetail={vi.fn()}
        onEdit={vi.fn()}
        onArchive={vi.fn()}
        onRestore={vi.fn()}
        onForceDelete={vi.fn()}
      />
    </DndContext>,
  );
}

describe('KanbanColumnView Header-Icons (#189)', () => {
  afterEach(() => cleanup());

  it('zeigt das passende Icon je Spalte', () => {
    renderColumn('BACKLOG', 'Backlog');
    expect(screen.getByTestId('InboxIcon')).toBeInTheDocument();
    cleanup();
    renderColumn('IN_PROGRESS', 'In Progress');
    expect(screen.getByTestId('PlayArrowIcon')).toBeInTheDocument();
    cleanup();
    renderColumn('IN_REVIEW', 'In Review');
    expect(screen.getByTestId('VisibilityIcon')).toBeInTheDocument();
    cleanup();
    renderColumn('DONE', 'Done');
    expect(screen.getByTestId('CheckCircleIcon')).toBeInTheDocument();
  });

  it('rendert weiterhin Label und Hinzufügen-Button', () => {
    renderColumn('BACKLOG', 'Backlog');
    expect(screen.getByText('Backlog')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Neues Item in Backlog' })).toBeInTheDocument();
  });
});
