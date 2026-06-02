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

function AuthBridge({ children }: { children: ReactNode }): JSX.Element {
  const auth = useAuth();

  useEffect(() => {
    setTokenGetter(() => auth.user?.access_token);
    setOnAuthExpired(() => {
      void auth.signinRedirect();
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
