import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import MobilePage from './MobilePage';
import { NotifyProvider } from '../../notify/NotifyProvider';
import { createKanbanItem } from '../../api/kanban';
import { ApiError } from '../../api/client';
import { clearMobileDevice, isMobileDevice } from '../../auth/mobileDevice';
import { useAuth } from '../../auth/useAuth';

vi.mock('../../api/kanban', () => ({
  createKanbanItem: vi.fn(),
}));

vi.mock('../../auth/mobileDevice', () => ({
  isMobileDevice: vi.fn(() => false),
  clearMobileDevice: vi.fn(),
}));

const signOutMock = vi.fn();
vi.mock('../../auth/useAuth', () => ({
  useAuth: vi.fn(() => ({ signOut: signOutMock })),
}));

const createMock = vi.mocked(createKanbanItem);
const isMobileDeviceMock = vi.mocked(isMobileDevice);
const clearMobileDeviceMock = vi.mocked(clearMobileDevice);
const useAuthMock = vi.mocked(useAuth);

function renderPage(): ReturnType<typeof render> {
  return render(
    <NotifyProvider>
      <MobilePage />
    </NotifyProvider>,
  );
}

describe('MobilePage', () => {
  beforeEach(() => {
    createMock.mockReset();
    clearMobileDeviceMock.mockReset();
    signOutMock.mockReset();
    isMobileDeviceMock.mockReturnValue(false);
    useAuthMock.mockReturnValue({ signOut: signOutMock } as never);
  });
  afterEach(() => cleanup());

  it('rendert die minimale UI mit Titel, Beschreibung und Submit', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: 'Item erstellen' })).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toBeInTheDocument();
    expect(screen.getByLabelText('Beschreibung')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Item erstellen' })).toBeDisabled();
  });

  it('aktiviert den Button erst bei nicht-leerem Titel', async () => {
    renderPage();
    const button = screen.getByRole('button', { name: 'Item erstellen' });
    expect(button).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Titel'), '  ');
    expect(button).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Titel'), 'Idee');
    expect(button).toBeEnabled();
  });

  it('erstellt ein Item im BACKLOG, leert das Formular und meldet Erfolg', async () => {
    createMock.mockResolvedValue({} as never);
    renderPage();
    await userEvent.type(screen.getByLabelText('Titel'), 'Neue Idee');
    await userEvent.type(screen.getByLabelText('Beschreibung'), 'Details');
    await userEvent.click(screen.getByRole('button', { name: 'Item erstellen' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith('Neue Idee', 'Details', 'BACKLOG'),
    );
    expect(await screen.findByText('Item im Backlog erstellt')).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue('');
  });

  it('zeigt keinen Kopplungs-Hinweis auf ungekoppeltem Gerät', () => {
    isMobileDeviceMock.mockReturnValue(false);
    renderPage();
    expect(screen.queryByText('Gerät gekoppelt (30 Tage)')).not.toBeInTheDocument();
  });

  it('zeigt auf gekoppeltem Gerät den Hinweis und hebt die Kopplung auf', async () => {
    isMobileDeviceMock.mockReturnValue(true);
    renderPage();
    expect(screen.getByText('Gerät gekoppelt (30 Tage)')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Kopplung aufheben' }));
    expect(clearMobileDeviceMock).toHaveBeenCalled();
    expect(signOutMock).toHaveBeenCalled();
    expect(await screen.findByText('Kopplung aufgehoben')).toBeInTheDocument();
  });

  it('zeigt eine Fehlermeldung bei API-Fehler und behält die Eingabe', async () => {
    createMock.mockRejectedValue(new ApiError(500, 'Serverfehler', null));
    renderPage();
    await userEvent.type(screen.getByLabelText('Titel'), 'Idee');
    await userEvent.click(screen.getByRole('button', { name: 'Item erstellen' }));

    expect(await screen.findByText(/Erstellen fehlgeschlagen: Serverfehler/)).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('Idee');
  });
});
