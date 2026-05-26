import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

import { useAuth } from './useAuth';

interface ProtectedRouteProps {
  children: ReactNode;
}

// Erzwingt einen aktiven Login. Nicht-eingeloggte User werden zum Keycloak-Login
// umgeleitet. Solange der OIDC-Provider noch laedt oder gerade redirected, wird
// ein dezenter Loader gerendert — kein blankes Weiss.
export function ProtectedRoute({ children }: ProtectedRouteProps): JSX.Element {
  const { isAuthenticated, isLoading, error, signIn } = useAuth();

  useEffect(() => {
    if (!isLoading && !isAuthenticated && !error) {
      signIn();
    }
  }, [isLoading, isAuthenticated, error, signIn]);

  if (error) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          gap: 2,
          p: 3,
        }}
        role="alert"
      >
        <Typography variant="h6">Anmeldung fehlgeschlagen</Typography>
        <Typography variant="body2" color="text.secondary">
          Bitte erneut versuchen.
        </Typography>
      </Box>
    );
  }

  if (!isAuthenticated) {
    return (
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
        }}
        aria-busy="true"
      >
        <CircularProgress aria-label="Anmeldung laeuft" />
      </Box>
    );
  }

  return <>{children}</>;
}
