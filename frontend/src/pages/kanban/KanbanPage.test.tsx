import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { MemoryRouter, Route, Routes } from 'react-router-dom';

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
  deleteKanbanEpic: vi.fn(),
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
  deleteKanbanEpic,
  getKanbanEpics,
  getKanbanSettings,
  listKanbanComments,
  listKanbanItems,
  updateKanbanItem,
  updateKanbanSettings,
} from '../../api/kanban';
import { ApiError } from '../../api/client';

const list = listKanbanItems as ReturnType<typeof vi.fn>;
const getEpics = getKanbanEpics as ReturnType<typeof vi.fn>;
const create = createKanbanItem as ReturnType<typeof vi.fn>;
const update = updateKanbanItem as ReturnType<typeof vi.fn>;
const deleteEpic = deleteKanbanEpic as ReturnType<typeof vi.fn>;
const archive = archiveKanbanItem as ReturnType<typeof vi.fn>;
const getSettings = getKanbanSettings as ReturnType<typeof vi.fn>;
const putSettings = updateKanbanSettings as ReturnType<typeof vi.fn>;
const listComments = listKanbanComments as ReturnType<typeof vi.fn>;

function renderPage(view: 'board' | 'list' | 'epics' = 'board') {
  return render(
    <MemoryRouter initialEntries={[`/kanban/${view}`]}>
      <NotifyProvider>
        <Routes>
          <Route path="/kanban/:view" element={<KanbanPage />} />
        </Routes>
      </NotifyProvider>
    </MemoryRouter>,
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

    await waitFor(() => expect(update).toHaveBeenCalledWith(1, 'Neu', 'AlterBody', null, null));
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

  it('zeigt die Listenansicht unter der Route /kanban/list (#328)', async () => {
    list.mockResolvedValue({ BACKLOG: [], READY: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] });

    renderPage('list');

    expect(await screen.findByTestId('list-view-stub')).toBeInTheDocument();
    expect(screen.queryByText('Noch keine Kanban-Items')).not.toBeInTheDocument();
  });

  it('erhöht den reloadKey der Liste nach dem Anlegen im Listen-Modus (#308)', async () => {
    list.mockResolvedValue({ BACKLOG: [], READY: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] });
    create.mockResolvedValueOnce(makeItem({ id: 7, title: 'Neu' }));

    renderPage('list');

    const stub = await screen.findByTestId('list-view-stub');
    expect(stub).toHaveTextContent('reloadKey=0');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Neues Item' }));
    const titleInput = await screen.findByLabelText('Titel');
    await user.type(titleInput, 'Neu');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    // Body-Template ist hier nebensächlich — der Test prüft den Reload-Trigger.
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith('Neu', expect.any(String), 'BACKLOG', 'ITEM', null, null),
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

    renderPage('epics');
    const user = userEvent.setup();

    // Epics-Ansicht (Route /kanban/epics) → Kachel mit Fortschritt.
    expect(await screen.findByText('Workshop A')).toBeInTheDocument();
    expect(screen.getByText('1/2 Stories fertig')).toBeInTheDocument();

    // Detail öffnen → Voll-Board mit dem Kind-Item in seiner Spalte.
    await user.click(screen.getByRole('button', { name: 'Epic öffnen: Workshop A' }));
    expect(await screen.findByText('Story X')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Alle Epics' })).toBeInTheDocument();

    // Der Spalten-„+"-Einstieg legt ein Item an, das dem Epic zugeordnet ist (parentId=7).
    // Kein doppelter „Neue Story"-Kopf-Button mehr (#343).
    expect(screen.queryByRole('button', { name: 'Neue Story' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Neues Item in Backlog' }));
    await user.type(await screen.findByLabelText('Titel'), 'Neue Story');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith('Neue Story', expect.any(String), 'BACKLOG', 'ITEM', 7, null),
    );
  });

  function emptyBoard() {
    return { BACKLOG: [], READY: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] };
  }

  function epicFixture(overrides = {}) {
    return {
      id: 7,
      number: 3,
      title: 'Workshop A',
      body: 'Alte Beschreibung',
      type: 'EPIC' as const,
      shortcode: null,
      progress: { done: 0, total: 0 },
      ...overrides,
    };
  }

  async function openEpicDetail(user: ReturnType<typeof userEvent.setup>) {
    expect(await screen.findByText('Workshop A')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Epic öffnen: Workshop A' }));
    expect(await screen.findByRole('button', { name: 'Alle Epics' })).toBeInTheDocument();
  }

  it('bearbeitet ein Epic über das Edit-Modal und ruft updateKanbanItem mit Kürzel (#331)', async () => {
    list.mockResolvedValue(emptyBoard());
    getEpics.mockResolvedValue([epicFixture()]);
    update.mockResolvedValueOnce(makeItem({ id: 7 }));

    renderPage('epics');
    const user = userEvent.setup();
    await openEpicDetail(user);

    await user.click(screen.getByRole('button', { name: 'Epic bearbeiten' }));
    // Felder sind vorbefüllt.
    expect(await screen.findByLabelText('Titel')).toHaveValue('Workshop A');
    expect(screen.getByLabelText('Kürzel')).toHaveValue('');

    await user.clear(screen.getByLabelText('Titel'));
    await user.type(screen.getByLabelText('Titel'), 'Workshop B');
    await user.type(screen.getByLabelText('Kürzel'), 'WSB');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(7, 'Workshop B', 'Alte Beschreibung', 'WSB'),
    );
  });

  it('löscht ein leeres Epic nach Bestätigung und kehrt zur Kachel-Liste zurück (#331)', async () => {
    list.mockResolvedValue(emptyBoard());
    getEpics.mockResolvedValue([epicFixture()]);
    deleteEpic.mockResolvedValueOnce(undefined);

    renderPage('epics');
    const user = userEvent.setup();
    await openEpicDetail(user);

    await user.click(screen.getByRole('button', { name: 'Epic löschen' }));
    await user.click(screen.getByRole('button', { name: 'Löschen' }));

    await waitFor(() => expect(deleteEpic).toHaveBeenCalledWith(7));
    // Zurück zur Kachel-Liste: der Detail-Zurück-Button verschwindet.
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Alle Epics' })).not.toBeInTheDocument(),
    );
  });

  it('zeigt bei 409 eine verständliche Meldung und behält das Epic (#331)', async () => {
    list.mockResolvedValue(emptyBoard());
    getEpics.mockResolvedValue([epicFixture()]);
    deleteEpic.mockRejectedValueOnce(new ApiError(409, 'Conflict', null));

    renderPage('epics');
    const user = userEvent.setup();
    await openEpicDetail(user);

    await user.click(screen.getByRole('button', { name: 'Epic löschen' }));
    await user.click(screen.getByRole('button', { name: 'Löschen' }));

    expect(
      await screen.findByText(
        'Das Epic hat noch zugeordnete Items und kann nicht gelöscht werden.',
      ),
    ).toBeInTheDocument();
    // Epic bleibt im Detail sichtbar (findBy: der Bestätigungsdialog blendet den Hintergrund
    // während seiner Schließ-Transition kurz per aria-hidden aus).
    expect(await screen.findByRole('button', { name: 'Alle Epics' })).toBeInTheDocument();
  });
});
