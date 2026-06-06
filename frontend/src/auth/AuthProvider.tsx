import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  AuthProvider as OidcAuthProvider,
  useAuth,
  type AuthProviderProps,
} from 'react-oidc-context';

import { isMobileDevice, markMobileDeviceFromUrl } from './mobileDevice';
import { buildOidcConfig } from './oidcConfig';
import { setOnAuthExpired, setTokenGetter } from './tokenBridge';

// Wrappt die App im react-oidc-context AuthProvider und verbindet den
// Hook-basierten User-State mit dem nicht-Hook-basierten API-Client
// ueber tokenBridge.

// Loop-Guard (#233): Sobald ein 401 einen Re-Login-Redirect anstößt, navigiert der Browser
// komplett zu Keycloak — bis dahin dürfen weitere 401 (z. B. von mehreren Widgets) keinen
// zweiten Redirect auslösen, sonst entsteht ein Endlos-"Zucken". Das Flag lebt modulweit, weil
// signinRedirect die Seite ohnehin neu lädt; schlägt der Redirect fehl, wird es zurückgesetzt.
let redirectInFlight = false;

function AuthBridge({ children }: { children: ReactNode }): JSX.Element {
  const auth = useAuth();

  useEffect(() => {
    setTokenGetter(() => auth.user?.access_token);
    setOnAuthExpired(() => {
      if (redirectInFlight) return;
      redirectInFlight = true;
      void auth.signinRedirect().catch(() => {
        redirectInFlight = false;
      });
    });
  }, [auth]);

  return <>{children}</>;
}

export function AuthProvider({ children }: { children: ReactNode }): JSX.Element {
  // Einmalig beim Mount: Pairing-Flag aus der URL auswerten und die passende
  // Config (Mobile = localStorage + offline_access, sonst Desktop) festlegen.
  // useState-Initializer läuft genau einmal und stabil über alle Re-Renders.
  const [config] = useState<AuthProviderProps>(() => {
    markMobileDeviceFromUrl();
    return buildOidcConfig(isMobileDevice());
  });

  return (
    <OidcAuthProvider {...config}>
      <AuthBridge>{children}</AuthBridge>
    </OidcAuthProvider>
  );
}
