import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { Responsive as ResponsiveGridLayout } from 'react-grid-layout';
import type { Layout } from 'react-grid-layout';

import {
  getDashboard,
  updateDashboard,
  type DashboardDetail,
  type WidgetDto,
} from '../../api/dashboard';
import { ApiError } from '../../api/client';
import {
  AUTO_SAVE_DEBOUNCE_MS,
  DESKTOP_MIN_WIDTH,
  GRID_COLS,
  GRID_ROW_HEIGHT,
} from './widgetDefaults';
import useViewportWidth from './useViewportWidth';

type SaveState = 'idle' | 'pending' | 'saved' | { kind: 'error'; message: string };

/** Eindeutige Grid-Item-Keys — wir nehmen die DB-ID, oder ein lokaler Prefix für neue. */
function widgetKey(widget: WidgetDto, fallbackIndex: number): string {
  return widget.id != null ? `w-${widget.id}` : `new-${fallbackIndex}`;
}

function toLayouts(widgets: WidgetDto[]): Layout[] {
  return widgets.map((w, i) => ({
    i: widgetKey(w, i),
    x: w.posX,
    y: w.posY,
    w: w.width,
    h: w.height,
  }));
}

export default function DashboardPage(): JSX.Element {
  const { id } = useParams<{ id: string }>();
  const dashboardId = Number(id);
  const navigate = useNavigate();
  const viewportWidth = useViewportWidth();
  const isDesktop = viewportWidth >= DESKTOP_MIN_WIDTH;

  const [detail, setDetail] = useState<DashboardDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saveState, setSaveState] = useState<SaveState>('idle');
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!Number.isFinite(dashboardId)) {
      setError('Ungültige Dashboard-ID');
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const data = await getDashboard(dashboardId);
        if (!cancelled) setDetail(data);
      } catch (e) {
        if (!cancelled) {
          setError(
            e instanceof ApiError && e.status === 404
              ? 'Dashboard nicht gefunden'
              : e instanceof ApiError
                ? e.message
                : 'Unbekannter Fehler',
          );
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [dashboardId]);

  const scheduleSave = useCallback(
    (widgets: WidgetDto[]) => {
      if (saveTimer.current) clearTimeout(saveTimer.current);
      setSaveState('pending');
      saveTimer.current = setTimeout(() => {
        (async () => {
          try {
            const updated = await updateDashboard(dashboardId, widgets);
            setDetail(updated);
            setSaveState('saved');
          } catch (e) {
            setSaveState({
              kind: 'error',
              message: e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen',
            });
          }
        })();
      }, AUTO_SAVE_DEBOUNCE_MS);
    },
    [dashboardId],
  );

  // Cleanup beim Unmount, damit ein offener Save-Timer nicht nach dem Wechseln feuert.
  useEffect(() => {
    return () => {
      if (saveTimer.current) clearTimeout(saveTimer.current);
    };
  }, []);

  function handleLayoutChange(newLayout: Layout[]): void {
    if (!detail) return;
    const updated: WidgetDto[] = detail.widgets.map((w, i) => {
      const item = newLayout.find((l) => l.i === widgetKey(w, i));
      if (!item) return w;
      return { ...w, posX: item.x, posY: item.y, width: item.w, height: item.h };
    });
    setDetail({ ...detail, widgets: updated });
    scheduleSave(updated);
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/dashboards')}>
          Zurück zur Liste
        </Button>
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      </Box>
    );
  }

  if (!isDesktop) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="info" role="alert">
          Dashboards sind für Desktop-Ansicht (Breite ab {DESKTOP_MIN_WIDTH} px) optimiert.
          Bitte am größeren Bildschirm öffnen.
        </Alert>
      </Box>
    );
  }

  if (!detail) {
    return (
      <Box
        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 200 }}
        aria-busy="true"
      >
        <CircularProgress aria-label="Dashboard wird geladen" />
      </Box>
    );
  }

  return (
    <Box>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
        <Stack direction="row" alignItems="center" spacing={1}>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/dashboards')}
            size="small"
            color="inherit"
          >
            Liste
          </Button>
          <Typography variant="h4">{detail.name}</Typography>
        </Stack>
        <SaveStatus state={saveState} />
      </Stack>

      {detail.widgets.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
          <Typography>Dieses Dashboard ist noch leer.</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>
            Widget-Typen kommen in den nächsten Phasen (#41 Textbox, #42 KPI).
          </Typography>
        </Paper>
      )}

      {detail.widgets.length > 0 && (
        <ResponsiveGridLayout
          className="layout"
          layouts={{ lg: toLayouts(detail.widgets) }}
          breakpoints={{ lg: DESKTOP_MIN_WIDTH }}
          cols={{ lg: GRID_COLS }}
          rowHeight={GRID_ROW_HEIGHT}
          compactType="vertical"
          preventCollision={false}
          onLayoutChange={handleLayoutChange}
        >
          {detail.widgets.map((w, i) => (
            <Paper key={widgetKey(w, i)} variant="outlined" sx={{ p: 2, overflow: 'hidden' }}>
              <Typography variant="caption" color="text.secondary">
                {w.type}
              </Typography>
              <Typography variant="body2" sx={{ mt: 0.5, wordBreak: 'break-word' }}>
                {w.config}
              </Typography>
            </Paper>
          ))}
        </ResponsiveGridLayout>
      )}
    </Box>
  );
}

function SaveStatus({ state }: { state: SaveState }): JSX.Element | null {
  if (state === 'idle') return null;
  if (state === 'pending') {
    return (
      <Typography variant="caption" color="text.secondary" aria-live="polite">
        speichert…
      </Typography>
    );
  }
  if (state === 'saved') {
    return (
      <Typography variant="caption" color="success.main" aria-live="polite">
        gespeichert
      </Typography>
    );
  }
  return (
    <Typography variant="caption" color="error.main" role="alert">
      Fehler: {state.message}
    </Typography>
  );
}
