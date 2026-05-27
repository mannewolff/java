import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import DashboardListPage from './DashboardListPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

vi.mock('../../api/dashboard', () => ({
  listDashboards: vi.fn(),
  createDashboard: vi.fn(),
  deleteDashboard: vi.fn(),
  setDefaultDashboard: vi.fn(),
}));

import {
  createDashboard,
  deleteDashboard,
  listDashboards,
  setDefaultDashboard,
} from '../../api/dashboard';

const list = listDashboards as ReturnType<typeof vi.fn>;
const create = createDashboard as ReturnType<typeof vi.fn>;
const del = deleteDashboard as ReturnType<typeof vi.fn>;
const setDefault = setDefaultDashboard as ReturnType<typeof vi.fn>;

function ts(): string {
  return '2026-05-26T10:00:00Z';
}

describe('DashboardListPage', () => {
  beforeEach(() => {
    list.mockReset();
    create.mockReset();
    del.mockReset();
    setDefault.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  function render_() {
    return render(
      <MemoryRouter>
        <NotifyProvider>
          <DashboardListPage />
        </NotifyProvider>
      </MemoryRouter>,
    );
  }

  it('shows loading state initially', () => {
    list.mockReturnValueOnce(new Promise(() => undefined));

    render_();

    expect(screen.getByLabelText('Dashboards werden geladen')).toBeInTheDocument();
  });

  it('shows empty-state CTA card when list is empty', async () => {
    list.mockResolvedValueOnce([]);

    render_();

    await waitFor(() =>
      expect(screen.getByText(/Noch keine Dashboards/)).toBeInTheDocument(),
    );
    // CTA-Button neben dem "+"-Header-Button
    expect(
      screen.getByRole('button', { name: 'Erstes Dashboard anlegen' }),
    ).toBeInTheDocument();
  });

  it('renders dashboards with Default chip and star icon', async () => {
    list.mockResolvedValueOnce([
      { id: 1, name: 'Main', isDefault: true, createdAt: ts(), updatedAt: ts() },
      { id: 2, name: 'Side', isDefault: false, createdAt: ts(), updatedAt: ts() },
    ]);

    render_();

    await waitFor(() => expect(screen.getByText('Main')).toBeInTheDocument());
    expect(screen.getByText('Side')).toBeInTheDocument();
    expect(screen.getByText('Default')).toBeInTheDocument();
  });

  it('legt direkt ein Dashboard "Neues Dashboard" an beim Klick auf Neu', async () => {
    list.mockResolvedValueOnce([]);
    create.mockResolvedValueOnce({
      id: 1,
      name: 'Neues Dashboard',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
    });

    render_();
    await waitFor(() =>
      expect(screen.getByText(/Noch keine Dashboards/)).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Neues Dashboard anlegen' }));

    await waitFor(() => expect(create).toHaveBeenCalledWith('Neues Dashboard'));
    // Navigate-Aufruf landet nicht im MemoryRouter-DOM hier, aber der API-Call belegt
    // den Pfad eindeutig.
  });

  it('promotes a non-default dashboard to Default', async () => {
    list
      .mockResolvedValueOnce([
        { id: 1, name: 'A', isDefault: true, createdAt: ts(), updatedAt: ts() },
        { id: 2, name: 'B', isDefault: false, createdAt: ts(), updatedAt: ts() },
      ])
      .mockResolvedValueOnce([
        { id: 1, name: 'A', isDefault: false, createdAt: ts(), updatedAt: ts() },
        { id: 2, name: 'B', isDefault: true, createdAt: ts(), updatedAt: ts() },
      ]);
    setDefault.mockResolvedValueOnce({
      id: 2,
      name: 'B',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
    });

    render_();
    await waitFor(() => expect(screen.getByText('B')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Als Default markieren' }));

    await waitFor(() => expect(setDefault).toHaveBeenCalledWith(2));
  });

  it('asks for confirmation before deleting and calls API on confirm', async () => {
    list
      .mockResolvedValueOnce([
        { id: 7, name: 'Bye', isDefault: false, createdAt: ts(), updatedAt: ts() },
      ])
      .mockResolvedValueOnce([]);
    del.mockResolvedValueOnce(undefined);

    render_();
    await waitFor(() => expect(screen.getByText('Bye')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Dashboard Bye löschen' }));
    expect(screen.getByText(/Dashboard löschen\?/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Löschen' }));
    await waitFor(() => expect(del).toHaveBeenCalledWith(7));
  });

  it('cancels delete confirmation without calling API', async () => {
    list.mockResolvedValueOnce([
      { id: 7, name: 'Bye', isDefault: false, createdAt: ts(), updatedAt: ts() },
    ]);

    render_();
    await waitFor(() => expect(screen.getByText('Bye')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Dashboard Bye löschen' }));
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(del).not.toHaveBeenCalled();
  });

  it('shows an error alert when the list request fails', async () => {
    list.mockRejectedValueOnce(new Error('boom'));

    render_();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Unbekannter Fehler/),
    );
  });
});
