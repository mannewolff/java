import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import DashboardPage from './DashboardPage';
import { EditModeProvider } from './EditModeContext';
import { DESKTOP_MIN_WIDTH, WIDGET_DEFAULTS } from './widgetDefaults';

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
      <EditModeProvider>
        <Routes>
          <Route path="/dashboards/:id" element={<DashboardPage />} />
          <Route path="/dashboards" element={<div>Liste</div>} />
        </Routes>
      </EditModeProvider>
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

  it('zeigt Empty-State und schaltet automatisch in den Edit-Modus bei 0 Widgets', async () => {
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
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByText('Main')).toBeInTheDocument());
    expect(screen.getByText(/leer/)).toBeInTheDocument();
    // Auto-Edit: Buttons "Speichern" + "Abbrechen" sind sichtbar, "Bearbeiten" nicht.
    expect(screen.getByRole('button', { name: /Speichern/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Abbrechen/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Bearbeiten/ })).not.toBeInTheDocument();
  });

  it('Read-Modus per Default bei Dashboard mit Widgets', async () => {
    get.mockResolvedValueOnce({
      id: 1,
      name: 'Main',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
      widgets: [
        {
          id: 7,
          type: 'TEXTBOX',
          posX: 0,
          posY: 0,
          width: 4,
          height: 3,
          config: JSON.stringify({ markdown: '# Hello' }),
        },
      ],
    });

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /Bearbeiten/ })).toBeInTheDocument());
    // Im Read-Modus keine Edit-Icons in den Widgets.
    expect(screen.queryByRole('button', { name: 'Textbox bearbeiten' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Textbox löschen' })).not.toBeInTheDocument();
  });

  it('Bearbeiten-Button schaltet in den Edit-Modus, Edit-Icons werden sichtbar', async () => {
    get.mockResolvedValueOnce({
      id: 1,
      name: 'Main',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
      widgets: [
        {
          id: 7,
          type: 'TEXTBOX',
          posX: 0,
          posY: 0,
          width: 4,
          height: 3,
          config: JSON.stringify({ markdown: '# Hello' }),
        },
      ],
    });

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /Bearbeiten/ })).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Bearbeiten/ }));

    expect(screen.getByRole('button', { name: 'Textbox bearbeiten' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Speichern/ })).toBeInTheDocument();
  });

  it('Speichern ruft updateDashboard genau einmal mit den aktuellen Widgets', async () => {
    const initial = {
      id: 1,
      name: 'Main',
      isDefault: true,
      createdAt: ts(),
      updatedAt: ts(),
      widgets: [
        {
          id: 7,
          type: 'TEXTBOX' as const,
          posX: 0,
          posY: 0,
          width: 4,
          height: 3,
          config: JSON.stringify({ markdown: '# Hello' }),
        },
      ],
    };
    get.mockResolvedValueOnce(initial);
    update.mockResolvedValueOnce(initial);

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /Bearbeiten/ })).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Bearbeiten/ }));
    await user.click(screen.getByRole('button', { name: /Speichern/ }));

    await waitFor(() => expect(update).toHaveBeenCalledTimes(1));
    expect(update).toHaveBeenCalledWith(1, initial.widgets);
  });

  it('shows a not-found message when API returns 404', async () => {
    const err = new Error('not found') as Error & { status?: number; name: string };
    err.name = 'ApiError';
    (err as { status: number }).status = 404;
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
});
