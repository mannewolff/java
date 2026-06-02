import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import MobilePage from './MobilePage';
import { NotifyProvider } from '../../notify/NotifyProvider';
import { createKanbanItem } from '../../api/kanban';
import { ApiError } from '../../api/client';

vi.mock('../../api/kanban', () => ({
  createKanbanItem: vi.fn(),
}));

const createMock = vi.mocked(createKanbanItem);

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

  it('zeigt eine Fehlermeldung bei API-Fehler und behält die Eingabe', async () => {
    createMock.mockRejectedValue(new ApiError(500, 'Serverfehler', null));
    renderPage();
    await userEvent.type(screen.getByLabelText('Titel'), 'Idee');
    await userEvent.click(screen.getByRole('button', { name: 'Item erstellen' }));

    expect(await screen.findByText(/Erstellen fehlgeschlagen: Serverfehler/)).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('Idee');
  });
});
