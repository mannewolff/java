import { describe, expect, it, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import type { AuthContextProps } from 'react-oidc-context';

import { useAuth } from './useAuth';

vi.mock('react-oidc-context', () => ({
  useAuth: vi.fn(),
}));

import { useAuth as useOidcAuthMock } from 'react-oidc-context';

const useOidcAuth = useOidcAuthMock as unknown as ReturnType<typeof vi.fn>;

function mockOidc(state: Partial<AuthContextProps>): void {
  useOidcAuth.mockReturnValue({
    isLoading: false,
    isAuthenticated: false,
    user: undefined,
    error: undefined,
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    ...state,
  } as unknown as AuthContextProps);
}

describe('useAuth', () => {
  it('returns isAuthenticated=false when no user is signed in', () => {
    mockOidc({ isAuthenticated: false });

    const { result } = renderHook(() => useAuth());

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.username).toBeUndefined();
    expect(result.current.initial).toBe('?');
  });

  it('exposes preferred_username and uppercased initial when authenticated', () => {
    mockOidc({
      isAuthenticated: true,
      user: {
        access_token: 'tok',
        profile: { preferred_username: 'alice', email: 'alice@example.com' },
      } as unknown as AuthContextProps['user'],
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.username).toBe('alice');
    expect(result.current.email).toBe('alice@example.com');
    expect(result.current.initial).toBe('A');
  });

  it('falls back to email when preferred_username is missing', () => {
    mockOidc({
      isAuthenticated: true,
      user: {
        access_token: 'tok',
        profile: { email: 'bob@example.com' },
      } as unknown as AuthContextProps['user'],
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.username).toBe('bob@example.com');
    expect(result.current.initial).toBe('B');
  });

  it('invokes signinRedirect when signIn() is called', () => {
    const signinRedirect = vi.fn();
    mockOidc({ signinRedirect });

    const { result } = renderHook(() => useAuth());
    result.current.signIn();

    expect(signinRedirect).toHaveBeenCalledTimes(1);
  });

  it('invokes signoutRedirect when signOut() is called', () => {
    const signoutRedirect = vi.fn();
    mockOidc({ signoutRedirect });

    const { result } = renderHook(() => useAuth());
    result.current.signOut();

    expect(signoutRedirect).toHaveBeenCalledTimes(1);
  });
});
