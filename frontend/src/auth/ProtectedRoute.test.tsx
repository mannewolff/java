import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

import { ProtectedRoute } from './ProtectedRoute';

vi.mock('./useAuth', () => ({
  useAuth: vi.fn(),
}));

import { useAuth as useAuthMock } from './useAuth';

const useAuth = useAuthMock as unknown as ReturnType<typeof vi.fn>;

interface MockAuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  error: Error | undefined;
  signIn: () => void;
}

function setAuth(overrides: Partial<MockAuthState> = {}): { signIn: () => void } {
  const signIn = overrides.signIn ?? vi.fn();
  useAuth.mockReturnValue({
    isAuthenticated: false,
    isLoading: false,
    username: undefined,
    email: undefined,
    initial: '?',
    error: undefined,
    signIn,
    signOut: vi.fn(),
    ...overrides,
  });
  return { signIn };
}

describe('ProtectedRoute', () => {
  it('renders children when authenticated', () => {
    setAuth({ isAuthenticated: true });

    render(
      <ProtectedRoute>
        <div>protected-content</div>
      </ProtectedRoute>,
    );

    expect(screen.getByText('protected-content')).toBeInTheDocument();
  });

  it('shows a loading state and redirects to sign-in when unauthenticated', () => {
    const { signIn } = setAuth({ isAuthenticated: false });

    render(
      <ProtectedRoute>
        <div>protected-content</div>
      </ProtectedRoute>,
    );

    expect(screen.queryByText('protected-content')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Anmeldung laeuft')).toBeInTheDocument();
    expect(signIn).toHaveBeenCalledTimes(1);
  });

  it('does not redirect while auth is still loading', () => {
    const { signIn } = setAuth({ isAuthenticated: false, isLoading: true });

    render(
      <ProtectedRoute>
        <div>protected-content</div>
      </ProtectedRoute>,
    );

    expect(signIn).not.toHaveBeenCalled();
  });

  it('renders an error state and does not redirect when auth errored', () => {
    const { signIn } = setAuth({
      isAuthenticated: false,
      error: new Error('boom'),
    });

    render(
      <ProtectedRoute>
        <div>protected-content</div>
      </ProtectedRoute>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Anmeldung fehlgeschlagen');
    expect(signIn).not.toHaveBeenCalled();
  });
});
