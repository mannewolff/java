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
}));

import {
  archiveKanbanItem,
  createKanbanItem,
  getKanbanSettings,
  listKanbanItems,
  updateKanbanItem,
  updateKanbanSettings,
} from '../../api/kanban';

const list = listKanbanItems as ReturnType<typeof vi.fn>;
const create = createKanbanItem as ReturnType<typeof vi.fn>;
const update = updateKanbanItem as ReturnType<typeof vi.fn>;
const archive = archiveKanbanItem as ReturnType<typeof vi.fn>;
const getSettings = getKanbanSettings as ReturnType<typeof vi.fn>;
const putSettings = updateKanbanSettings as ReturnType<typeof vi.fn>;

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
    archived: false,
    number: 1,
    ...overrides,
  };
}

describe('KanbanPage', () => {
  beforeEach(() => {
    list.mockReset();
    create.mockReset();
    update.mockReset();
    archive.mockReset();
    getSettings.mockReset();
    putSettings.mockReset();
    getSettings.mockResolvedValue({ doneRetentionDays: 5 });
  });

  afterEach(() => cleanup());

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

  it('legt ein neues Item via Drawer an', async () => {
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
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    await waitFor(() => expect(create).toHaveBeenCalledWith('Neu', '', 'BACKLOG'));
    await waitFor(() => expect(screen.getByText('Neu')).toBeInTheDocument());
  });

  it('Drawer "Übernehmen" ist disabled, solange Titel leer ist', async () => {
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

    const apply = await screen.findByRole('button', { name: 'Übernehmen' });
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

  it('öffnet den Edit-Drawer mit Inhalt und ruft updateKanbanItem', async () => {
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

    const titleInput = await screen.findByLabelText('Titel');
    await user.clear(titleInput);
    await user.type(titleInput, 'Neu');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

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
    expect(screen.getByText(/wird in 3 Tagen gelöscht/)).toBeInTheDocument();
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
});
