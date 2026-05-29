import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanDetailModal from './KanbanDetailModal';
import type { KanbanComment, KanbanItem } from '../../api/kanban';
import {
  addKanbanComment,
  deleteKanbanComment,
  listKanbanComments,
  updateKanbanComment,
} from '../../api/kanban';

vi.mock('../../auth/useAuth', () => ({
  useAuth: () => ({
    isLoading: false,
    isAuthenticated: true,
    username: 'alice',
    email: undefined,
    initial: 'A',
    error: undefined,
    signIn: vi.fn(),
    signOut: vi.fn(),
  }),
}));

vi.mock('../../api/kanban', async () => {
  const actual = await vi.importActual<typeof import('../../api/kanban')>('../../api/kanban');
  return {
    ...actual,
    listKanbanComments: vi.fn(),
    addKanbanComment: vi.fn(),
    updateKanbanComment: vi.fn(),
    deleteKanbanComment: vi.fn(),
  };
});

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

function makeComment(overrides: Partial<KanbanComment> = {}): KanbanComment {
  return {
    id: 10,
    itemId: 1,
    author: 'alice',
    body: 'Hallo',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('KanbanDetailModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listKanbanComments).mockResolvedValue([]);
    vi.mocked(addKanbanComment).mockResolvedValue(makeComment());
    vi.mocked(updateKanbanComment).mockResolvedValue(makeComment());
    vi.mocked(deleteKanbanComment).mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it('zeigt Titel und Body sofort bearbeitbar', async () => {
    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
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

    await screen.findByText('Noch keine Kommentare.');
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

    await screen.findByText('Noch keine Kommentare.');
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

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('zeigt den Cleanup-Countdown für DONE-Items', async () => {
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

    await screen.findByText('Noch keine Kommentare.');
    // 5 Tage Retention, 2 Tage vergangen → 3 Tage übrig.
    expect(screen.getByText(/wird in 3 Tagen gelöscht/)).toBeInTheDocument();
  });

  it('lädt Kommentare beim Öffnen und bietet Edit/Delete nur für eigene', async () => {
    vi.mocked(listKanbanComments).mockResolvedValue([
      makeComment({ id: 10, author: 'alice', body: 'Mein Kommentar' }),
      makeComment({ id: 11, author: 'bob', body: 'Fremder Kommentar' }),
    ]);

    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Mein Kommentar');
    expect(screen.getByText('Fremder Kommentar')).toBeInTheDocument();
    expect(listKanbanComments).toHaveBeenCalledWith(1);
    // Nur der eigene Kommentar (alice) bekommt Edit/Delete.
    expect(screen.getAllByLabelText('Kommentar bearbeiten')).toHaveLength(1);
    expect(screen.getAllByLabelText('Kommentar löschen')).toHaveLength(1);
  });

  it('fügt einen Kommentar hinzu', async () => {
    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Neuer Kommentar'), 'Hallo Welt');
    await user.click(screen.getByRole('button', { name: 'Kommentar hinzufügen' }));

    await waitFor(() => expect(addKanbanComment).toHaveBeenCalledWith(1, 'Hallo Welt'));
  });

  it('bearbeitet einen eigenen Kommentar', async () => {
    vi.mocked(listKanbanComments).mockResolvedValue([
      makeComment({ id: 10, author: 'alice', body: 'Alt' }),
    ]);

    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Alt');
    const user = userEvent.setup();
    await user.click(screen.getByLabelText('Kommentar bearbeiten'));

    const editArea = screen.getByLabelText('Kommentar bearbeiten');
    await user.clear(editArea);
    await user.type(editArea, 'Neu');
    // Zwei "Speichern": das Edit-Feld (zuerst im DOM) und der Modal-Footer.
    await user.click(screen.getAllByRole('button', { name: 'Speichern' })[0]);

    await waitFor(() => expect(updateKanbanComment).toHaveBeenCalledWith(1, 10, 'Neu'));
  });

  it('löscht einen eigenen Kommentar nach Bestätigung', async () => {
    vi.mocked(listKanbanComments).mockResolvedValue([
      makeComment({ id: 10, author: 'alice', body: 'Weg damit' }),
    ]);

    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Weg damit');
    const user = userEvent.setup();
    await user.click(screen.getByLabelText('Kommentar löschen'));
    await user.click(screen.getByRole('button', { name: 'Löschen' }));

    await waitFor(() => expect(deleteKanbanComment).toHaveBeenCalledWith(1, 10));
  });
});
