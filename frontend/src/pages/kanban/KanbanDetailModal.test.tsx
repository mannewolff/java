import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanDetailModal from './KanbanDetailModal';
import type { KanbanComment, KanbanEpic, KanbanItem } from '../../api/kanban';
import {
  addKanbanComment,
  deleteKanbanComment,
  getKanbanEpics,
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
    getKanbanEpics: vi.fn(),
  };
});

function epic(overrides: Partial<KanbanEpic> = {}): KanbanEpic {
  return {
    id: 7,
    number: 3,
    title: 'Workshop',
    body: '',
    type: 'EPIC',
    shortcode: null,
    progress: { done: 0, total: 0 },
    ...overrides,
  };
}

function makeItem(overrides: Partial<KanbanItem> = {}): KanbanItem {
  return {
    id: 1,
    title: 'Titel',
    body: '## Kontext\nBody-Text',
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

/** Wechselt vom Lese- in den Edit-Modus. */
async function enterEdit(user: ReturnType<typeof userEvent.setup>): Promise<void> {
  await user.click(screen.getByRole('button', { name: 'Bearbeiten' }));
}

describe('KanbanDetailModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listKanbanComments).mockResolvedValue([]);
    vi.mocked(addKanbanComment).mockResolvedValue(makeComment());
    vi.mocked(updateKanbanComment).mockResolvedValue(makeComment());
    vi.mocked(deleteKanbanComment).mockResolvedValue(undefined);
    vi.mocked(getKanbanEpics).mockResolvedValue([]);
  });

  afterEach(() => cleanup());

  it('öffnet im Lesemodus mit gerendertem Markdown und ohne Roh-Textarea', async () => {
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
    // Markdown gerendert: die Ueberschrift "Kontext" erscheint als heading, nicht als "## Kontext".
    expect(screen.getByRole('heading', { name: 'Kontext' })).toBeInTheDocument();
    expect(screen.getByText('Body-Text')).toBeInTheDocument();
    // Keine Bearbeitungsfelder im Lesemodus.
    expect(screen.queryByLabelText('Markdown-Beschreibung')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Titel')).not.toBeInTheDocument();
  });

  it('zeigt im Edit-Modus die Roh-Markdown-Textarea und keine Kommentare', async () => {
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
    await enterEdit(user);

    expect(screen.getByLabelText('Titel')).toHaveValue('Titel');
    expect(screen.getByLabelText('Markdown-Beschreibung')).toHaveValue('## Kontext\nBody-Text');
    // Feste Starthöhe (kein Autosize), damit der CSS-Ziehgriff greift (#338).
    expect(screen.getByLabelText('Markdown-Beschreibung')).toHaveAttribute('rows', '8');
    // Kommentare sind im Edit-Modus ausgeblendet.
    expect(screen.queryByText('Kommentare')).not.toBeInTheDocument();
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
    await enterEdit(user);
    await user.clear(screen.getByLabelText('Titel'));

    expect(screen.getByRole('button', { name: 'Speichern' })).toBeDisabled();
  });

  it('Speichern ruft onSubmit mit getrimmtem Titel und Body und kehrt in den Lesemodus zurück', async () => {
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
    await enterEdit(user);
    const titleInput = screen.getByLabelText('Titel');
    await user.clear(titleInput);
    await user.type(titleInput, '  Neu  ');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).toHaveBeenCalledWith('Neu', 'Alt-Body', null, []);
    // Nach dem Speichern wieder im Lesemodus (Bearbeiten-Button sichtbar).
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Bearbeiten' })).toBeInTheDocument(),
    );
  });

  it('belegt die Epic-Auswahl im Edit-Modus mit der aktuellen Zuordnung vor (#339)', async () => {
    vi.mocked(getKanbanEpics).mockResolvedValue([epic({ id: 7, title: 'Workshop' })]);
    render(
      <KanbanDetailModal
        open
        item={makeItem({ parentId: 7 })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);
    await screen.findByRole('option', { name: /Workshop/ });

    expect(screen.getByLabelText('Epic')).toHaveValue('7');
  });

  it('ordnet ein Item nachträglich einem Epic zu und übergibt parentId (#339)', async () => {
    vi.mocked(getKanbanEpics).mockResolvedValue([epic({ id: 7, title: 'Workshop' })]);
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <KanbanDetailModal
        open
        item={makeItem({ parentId: null })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);
    await screen.findByRole('option', { name: /Workshop/ });
    await user.selectOptions(screen.getByLabelText('Epic'), '7');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).toHaveBeenCalledWith('Titel', '## Kontext\nBody-Text', 7, []);
  });

  it('entfernt die Epic-Zuordnung, wenn „(kein Epic)" gewählt wird (#339)', async () => {
    vi.mocked(getKanbanEpics).mockResolvedValue([epic({ id: 7, title: 'Workshop' })]);
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <KanbanDetailModal
        open
        item={makeItem({ parentId: 7 })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);
    await screen.findByRole('option', { name: /Workshop/ });
    await user.selectOptions(screen.getByLabelText('Epic'), '');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).toHaveBeenCalledWith('Titel', '## Kontext\nBody-Text', null, []);
  });

  it('übergibt die eingegebenen Abhängigkeits-Nummern an onSubmit (#353)', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <KanbanDetailModal
        open
        item={makeItem({})}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);
    await user.type(screen.getByLabelText('Abhängig von'), '12, 34');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).toHaveBeenCalledWith('Titel', '## Kontext\nBody-Text', null, [12, 34]);
  });

  it('zeigt vorhandene Abhängigkeiten im Lesemodus als #N (#353)', async () => {
    render(
      <KanbanDetailModal
        open
        item={makeItem({ dependencies: [12, 34] })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(await screen.findByLabelText('Abhängigkeiten')).toHaveTextContent(
      'Abhängig von: #12, #34',
    );
  });

  it('blockt das Speichern bei ungültiger Abhängigkeits-Eingabe (#353)', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <KanbanDetailModal
        open
        item={makeItem({})}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);
    await user.type(screen.getByLabelText('Abhängig von'), 'abc');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/Nur positive Nummern/)).toBeInTheDocument();
  });

  it('bietet Wiederherstellen/Endgültig-Löschen nur bei archivierten Items (#341)', async () => {
    const { rerender } = render(
      <KanbanDetailModal
        open
        item={makeItem({ archived: false })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        onRestore={vi.fn()}
        onForceDelete={vi.fn()}
      />,
    );
    await screen.findByText('Noch keine Kommentare.');
    expect(screen.queryByRole('button', { name: 'Wiederherstellen' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Endgültig löschen' })).not.toBeInTheDocument();

    rerender(
      <KanbanDetailModal
        open
        item={makeItem({ archived: true })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        onRestore={vi.fn()}
        onForceDelete={vi.fn()}
      />,
    );
    expect(screen.getByRole('button', { name: 'Wiederherstellen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Endgültig löschen' })).toBeInTheDocument();
  });

  it('Wiederherstellen ruft onRestore (#341)', async () => {
    const onRestore = vi.fn().mockResolvedValue(undefined);
    render(
      <KanbanDetailModal
        open
        item={makeItem({ archived: true })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        onRestore={onRestore}
        onForceDelete={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Wiederherstellen' }));

    expect(onRestore).toHaveBeenCalledTimes(1);
  });

  it('Endgültig löschen ruft onForceDelete erst nach Bestätigung (#341)', async () => {
    const onForceDelete = vi.fn().mockResolvedValue(undefined);
    render(
      <KanbanDetailModal
        open
        item={makeItem({ archived: true, title: 'Weg damit' })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        onRestore={vi.fn()}
        onForceDelete={onForceDelete}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Endgültig löschen' }));

    // Bestätigungsdialog erscheint; ohne Bestätigung kein Aufruf.
    expect(await screen.findByText(/wird unwiderruflich entfernt/)).toBeInTheDocument();
    expect(onForceDelete).not.toHaveBeenCalled();

    // Der bestätigende Button im Dialog (zweiter „Endgültig löschen").
    const confirmButtons = screen.getAllByRole('button', { name: 'Endgültig löschen' });
    await user.click(confirmButtons[confirmButtons.length - 1]);

    expect(onForceDelete).toHaveBeenCalledTimes(1);
  });

  it('zeigt keine Epic-Auswahl, wenn das Item selbst ein Epic ist (#339)', async () => {
    render(
      <KanbanDetailModal
        open
        item={makeItem({ type: 'EPIC' })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);

    expect(screen.queryByLabelText('Epic')).not.toBeInTheDocument();
    expect(getKanbanEpics).not.toHaveBeenCalled();
  });

  it('Abbrechen verwirft den Draft und kehrt in den Lesemodus zurück', async () => {
    const onSubmit = vi.fn();
    render(
      <KanbanDetailModal
        open
        item={makeItem({ title: 'Alt' })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);
    await user.clear(screen.getByLabelText('Titel'));
    await user.type(screen.getByLabelText('Titel'), 'Verworfen');
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onSubmit).not.toHaveBeenCalled();
    // Zurueck im Lesemodus; erneutes Bearbeiten zeigt den unveraenderten Titel.
    await enterEdit(user);
    expect(screen.getByLabelText('Titel')).toHaveValue('Alt');
  });

  it('Schließen ruft onClose', async () => {
    const onClose = vi.fn();
    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={onClose}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Schließen' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('ESC im Edit-Modus kehrt in den Lesemodus zurück, ohne onClose (#357)', async () => {
    const onClose = vi.fn();
    render(
      <KanbanDetailModal
        open
        item={makeItem({ title: 'Alt' })}
        retentionDays={5}
        onClose={onClose}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);
    expect(screen.getByLabelText('Titel')).toBeInTheDocument();

    await user.keyboard('{Escape}');

    // Zurück im Lesemodus (Bearbeiten-Button sichtbar), Modal bleibt offen.
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Bearbeiten' })).toBeInTheDocument(),
    );
    expect(screen.queryByLabelText('Titel')).not.toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('ESC im Lesemodus schließt das Modal (#357)', async () => {
    const onClose = vi.fn();
    render(
      <KanbanDetailModal
        open
        item={makeItem()}
        retentionDays={5}
        onClose={onClose}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await user.keyboard('{Escape}');

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('Backdrop-Klick im Edit-Modus kehrt in den Lesemodus zurück, ohne onClose (#357)', async () => {
    const onClose = vi.fn();
    const { baseElement } = render(
      <KanbanDetailModal
        open
        item={makeItem({ title: 'Alt' })}
        retentionDays={5}
        onClose={onClose}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const user = userEvent.setup();
    await enterEdit(user);

    const backdrop = baseElement.querySelector('.MuiBackdrop-root');
    expect(backdrop).not.toBeNull();
    await user.click(backdrop as Element);

    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Bearbeiten' })).toBeInTheDocument(),
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it('zeigt Status-Badge und Item-Nummer im Header (Kit-Optik)', async () => {
    render(
      <KanbanDetailModal
        open
        item={makeItem({ number: 42, column: 'READY', title: 'Startklar' })}
        retentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await screen.findByText('Noch keine Kommentare.');
    const title = screen.getByRole('heading', { name: /Startklar/ });
    expect(title).toHaveTextContent('#42');
    expect(title).toHaveTextContent('Ready');
  });

  it('nutzt die Kit-Chrome-Farben für Header-Trennstrich und Kommentar-Karten (Issue #302)', async () => {
    vi.mocked(listKanbanComments).mockResolvedValue([
      makeComment({ id: 10, author: 'alice', body: 'Kit-Look-Check' }),
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

    await screen.findByText('Kit-Look-Check');
    expect(screen.getByTestId('kanban-detail-header')).toHaveStyle({ borderBottomColor: '#e8e8e8' });
    expect(screen.getByTestId('kanban-comment-card-10')).toHaveStyle({
      backgroundColor: '#f8f8f8',
      borderColor: '#e8e8e8',
    });
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
    expect(screen.getByText(/wird in 3 Tagen archiviert/)).toBeInTheDocument();
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
    // Im Lesemodus gibt es nur ein "Speichern" — das des Kommentar-Edits.
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

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
