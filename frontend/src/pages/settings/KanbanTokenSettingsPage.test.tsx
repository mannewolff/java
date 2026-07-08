import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import KanbanTokenSettingsPage from './KanbanTokenSettingsPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

vi.mock('../../api/kanbanTokens', () => ({
  listKanbanTokens: vi.fn(),
  createKanbanToken: vi.fn(),
  revokeKanbanToken: vi.fn(),
}));

import {
  createKanbanToken,
  listKanbanTokens,
  revokeKanbanToken,
  type KanbanTokenSummary,
} from '../../api/kanbanTokens';

const list = listKanbanTokens as ReturnType<typeof vi.fn>;
const create = createKanbanToken as ReturnType<typeof vi.fn>;
const revoke = revokeKanbanToken as ReturnType<typeof vi.fn>;

function ts(over: Partial<KanbanTokenSummary> = {}): KanbanTokenSummary {
  return {
    id: 1,
    name: 'Board-Adapter',
    createdAt: '2026-07-08T10:00:00Z',
    lastUsedAt: undefined,
    revoked: false,
    ...over,
  };
}

describe('KanbanTokenSettingsPage', () => {
  beforeEach(() => {
    list.mockReset();
    create.mockReset();
    revoke.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  function render_() {
    return render(
      <MemoryRouter>
        <NotifyProvider>
          <KanbanTokenSettingsPage />
        </NotifyProvider>
      </MemoryRouter>,
    );
  }

  it('shows loading skeletons initially', () => {
    list.mockReturnValueOnce(new Promise(() => undefined));

    render_();

    expect(screen.getByLabelText('Tokens werden geladen')).toBeInTheDocument();
  });

  it('shows empty state when no tokens', async () => {
    list.mockResolvedValueOnce([]);

    render_();

    await waitFor(() => expect(screen.getByText(/Noch keine Tokens/)).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /Ersten Token erzeugen/i })).toBeInTheDocument();
  });

  it('renders active and revoked tokens with status chips', async () => {
    list.mockResolvedValueOnce([
      ts({ id: 1, name: 'Laptop', revoked: false }),
      ts({ id: 2, name: 'Alt', revoked: true }),
    ]);

    render_();

    await waitFor(() => expect(screen.getByText('Laptop')).toBeInTheDocument());
    expect(screen.getByText('Alt')).toBeInTheDocument();
    expect(screen.getByText('aktiv')).toBeInTheDocument();
    expect(screen.getByText('widerrufen')).toBeInTheDocument();
  });

  it('reveals the plaintext exactly once on create', async () => {
    list.mockResolvedValue([]);
    create.mockResolvedValueOnce({
      id: 99,
      name: 'Neuer',
      plaintext: 'tk_secret_value',
      createdAt: '2026-07-08T10:00:00Z',
    });

    render_();
    await waitFor(() => expect(screen.getByText(/Noch keine Tokens/)).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Neuen Token erzeugen' }));

    await user.type(screen.getByLabelText('Token-Name'), 'Neuer');
    await user.click(screen.getByRole('button', { name: 'Erzeugen' }));

    await waitFor(() =>
      expect(screen.getByLabelText('Plaintext-Token')).toHaveTextContent('tk_secret_value'),
    );

    await user.click(screen.getByRole('button', { name: 'Verstanden' }));

    await waitFor(() =>
      expect(screen.queryByLabelText('Plaintext-Token')).not.toBeInTheDocument(),
    );
  });

  it('confirms before revoking and calls API on confirm', async () => {
    list
      .mockResolvedValueOnce([ts({ id: 7, name: 'Bye' })])
      .mockResolvedValueOnce([ts({ id: 7, name: 'Bye', revoked: true })]);
    revoke.mockResolvedValueOnce(undefined);

    render_();
    await waitFor(() => expect(screen.getByText('Bye')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Token Bye widerrufen' }));
    expect(screen.getByText(/Token widerrufen\?/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Widerrufen' }));

    await waitFor(() => expect(revoke).toHaveBeenCalledWith(7));
  });

  it('cancels revoke without API call', async () => {
    list.mockResolvedValueOnce([ts({ id: 7, name: 'Bye' })]);

    render_();
    await waitFor(() => expect(screen.getByText('Bye')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Token Bye widerrufen' }));
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(revoke).not.toHaveBeenCalled();
  });

  it('does not render revoke button for already-revoked tokens', async () => {
    list.mockResolvedValueOnce([ts({ id: 1, name: 'Alt', revoked: true })]);

    render_();
    await waitFor(() => expect(screen.getByText('Alt')).toBeInTheDocument());

    expect(
      screen.queryByRole('button', { name: 'Token Alt widerrufen' }),
    ).not.toBeInTheDocument();
  });

  it('shows error alert on list failure', async () => {
    list.mockRejectedValueOnce(new Error('boom'));

    render_();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Unbekannter Fehler/),
    );
  });
});
