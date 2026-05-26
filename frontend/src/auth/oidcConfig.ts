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

export const oidcConfig: AuthProviderProps = {
  authority: `${keycloakUrl}/realms/${keycloakRealm}`,
  client_id: keycloakClientId,
  redirect_uri: redirectUri,
  post_logout_redirect_uri: redirectUri,
  response_type: 'code',
  scope: 'openid profile email',
  // Session-Storage statt local-Storage: Tokens verschwinden mit dem Tab, kein
  // Cross-Tab-Leak. Reicht fuer eine Single-User-Toolbox.
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  // PKCE ist Default bei response_type=code in oidc-client-ts ab v3.
  // Token-Endpoint-Antworten via Background-Iframe sind im Dev unzuverlaessig —
  // Silent-Renew via Refresh-Token ist sauberer.
  automaticSilentRenew: true,
  // Entfernt ?code=...&state=... aus der URL nach erfolgreichem Login.
  onSigninCallback: (): void => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
