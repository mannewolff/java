import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import SettingsPage from './SettingsPage';
import { NotifyProvider } from '../notify/NotifyProvider';
import type { AuthState } from '../auth/useAuth';

let authState: AuthState;

vi.mock('../auth/useAuth', () => ({
  useAuth: () => authState,
}));

function makeAuth(over: Partial<AuthState> = {}): AuthState {
  return {
    isLoading: false,
    isAuthenticated: true,
    username: 'alice',
    email: 'alice@example.com',
    initial: 'A',
    accessToken: 'header.payloadabcdef.signature',
    error: undefined,
    signIn: vi.fn(),
    signOut: vi.fn(),
    ...over,
  };
}

function render_() {
  return render(
    <MemoryRouter>
      <NotifyProvider>
        <SettingsPage />
      </NotifyProvider>
    </MemoryRouter>,
  );
}

describe('SettingsPage — Entwickler / API', () => {
  beforeEach(() => {
    authState = makeAuth();
  });

  afterEach(() => {
    cleanup();
  });

  it('zeigt den maskierten Bearer-Token, nicht den Klartext', () => {
    render_();
    const code = screen.getByLabelText('Maskierter Bearer-Token');
    // Maske = erste 3 + … + letzte 6 Zeichen.
    expect(code).toHaveTextContent('hea…nature');
    expect(code).not.toHaveTextContent('payloadabcdef');
  });

  it('verlinkt die Swagger UI in einem neuen Tab', () => {
    render_();
    const link = screen.getByRole('link', { name: /Swagger UI öffnen/ });
    expect(link).toHaveAttribute('href', '/api/swagger-ui.html');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
  });

  it('kopiert den vollständigen Token und zeigt Feedback', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    const originalClipboard = navigator.clipboard;
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
      writable: true,
    });

    try {
      render_();
      const user = userEvent.setup();

      // userEvent.setup() registriert seinen eigenen Clipboard-Stub — danach
      // erneut patchen, damit unser writeText greift.
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText },
        configurable: true,
        writable: true,
      });

      await user.click(screen.getByRole('button', { name: 'Token kopieren' }));

      expect(writeText).toHaveBeenCalledWith('header.payloadabcdef.signature');
      const alert = await screen.findByRole('alert');
      expect(alert).toHaveTextContent('Token kopiert');
    } finally {
      if (originalClipboard !== undefined) {
        Object.defineProperty(navigator, 'clipboard', {
          value: originalClipboard,
          configurable: true,
          writable: true,
        });
      }
    }
  });

  it('zeigt einen Hinweis, wenn kein Token verfügbar ist', () => {
    authState = makeAuth({ accessToken: undefined });
    render_();
    expect(screen.getByText('Kein Token verfügbar.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Token kopieren' })).not.toBeInTheDocument();
  });
});
