import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Alert, Box, CircularProgress } from '@mui/material';

import { listDashboards, createDashboard } from '../../api/dashboard';
import { ApiError } from '../../api/client';

/**
 * Landet auf {@code /dashboards/default} und entscheidet wohin als nächstes:
 *
 * <ul>
 *   <li>User hat ein Default-Dashboard → Redirect auf {@code /dashboards/&lt;id&gt;}</li>
 *   <li>User hat Dashboards, aber kein Default → erstes davon</li>
 *   <li>User hat noch nichts → automatisch ein erstes "Mein Dashboard" anlegen und dort hin</li>
 * </ul>
 */
export default function DashboardDefaultRedirect(): JSX.Element {
  const [targetId, setTargetId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const list = await listDashboards();
        if (cancelled) return;
        if (list.length === 0) {
          // Auto-Bootstrap: erstes Dashboard für den User anlegen.
          const created = await createDashboard('Mein Dashboard');
          if (cancelled) return;
          setTargetId(created.id);
          return;
        }
        const def = list.find((d) => d.isDefault) ?? list[0];
        setTargetId(def.id);
      } catch (e) {
        if (cancelled) return;
        setError(e instanceof ApiError ? e.message : 'Unbekannter Fehler');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">Dashboards konnten nicht geladen werden: {error}</Alert>
      </Box>
    );
  }

  if (targetId === null) {
    return (
      <Box
        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 200 }}
        aria-busy="true"
      >
        <CircularProgress aria-label="Dashboard wird geladen" />
      </Box>
    );
  }

  return <Navigate to={`/dashboards/${targetId}`} replace />;
}
