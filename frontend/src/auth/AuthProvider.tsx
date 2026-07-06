import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  AuthProvider as OidcAuthProvider,
  useAuth,
  type AuthProviderProps,
} from 'react-oidc-context';

import { buildOidcConfig } from './oidcConfig';
import { clearReloginGuard, shouldAttemptRelogin } from './reloginGuard';
import {
  setOnAuthExpired,
  setTokenGetter,
  setTokenRefresher,
} from './tokenBridge';

// Wrappt die App im react-oidc-context AuthProvider und verbindet den
// Hook-basierten User-State mit dem nicht-Hook-basierten API-Client
// ueber tokenBridge.

// In-Page-Guard (#233): mehrere 401 innerhalb desselben Seiten-Lebenszyklus (z. B. von mehreren
// Widgets) dürfen nur einen Redirect auslösen. Der reload-feste Teil sitzt in reloginGuard.
let redirectInFlight = false;

// Entprellt den proaktiven Foreground-Refresh (#237): visibilitychange und focus feuern oft
// gemeinsam — ein Renew pro Fenster reicht.
const FOREGROUND_RENEW_DEBOUNCE_MS = 10_000;
let lastForegroundRenewAt = 0;

function AuthBridge({ children }: { children: ReactNode }): JSX.Element {
  const auth = useAuth();

  useEffect(() => {
    // Erfolgreich eingeloggt → reload-festen Loop-Breaker zurücksetzen, damit ein späterer
    // echter 401 wieder ein Re-Login auslösen darf.
    if (auth.isAuthenticated) {
      clearReloginGuard();
    }
    setTokenGetter(() => auth.user?.access_token);
    // On-Demand-Refresh (#237): der API-Client erneuert bei 401 zuerst still über den
    // (Offline-)Refresh-Token, statt sofort den vollen Re-Login auszulösen.
    setTokenRefresher(async () => {
      try {
        const user = await auth.signinSilent();
        return user?.access_token;
      } catch {
        return undefined;
      }
    });
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

  useEffect(() => {
    // Proaktiver Refresh beim Zurückkommen in den Vordergrund (#237). iOS-Safari friert die App
    // im Hintergrund ein; der automaticSilentRenew-Timer feuert dann nicht und der Access-Token
    // (5 Min) ist beim Wiederöffnen abgelaufen. Hier erneuern wir ihn, bevor der erste API-Call
    // mit dem alten Token feuert und einen unnötigen Re-Login auslöst.
    const renewOnForeground = (): void => {
      if (!auth.isAuthenticated) return;
      if (document.visibilityState === 'hidden') return;
      const now = Date.now();
      if (now - lastForegroundRenewAt < FOREGROUND_RENEW_DEBOUNCE_MS) return;
      lastForegroundRenewAt = now;
      void auth.signinSilent().catch(() => undefined);
    };
    document.addEventListener('visibilitychange', renewOnForeground);
    window.addEventListener('focus', renewOnForeground);
    return () => {
      document.removeEventListener('visibilitychange', renewOnForeground);
      window.removeEventListener('focus', renewOnForeground);
    };
  }, [auth]);

  return <>{children}</>;
}

export function AuthProvider({ children }: { children: ReactNode }): JSX.Element {
  // Einmalig beim Mount die (Desktop-)OIDC-Config festlegen. Der useState-Initializer
  // läuft genau einmal und stabil über alle Re-Renders.
  const [config] = useState<AuthProviderProps>(() => buildOidcConfig());

  return (
    <OidcAuthProvider {...config}>
      <AuthBridge>{children}</AuthBridge>
    </OidcAuthProvider>
  );
}
