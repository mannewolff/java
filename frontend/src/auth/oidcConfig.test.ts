import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
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
const settings = (): OidcSettings => buildOidcConfig() as unknown as OidcSettings;

// Node v26/jsdom liefert window.sessionStorage nicht zuverlässig — In-Memory-Stub, damit der
// userStore-Probe-Schreibzugriff beobachtbar ist.
function memoryStorage(): Storage {
  const store = new Map<string, string>();
  return {
    getItem: (k) => store.get(k) ?? null,
    setItem: (k, v) => void store.set(k, String(v)),
    removeItem: (k) => void store.delete(k),
    clear: () => store.clear(),
    key: (i) => [...store.keys()][i] ?? null,
    get length() {
      return store.size;
    },
  } as Storage;
}

describe('buildOidcConfig', () => {
  beforeEach(() => {
    vi.stubGlobal('sessionStorage', memoryStorage());
    vi.stubGlobal('localStorage', memoryStorage());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('fragt keinen offline_access an und schreibt in sessionStorage (Desktop-only, #334)', async () => {
    const cfg = settings();
    expect(cfg.scope).toBe('openid profile email');
    expect(cfg.scope).not.toContain('offline_access');

    // WebStorageStateStore legt unter dem Präfix "oidc." ab.
    await cfg.userStore.set('probe', 'v');
    expect(window.sessionStorage.getItem('oidc.probe')).toBe('v');
    expect(window.localStorage.getItem('oidc.probe')).toBeNull();
  });

  it('setzt PKCE-typische Felder (code-Flow, silent renew)', () => {
    const cfg = settings();
    expect(cfg.response_type).toBe('code');
    expect(cfg.automaticSilentRenew).toBe(true);
  });

  it('revoziert Tokens beim Logout serverseitig (#312, bleibt nach #334 aktiv)', () => {
    expect(settings().revokeTokensOnSignout).toBe(true);
  });

  it('baut die Keycloak-Account-Console-URL', () => {
    expect(keycloakAccountConsoleUrl).toMatch(/\/realms\/.+\/account$/);
  });
});
