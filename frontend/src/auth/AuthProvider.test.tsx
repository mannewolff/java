import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ReactNode } from 'react';
import { render } from '@testing-library/react';

const signinRedirect = vi.fn().mockResolvedValue(undefined);

// react-oidc-context durch No-Op-Provider + Stub-useAuth ersetzen, damit nur die
// AuthBridge-Verdrahtung (tokenBridge + Loop-Guard) getestet wird.
vi.mock('react-oidc-context', () => ({
  AuthProvider: ({ children }: { children: ReactNode }) => <>{children}</>,
  useAuth: () => ({ user: { access_token: 'tok' }, signinRedirect }),
}));

import { AuthProvider } from './AuthProvider';
import { __resetAuthBridge, notifyAuthExpired } from './tokenBridge';

describe('AuthProvider Re-Login-Loop-Guard (#233)', () => {
  beforeEach(() => {
    __resetAuthBridge();
    signinRedirect.mockClear();
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
});
