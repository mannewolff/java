import { describe, expect, it } from 'vitest';
import type { WebStorageStateStore } from 'oidc-client-ts';

import { buildOidcConfig, keycloakAccountConsoleUrl } from './oidcConfig';

// buildOidcConfig liefert die Settings-Variante der AuthProviderProps-Union; für die
// Assertions auf einzelne Felder schmälern wir den Typ gezielt.
interface OidcSettings {
  scope: string;
  response_type: string;
  automaticSilentRenew: boolean;
  revokeTokensOnSignout: boolean;
  userStore: WebStorageStateStore;
}
const settings = (mobile: boolean): OidcSettings =>
  buildOidcConfig(mobile) as unknown as OidcSettings;

describe('buildOidcConfig', () => {
  it('fragt im Desktop-Modus keinen offline_access an und schreibt in sessionStorage', async () => {
    window.sessionStorage.clear();
    window.localStorage.clear();
    const cfg = settings(false);
    expect(cfg.scope).toBe('openid profile email');
    await cfg.userStore.set('probe', 'v');
    expect(Object.keys(window.sessionStorage).some((k) => k.includes('probe'))).toBe(true);
    expect(Object.keys(window.localStorage).some((k) => k.includes('probe'))).toBe(false);
  });

  it('fragt im Mobile-Modus offline_access an und schreibt in localStorage', async () => {
    window.sessionStorage.clear();
    window.localStorage.clear();
    const cfg = settings(true);
    expect(cfg.scope).toBe('openid profile email offline_access');
    await cfg.userStore.set('probe', 'v');
    expect(Object.keys(window.localStorage).some((k) => k.includes('probe'))).toBe(true);
    expect(Object.keys(window.sessionStorage).some((k) => k.includes('probe'))).toBe(false);
  });

  it('setzt PKCE-typische Felder (code-Flow, silent renew)', () => {
    const cfg = settings(true);
    expect(cfg.response_type).toBe('code');
    expect(cfg.automaticSilentRenew).toBe(true);
  });

  it('revoziert Tokens beim Logout serverseitig (#312)', () => {
    // In beiden Modi aktiv — der Offline-Token im Mobile-localStorage darf den Logout
    // nicht überleben, aber auch der Desktop-Refresh-Token soll sauber invalidiert werden.
    expect(settings(true).revokeTokensOnSignout).toBe(true);
    expect(settings(false).revokeTokensOnSignout).toBe(true);
  });

  it('baut die Keycloak-Account-Console-URL', () => {
    expect(keycloakAccountConsoleUrl).toMatch(/\/realms\/.+\/account$/);
  });
});
