import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  AuthProvider as OidcAuthProvider,
  useAuth,
  type AuthProviderProps,
} from 'react-oidc-context';

import { isMobileDevice, markMobileDeviceFromUrl } from './mobileDevice';
import { buildOidcConfig } from './oidcConfig';
import { clearReloginGuard, shouldAttemptRelogin } from './reloginGuard';
import { setOnAuthExpired, setTokenGetter } from './tokenBridge';

// Wrappt die App im react-oidc-context AuthProvider und verbindet den
// Hook-basierten User-State mit dem nicht-Hook-basierten API-Client
// ueber tokenBridge.

// In-Page-Guard (#233): mehrere 401 innerhalb desselben Seiten-Lebenszyklus (z. B. von mehreren
// Widgets) dürfen nur einen Redirect auslösen. Der reload-feste Teil sitzt in reloginGuard.
let redirectInFlight = false;

function AuthBridge({ children }: { children: ReactNode }): JSX.Element {
  const auth = useAuth();

  useEffect(() => {
    // Erfolgreich eingeloggt → reload-festen Loop-Breaker zurücksetzen, damit ein späterer
    // echter 401 wieder ein Re-Login auslösen darf.
    if (auth.isAuthenticated) {
      clearReloginGuard();
    }
    setTokenGetter(() => auth.user?.access_token);
    setOnAuthExpired(() => {
      if (redirectInFlight) return;
      // Reload-fester Loop-Breaker: behebt das Re-Login den 401 nicht, wird nach einem
      // Versuch gestoppt statt endlos neu zu laden (#233).
      if (!shouldAttemptRelogin()) return;
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
