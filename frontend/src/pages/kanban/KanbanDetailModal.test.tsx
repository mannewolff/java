import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanDetailModal from './KanbanDetailModal';
import type { KanbanItem } from '../../api/kanban';

function makeItem(overrides: Partial<KanbanItem> = {}): KanbanItem {
  return {
    id: 1,
    title: 'Titel',
    body: 'Body-Text',
    column: 'BACKLOG',
    position: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('KanbanDetailModal', () => {
  afterEach(() => cleanup());

  it('zeigt Titel und Body sofort bearbeitbar', () => {
    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('Titel')).toHaveValue('Titel');
    expect(screen.getByLabelText('Markdown-Beschreibung')).toHaveValue('Body-Text');
  });

  it('Speichern ist disabled, solange der Titel leer ist', async () => {
    render(
      <KanbanDetailModal
        open
        item={makeItem({ title: 'X' })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText('Titel'));

    expect(screen.getByRole('button', { name: 'Speichern' })).toBeDisabled();
  });

  it('Speichern ruft onSubmit mit getrimmtem Titel und Body', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <KanbanDetailModal
        open
        item={makeItem({ title: 'Alt', body: 'Alt-Body' })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    const user = userEvent.setup();
    const titleInput = screen.getByLabelText('Titel');
    await user.clear(titleInput);
    await user.type(titleInput, '  Neu  ');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).toHaveBeenCalledWith('Neu', 'Alt-Body');
  });

  it('Abbrechen ruft onClose, ohne zu speichern', async () => {
    const onClose = vi.fn();
    const onSubmit = vi.fn();
    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={onClose}
        onSubmit={onSubmit}
      />,
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('zeigt den Cleanup-Countdown für DONE-Items', () => {
    const moved = new Date(Date.now() - 2 * 86_400_000).toISOString();
    render(
      <KanbanDetailModal
        open
        item={makeItem({ column: 'DONE', movedToDoneAt: moved })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    // 5 Tage Retention, 2 Tage vergangen → 3 Tage übrig.
    expect(screen.getByText(/wird in 3 Tagen gelöscht/)).toBeInTheDocument();
  });
});
