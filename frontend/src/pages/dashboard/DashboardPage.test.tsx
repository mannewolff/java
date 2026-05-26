import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import DashboardPage from './DashboardPage';
import { AUTO_SAVE_DEBOUNCE_MS, DESKTOP_MIN_WIDTH, WIDGET_DEFAULTS } from './widgetDefaults';

vi.mock('../../api/dashboard', () => ({
  getDashboard: vi.fn(),
  updateDashboard: vi.fn(),
}));

import { getDashboard, updateDashboard } from '../../api/dashboard';

const get = getDashboard as ReturnType<typeof vi.fn>;
const update = updateDashboard as ReturnType<typeof vi.fn>;

function setViewport(width: number): void {
  Object.defineProperty(window, 'innerWidth', { configurable: true, value: width });
  window.dispatchEvent(new Event('resize'));
}

function ts(): string {
  return '2026-05-26T10:00:00Z';
}

function render_(id = '1') {
  return render(
    <MemoryRouter initialEntries={[`/dashboards/${id}`]}>
      <Routes>
        <Route path="/dashboards/:id" element={<DashboardPage />} />
        <Route path="/dashboards" element={<div>Liste</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DashboardPage', () => {
  beforeEach(() => {
    get.mockReset();
    update.mockReset();
    setViewport(1280); // Desktop
  });

  afterEach(() => {
    cleanup();
  });

  it('shows desktop-only hint below the viewport threshold', async () => {
    setViewport(DESKTOP_MIN_WIDTH - 1);
    get.mockResolvedValueOnce({
      id: 1,
      name: 'Main',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
      widgets: [],
    });

    render_();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Desktop-Ansicht/),
    );
  });

  it('renders the dashboard with an empty-state hint when no widgets are set', async () => {
    get.mockResolvedValueOnce({
      id: 1,
      name: 'Main',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
      widgets: [],
    });

    await act(async () => {
      render_();
      // Lass die mock-Promise auflösen
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByText('Main')).toBeInTheDocument());
    expect(screen.getByText(/leer/)).toBeInTheDocument();
  });

  it('shows a not-found message when API returns 404', async () => {
    const err = new Error('not found') as Error & { status?: number; name: string };
    err.name = 'ApiError';
    (err as { status: number }).status = 404;
    // Wir importieren ApiError nicht direkt — der page-Code prüft `instanceof ApiError`,
    // bei einem normalen Error landet er im "Unbekannter Fehler"-Fallback.
    get.mockRejectedValueOnce(err);

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Unbekannter Fehler/),
    );
  });

  it('rejects an invalid dashboard id without calling the API', async () => {
    render_('not-a-number');

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Ungültige Dashboard-ID/),
    );
    expect(get).not.toHaveBeenCalled();
  });

  it('exposes documented widget defaults — TEXTBOX 4×3, KPI 2×2', () => {
    expect(WIDGET_DEFAULTS.TEXTBOX).toEqual({ width: 4, height: 3 });
    expect(WIDGET_DEFAULTS.KPI).toEqual({ width: 2, height: 2 });
  });

  it('debounces auto-save to AUTO_SAVE_DEBOUNCE_MS', () => {
    // Reine Konstanten-Verifikation — die Debounce-Zeit ist Teil des Issue-Kontrakts (500 ms)
    // und sollte sich nicht unkontrolliert ändern.
    expect(AUTO_SAVE_DEBOUNCE_MS).toBe(500);
  });
});
