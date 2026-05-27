import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import DashboardDefaultRedirect from './DashboardDefaultRedirect';

vi.mock('../../api/dashboard', () => ({
  listDashboards: vi.fn(),
  createDashboard: vi.fn(),
}));

import { createDashboard, listDashboards } from '../../api/dashboard';

const list = listDashboards as ReturnType<typeof vi.fn>;
const create = createDashboard as ReturnType<typeof vi.fn>;

function ts(): string {
  return '2026-05-26T10:00:00Z';
}

function render_() {
  return render(
    <MemoryRouter initialEntries={['/dashboards/default']}>
      <Routes>
        <Route path="/dashboards/default" element={<DashboardDefaultRedirect />} />
        <Route path="/dashboards/:id" element={<div>landed-on-detail</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DashboardDefaultRedirect', () => {
  beforeEach(() => {
    list.mockReset();
    create.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it('redirects to the marked default dashboard', async () => {
    list.mockResolvedValueOnce([
      { id: 7, name: 'Default', isDefault: true, createdAt: ts(), updatedAt: ts() },
      { id: 8, name: 'Other', isDefault: false, createdAt: ts(), updatedAt: ts() },
    ]);

    render_();

    await waitFor(() => expect(screen.getByText('landed-on-detail')).toBeInTheDocument());
    expect(create).not.toHaveBeenCalled();
  });

  it('falls back to the first dashboard when no default is set', async () => {
    list.mockResolvedValueOnce([
      { id: 3, name: 'First', isDefault: false, createdAt: ts(), updatedAt: ts() },
      { id: 4, name: 'Second', isDefault: false, createdAt: ts(), updatedAt: ts() },
    ]);

    render_();

    await waitFor(() => expect(screen.getByText('landed-on-detail')).toBeInTheDocument());
    expect(create).not.toHaveBeenCalled();
  });

  it('auto-creates a first dashboard when the user has none', async () => {
    list.mockResolvedValueOnce([]);
    create.mockResolvedValueOnce({
      id: 99,
      name: 'Mein Dashboard',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
    });

    render_();

    await waitFor(() => expect(create).toHaveBeenCalledWith('Mein Dashboard'));
    await waitFor(() => expect(screen.getByText('landed-on-detail')).toBeInTheDocument());
  });

  it('shows an error alert when the list call fails', async () => {
    list.mockRejectedValueOnce(new Error('network down'));

    render_();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Unbekannter Fehler/),
    );
  });
});
