import { WebStorageStateStore } from 'oidc-client-ts';
import type { AuthProviderProps } from 'react-oidc-context';

import { clearReloginGuard } from './reloginGuard';

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
 * Baut die OIDC-Konfiguration für die Web-UI (reiner Desktop-Client, #334): Token in
 * sessionStorage (verschwindet mit dem Tab, kein Cross-Tab-Leak) und Scope ohne
 * `offline_access` — es wird bewusst kein langlebiger Offline-Refresh-Token angefragt.
 */
export function buildOidcConfig(): AuthProviderProps {
  return {
    authority: `${keycloakUrl}/realms/${keycloakRealm}`,
    client_id: keycloakClientId,
    redirect_uri: redirectUri,
    post_logout_redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid profile email',
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    // PKCE ist Default bei response_type=code in oidc-client-ts ab v3.
    // Token-Endpoint-Antworten via Background-Iframe sind im Dev unzuverlaessig —
    // Silent-Renew via Refresh-Token ist sauberer.
    automaticSilentRenew: true,
    // Beim Logout Access- UND Refresh-Token serverseitig am Revocation-Endpoint invalidieren
    // (#312). Allgemeine Sicherheits-Hygiene: ein nicht revozierter Refresh-Token bliebe bis
    // zum Ablauf einlösbar — ein Restrisiko bei XSS oder Gerätezugriff. Bewusst auch nach dem
    // Wegfall des Mobile-/Offline-Pfads beibehalten (#334). Default der Property ist false.
    revokeTokensOnSignout: true,
    // Entfernt ?code=...&state=... aus der URL nach erfolgreichem Login und setzt den
    // reload-festen Re-Login-Loop-Breaker zurück (#233).
    onSigninCallback: (): void => {
      clearReloginGuard();
      window.history.replaceState({}, document.title, window.location.pathname);
    },
  };
}
