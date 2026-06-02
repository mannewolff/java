import { WebStorageStateStore } from 'oidc-client-ts';
import type { AuthProviderProps } from 'react-oidc-context';

// Liest die OIDC-Verbindungsdaten aus den VITE_-Variablen. Defaults zielen auf den
// lokalen Keycloak-Dev-Container und sind so geschnitten, dass die App ohne extra
// .env beim "npm run dev" startet.
const keycloakUrl: string =
  import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081';
const keycloakRealm: string =
  import.meta.env.VITE_KEYCLOAK_REALM ?? 'toolbox-dev';
const keycloakClientId: string =
  import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'toolbox-web';

const redirectUri: string =
  typeof window === 'undefined' ? 'http://localhost:5173' : window.location.origin;

/** Link auf die Keycloak-Account-Console — dort kann der User Geräte/Sessions widerrufen (#206). */
export const keycloakAccountConsoleUrl = `${keycloakUrl}/realms/${keycloakRealm}/account`;

/**
 * Baut die OIDC-Konfiguration. Im Mobile-Modus (#206) wird der Token persistent in
 * localStorage gehalten und `offline_access` angefragt — damit erhält das Handy einen
 * Offline-Refresh-Token und bleibt bis zu 30 Tage ohne Neuanmeldung eingeloggt.
 *
 * Desktop-Default: sessionStorage (Token verschwindet mit dem Tab, kein Cross-Tab-Leak)
 * und kein offline_access.
 */
export function buildOidcConfig(mobile: boolean): AuthProviderProps {
  const store: Storage = mobile ? window.localStorage : window.sessionStorage;
  const scope: string = mobile
    ? 'openid profile email offline_access'
    : 'openid profile email';

  return {
    authority: `${keycloakUrl}/realms/${keycloakRealm}`,
    client_id: keycloakClientId,
    redirect_uri: redirectUri,
    post_logout_redirect_uri: redirectUri,
    response_type: 'code',
    scope,
    userStore: new WebStorageStateStore({ store }),
    // PKCE ist Default bei response_type=code in oidc-client-ts ab v3.
    // Token-Endpoint-Antworten via Background-Iframe sind im Dev unzuverlaessig —
    // Silent-Renew via Refresh-Token ist sauberer.
    automaticSilentRenew: true,
    // Entfernt ?code=...&state=... aus der URL nach erfolgreichem Login.
    onSigninCallback: (): void => {
      window.history.replaceState({}, document.title, window.location.pathname);
    },
  };
}
