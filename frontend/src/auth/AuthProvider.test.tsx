import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ReactNode } from 'react';
import { render } from '@testing-library/react';

const signinRedirect = vi.fn().mockResolvedValue(undefined);
const signinSilent = vi.fn().mockResolvedValue({ access_token: 'tok2' });

// react-oidc-context durch No-Op-Provider + Stub-useAuth ersetzen, damit nur die
// AuthBridge-Verdrahtung (tokenBridge + Loop-Guard + Foreground-Refresh) getestet wird.
vi.mock('react-oidc-context', () => ({
  AuthProvider: ({ children }: { children: ReactNode }) => <>{children}</>,
  useAuth: () => ({
    user: { access_token: 'tok' },
    isAuthenticated: true,
    signinRedirect,
    signinSilent,
  }),
}));

import { AuthProvider } from './AuthProvider';
import {
  __resetAuthBridge,
  notifyAuthExpired,
  tryRefreshToken,
} from './tokenBridge';

describe('AuthProvider Re-Login-Loop-Guard (#233)', () => {
  beforeEach(() => {
    __resetAuthBridge();
    signinRedirect.mockClear();
    signinSilent.mockClear();
    window.sessionStorage.clear();
  });

  it('löst bei wiederholten auth-expired-Events höchstens einen signinRedirect aus', () => {
    render(
      <AuthProvider>
        <div />
      </AuthProvider>,
    );

    notifyAuthExpired();
    notifyAuthExpired();
    notifyAuthExpired();

    expect(signinRedirect).toHaveBeenCalledTimes(1);
  });

  it('registriert signinSilent als Token-Refresher (#237)', async () => {
    render(
      <AuthProvider>
        <div />
      </AuthProvider>,
    );

    await expect(tryRefreshToken()).resolves.toBe('tok2');
    expect(signinSilent).toHaveBeenCalledTimes(1);
  });

  it('erneuert den Token still, wenn die App in den Vordergrund kommt (#237)', () => {
    render(
      <AuthProvider>
        <div />
      </AuthProvider>,
    );

    window.dispatchEvent(new Event('focus'));

    expect(signinSilent).toHaveBeenCalledTimes(1);
    expect(signinRedirect).not.toHaveBeenCalled();
  });
});
