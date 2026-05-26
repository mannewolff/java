import { useAuth as useOidcAuth } from 'react-oidc-context';

export interface AuthState {
  isLoading: boolean;
  isAuthenticated: boolean;
  username: string | undefined;
  email: string | undefined;
  // Initialen fuer Avatar-Anzeige; '?' wenn weder Username noch Email vorhanden.
  initial: string;
  error: Error | undefined;
  signIn: () => void;
  signOut: () => void;
}

// Eigener Hook auf Basis von react-oidc-context. Liefert die Felder, die das UI
// tatsaechlich braucht (Username, Initial, Aktionen) und entkoppelt die App vom
// genauen Profil-Claim-Layout.
export function useAuth(): AuthState {
  const oidc = useOidcAuth();

  const username =
    (oidc.user?.profile.preferred_username as string | undefined) ??
    (oidc.user?.profile.name as string | undefined) ??
    (oidc.user?.profile.email as string | undefined);

  const email = oidc.user?.profile.email as string | undefined;

  const initial = (username ?? email ?? '?').trim().charAt(0).toUpperCase() || '?';

  return {
    isLoading: oidc.isLoading,
    isAuthenticated: oidc.isAuthenticated,
    username,
    email,
    initial,
    error: oidc.error,
    signIn: () => {
      void oidc.signinRedirect();
    },
    signOut: () => {
      void oidc.signoutRedirect();
    },
  };
}
