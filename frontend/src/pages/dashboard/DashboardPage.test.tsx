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
  renameDashboard: vi.fn(),
}));

import { getDashboard, renameDashboard, updateDashboard } from '../../api/dashboard';

const get = getDashboard as ReturnType<typeof vi.fn>;
const update = updateDashboard as ReturnType<typeof vi.fn>;
const rename = renameDashboard as ReturnType<typeof vi.fn>;

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
    rename.mockReset();
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

  // ---- Inline-Rename (#43) ----

  it('renames the dashboard via the inline rename flow', async () => {
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
    rename.mockResolvedValueOnce({ ...initial, name: 'Renamed' });

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByText('Main')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Dashboard umbenennen' }));

    const input = screen.getByLabelText('Neuer Dashboard-Name') as HTMLInputElement;
    expect(input.value).toBe('Main');
    await user.clear(input);
    await user.type(input, 'Renamed');
    await user.click(screen.getByRole('button', { name: 'Umbenennen speichern' }));

    await waitFor(() => expect(rename).toHaveBeenCalledWith(1, 'Renamed'));
    await waitFor(() => expect(screen.getByText('Renamed')).toBeInTheDocument());
  });

  it('shows an inline error when renaming to an empty name', async () => {
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

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByText('Main')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Dashboard umbenennen' }));
    const input = screen.getByLabelText('Neuer Dashboard-Name');
    await user.clear(input);
    await user.click(screen.getByRole('button', { name: 'Umbenennen speichern' }));

    expect(screen.getByText(/Name darf nicht leer sein/)).toBeInTheDocument();
    expect(rename).not.toHaveBeenCalled();
  });

  // ---- Widget-Lösch-Confirm (#43) ----

  it('opens a confirm dialog when widget delete is clicked', async () => {
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

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /Bearbeiten/ })).toBeInTheDocument());
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Bearbeiten/ }));

    // Warte bis Edit-Mode-Indikator (Speichern-Button) da ist  jetzt sind auch die
    // Widget-IconButtons gerendert.
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Textbox löschen' })).toBeInTheDocument(),
    );

    // Trash-Icon → Confirm-Dialog
    await user.click(screen.getByRole('button', { name: 'Textbox löschen' }));
    expect(screen.getByText(/Widget löschen\?/)).toBeInTheDocument();
  });

  it('removes the widget from the draft when delete is confirmed', async () => {
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

    await act(async () => {
      render_();
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /Bearbeiten/ })).toBeInTheDocument());
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Bearbeiten/ }));

    await user.click(screen.getByRole('button', { name: 'Textbox löschen' }));
    await user.click(screen.getByRole('button', { name: 'Löschen' }));

    // Widget verschwindet aus dem Render — der Delete-Button ist nicht mehr da.
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Textbox löschen' })).not.toBeInTheDocument(),
    );
  });
});
