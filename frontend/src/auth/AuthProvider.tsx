import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { AuthProvider as OidcAuthProvider, useAuth } from 'react-oidc-context';

import { oidcConfig } from './oidcConfig';
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
  return (
    <OidcAuthProvider {...oidcConfig}>
      <AuthBridge>{children}</AuthBridge>
    </OidcAuthProvider>
  );
}
