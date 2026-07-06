import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanPage from './KanbanPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

vi.mock('../../api/kanban', () => ({
  KANBAN_COLUMNS: ['BACKLOG', 'READY', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'],
  listKanbanItems: vi.fn(),
  createKanbanItem: vi.fn(),
  updateKanbanItem: vi.fn(),
  moveKanbanItem: vi.fn(),
  archiveKanbanItem: vi.fn(),
  forceDeleteKanbanItem: vi.fn(),
  restoreKanbanItem: vi.fn(),
  getKanbanSettings: vi.fn(),
  updateKanbanSettings: vi.fn(),
  getKanbanEpics: vi.fn(),
  listKanbanComments: vi.fn(),
  addKanbanComment: vi.fn(),
  updateKanbanComment: vi.fn(),
  deleteKanbanComment: vi.fn(),
}));

// Detail-Modal (Klick auf Karte oder Menü "Bearbeiten", #304) braucht einen eingeloggten User.
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

// Listenansicht wird als Stub gemockt — hier testen wir nur den View-Toggle und dass der
// Parent den Reload-Trigger (#308) an die Liste durchreicht, nicht die Liste selbst.
vi.mock('./KanbanListView', () => ({
  default: ({ reloadKey }: { reloadKey?: number }) => (
    <div data-testid="list-view-stub">ListView-Stub reloadKey={reloadKey}</div>
  ),
}));

function installMemoryLocalStorage(): void {
  const store = new Map<string, string>();
  vi.stubGlobal('localStorage', {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => void store.set(k, String(v)),
    removeItem: (k: string) => void store.delete(k),
    clear: () => store.clear(),
    key: (i: number) => [...store.keys()][i] ?? null,
    get length() {
      return store.size;
    },
  });
}

import {
  archiveKanbanItem,
  createKanbanItem,
  getKanbanEpics,
  getKanbanSettings,
  listKanbanComments,
  listKanbanItems,
  updateKanbanItem,
  updateKanbanSettings,
} from '../../api/kanban';

const list = listKanbanItems as ReturnType<typeof vi.fn>;
const getEpics = getKanbanEpics as ReturnType<typeof vi.fn>;
const create = createKanbanItem as ReturnType<typeof vi.fn>;
const update = updateKanbanItem as ReturnType<typeof vi.fn>;
const archive = archiveKanbanItem as ReturnType<typeof vi.fn>;
const getSettings = getKanbanSettings as ReturnType<typeof vi.fn>;
const putSettings = updateKanbanSettings as ReturnType<typeof vi.fn>;
const listComments = listKanbanComments as ReturnType<typeof vi.fn>;

function renderPage() {
  return render(
    <NotifyProvider>
      <KanbanPage />
    </NotifyProvider>,
  );
}

function makeItem(overrides = {}) {
  return {
    id: 1,
    title: 'Test-Item',
    body: 'Beschreibung',
    column: 'BACKLOG' as const,
    position: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    movedToDoneAt: null,
    archived: false,
    number: 1,
    type: 'ITEM',
    parentId: null,
    ...overrides,
  };
}

describe('KanbanPage', () => {
  beforeEach(() => {
    installMemoryLocalStorage();
    list.mockReset();
    create.mockReset();
    update.mockReset();
    archive.mockReset();
    getSettings.mockReset();
    putSettings.mockReset();
    listComments.mockReset();
    getEpics.mockReset();
    getSettings.mockResolvedValue({ doneRetentionDays: 5 });
    listComments.mockResolvedValue([]);
    getEpics.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('zeigt den Empty-State, wenn das Board leer ist', async () => {
    list.mockResolvedValueOnce({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();

    await waitFor(() =>
      expect(screen.getByText('Noch keine Kanban-Items')).toBeInTheDocument(),
    );
  });

  it('zeigt die fünf Spalten mit Items, wenn das Board befüllt ist', async () => {
    list.mockResolvedValueOnce({
      BACKLOG: [makeItem({ id: 1, title: 'Backlog-Item' })],
      READY: [makeItem({ id: 4, title: 'Ready-Item', column: 'READY' })],
      IN_PROGRESS: [makeItem({ id: 2, title: 'In-Progress-Item', column: 'IN_PROGRESS' })],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();

    await waitFor(() => expect(screen.getByText('Backlog-Item')).toBeInTheDocument());
    expect(screen.getByText('Ready-Item')).toBeInTheDocument();
    expect(screen.getByText('In-Progress-Item')).toBeInTheDocument();
    expect(screen.getByLabelText('Spalte Backlog')).toBeInTheDocument();
    expect(screen.getByLabelText('Spalte Ready')).toBeInTheDocument();
    expect(screen.getByLabelText('Spalte In Progress')).toBeInTheDocument();
    expect(screen.getByLabelText('Spalte In Review')).toBeInTheDocument();
    expect(screen.getByLabelText('Spalte Done')).toBeInTheDocument();
  });

  it('legt ein neues Item via Anlage-Modal an', async () => {
    list.mockResolvedValueOnce({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });
    create.mockResolvedValueOnce(makeItem({ id: 7, title: 'Neu' }));
    list.mockResolvedValueOnce({
      BACKLOG: [makeItem({ id: 7, title: 'Neu' })],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Noch keine Kanban-Items')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Erstes Item anlegen' }));

    const titleInput = await screen.findByLabelText('Titel');
    await user.type(titleInput, 'Neu');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith(
        'Neu',
        '## Kontext\n\n## Aufgabe\n\n## Akzeptanzkriterium\n\n## Abhängigkeiten\n',
        'BACKLOG',
        'ITEM',
        null,
      ),
    );
    await waitFor(() => expect(screen.getByText('Neu')).toBeInTheDocument());
  });

  it('Anlegen-Modal "Anlegen" ist disabled, solange Titel leer ist', async () => {
    list.mockResolvedValueOnce({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Noch keine Kanban-Items')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Erstes Item anlegen' }));

    const apply = await screen.findByRole('button', { name: 'Anlegen' });
    expect(apply).toBeDisabled();
  });

  it('zeigt den Archivieren-Confirm-Dialog und ruft die API nach Bestätigung', async () => {
    list.mockResolvedValueOnce({
      BACKLOG: [makeItem({ id: 1, title: 'Weg damit' })],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });
    archive.mockResolvedValueOnce(undefined);
    list.mockResolvedValueOnce({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Weg damit')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Item-Menü' }));
    await user.click(screen.getByRole('menuitem', { name: 'Archivieren' }));
    expect(screen.getByText(/Item archivieren\?/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Archivieren' }));
    await waitFor(() => expect(archive).toHaveBeenCalledWith(1));
  });

  it('öffnet über das Karten-Menü "Bearbeiten" das Detail-Modal und ruft updateKanbanItem (#304)', async () => {
    list.mockResolvedValueOnce({
      BACKLOG: [makeItem({ id: 1, title: 'Alt', body: 'AlterBody' })],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });
    update.mockResolvedValueOnce(makeItem({ id: 1, title: 'Neu', body: 'AlterBody' }));
    list.mockResolvedValueOnce({
      BACKLOG: [makeItem({ id: 1, title: 'Neu', body: 'AlterBody' })],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Alt')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Item-Menü' }));
    await user.click(screen.getByRole('menuitem', { name: 'Bearbeiten' }));

    // Das Modal öffnet im Lesemodus — erst über den Modal-Button "Bearbeiten" in den Edit-Modus.
    await user.click(await screen.findByRole('button', { name: 'Bearbeiten' }));
    const titleInput = await screen.findByLabelText('Titel');
    expect(titleInput).toHaveValue('Alt');
    await user.clear(titleInput);
    await user.type(titleInput, 'Neu');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    await waitFor(() => expect(update).toHaveBeenCalledWith(1, 'Neu', 'AlterBody'));
  });

  it('zeigt den Cleanup-Countdown bei DONE-Items', async () => {
    const moved = new Date(Date.now() - 2 * 86_400_000).toISOString();
    list.mockResolvedValueOnce({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [
        makeItem({
          id: 99,
          title: 'Erledigt',
          column: 'DONE',
          movedToDoneAt: moved,
        }),
      ],
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Erledigt')).toBeInTheDocument());
    expect(screen.getByText(/wird in 3 Tagen archiviert/)).toBeInTheDocument();
  });

  it('rendert einen Error-State, wenn der Initial-Load scheitert', async () => {
    list.mockRejectedValueOnce(new Error('boom'));

    renderPage();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/konnten nicht geladen/i),
    );
  });

  it('öffnet den Settings-Drawer mit dem geladenen Retention-Wert', async () => {
    getSettings.mockResolvedValue({ doneRetentionDays: 14 });
    list.mockResolvedValueOnce({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Noch keine Kanban-Items')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Kanban-Einstellungen' }));

    await waitFor(() => expect(screen.getByText('Kanban-Einstellungen')).toBeInTheDocument());
    expect(screen.getByText(/Done-Items nach/)).toHaveTextContent('14');
  });

  it('persistiert eine neue Retention via updateKanbanSettings', async () => {
    getSettings.mockResolvedValue({ doneRetentionDays: 5 });
    putSettings.mockResolvedValueOnce({ doneRetentionDays: 10 });
    list.mockResolvedValueOnce({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Noch keine Kanban-Items')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Kanban-Einstellungen' }));
    await screen.findByText('Kanban-Einstellungen');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    await waitFor(() => expect(putSettings).toHaveBeenCalledTimes(1));
    expect(putSettings).toHaveBeenCalledWith(5);
  });

  it('wechselt über den View-Toggle zur Listenansicht und persistiert die Wahl', async () => {
    list.mockResolvedValue({ BACKLOG: [], READY: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] });

    renderPage();
    await waitFor(() => expect(screen.getByText('Noch keine Kanban-Items')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Liste' }));

    expect(screen.getByTestId('list-view-stub')).toBeInTheDocument();
    expect(screen.queryByText('Noch keine Kanban-Items')).not.toBeInTheDocument();
    expect(localStorage.getItem('kanban.view')).toBe('list');
  });

  it('startet in der Listenansicht, wenn localStorage "list" gespeichert hat', async () => {
    localStorage.setItem('kanban.view', 'list');
    list.mockResolvedValue({ BACKLOG: [], READY: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] });

    renderPage();

    expect(await screen.findByTestId('list-view-stub')).toBeInTheDocument();
  });

  it('erhöht den reloadKey der Liste nach dem Anlegen im Listen-Modus (#308)', async () => {
    localStorage.setItem('kanban.view', 'list');
    list.mockResolvedValue({ BACKLOG: [], READY: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] });
    create.mockResolvedValueOnce(makeItem({ id: 7, title: 'Neu' }));

    renderPage();

    const stub = await screen.findByTestId('list-view-stub');
    expect(stub).toHaveTextContent('reloadKey=0');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Neues Item' }));
    const titleInput = await screen.findByLabelText('Titel');
    await user.type(titleInput, 'Neu');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    // Body-Template ist hier nebensächlich — der Test prüft den Reload-Trigger.
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith('Neu', expect.any(String), 'BACKLOG', 'ITEM', null),
    );
    // Der an die Liste durchgereichte reloadKey ist gestiegen — die Liste lädt neu.
    await waitFor(() =>
      expect(screen.getByTestId('list-view-stub')).toHaveTextContent('reloadKey=1'),
    );
  });

  it('Epics-Ansicht: zeigt Kacheln, öffnet das Detail als Voll-Board und legt eine Story im Epic an (#326)', async () => {
    list.mockResolvedValue({
      BACKLOG: [],
      READY: [],
      IN_PROGRESS: [makeItem({ id: 50, title: 'Story X', column: 'IN_PROGRESS', parentId: 7 })],
      IN_REVIEW: [],
      DONE: [],
    });
    getEpics.mockResolvedValue([
      {
        id: 7,
        number: 3,
        title: 'Workshop A',
        body: 'Beschreibung',
        type: 'EPIC',
        progress: { done: 1, total: 2 },
      },
    ]);
    create.mockResolvedValueOnce(makeItem({ id: 60, title: 'Neue Story', parentId: 7 }));

    renderPage();
    const user = userEvent.setup();

    // In die Epics-Ansicht wechseln → Kachel mit Fortschritt.
    await user.click(await screen.findByRole('button', { name: 'Epics' }));
    expect(await screen.findByText('Workshop A')).toBeInTheDocument();
    expect(screen.getByText('1/2 Stories fertig')).toBeInTheDocument();

    // Detail öffnen → Voll-Board mit dem Kind-Item in seiner Spalte.
    await user.click(screen.getByRole('button', { name: 'Epic öffnen: Workshop A' }));
    expect(await screen.findByText('Story X')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Alle Epics' })).toBeInTheDocument();

    // „Neue Story" legt ein Item an, das dem Epic zugeordnet ist (parentId=7).
    await user.click(screen.getByRole('button', { name: 'Neue Story' }));
    await user.type(await screen.findByLabelText('Titel'), 'Neue Story');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith('Neue Story', expect.any(String), 'BACKLOG', 'ITEM', 7),
    );
  });
});
