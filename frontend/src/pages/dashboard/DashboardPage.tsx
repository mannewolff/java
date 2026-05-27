import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { Responsive, WidthProvider } from 'react-grid-layout';
import type { Layout } from 'react-grid-layout';

// `Responsive` allein kennt die Container-Breite nicht und berechnet Spalten aus einem Default —
// Folge: horizontales Resize/Drag wird unzuverlässig. `WidthProvider` injiziert die Breite via
// ResizeObserver und reagiert auch live auf Browser-Window-Resize.
const ResponsiveGridLayout = WidthProvider(Responsive);

import {
  getDashboard,
  updateDashboard,
  type DashboardDetail,
  type WidgetDto,
  type WidgetType,
} from '../../api/dashboard';
import { ApiError } from '../../api/client';
import {
  AUTO_SAVE_DEBOUNCE_MS,
  DESKTOP_MIN_WIDTH,
  GRID_COLS,
  GRID_ROW_HEIGHT,
  newWidget,
} from './widgetDefaults';
import useViewportWidth from './useViewportWidth';
import WidgetKpi from './widgets/WidgetKpi';
import WidgetTextbox from './widgets/WidgetTextbox';

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

  function handleWidgetChange(index: number, next: WidgetDto): void {
    if (!detail) return;
    const updated = detail.widgets.map((w, i) => (i === index ? next : w));
    setDetail({ ...detail, widgets: updated });
    scheduleSave(updated);
  }

  function handleWidgetDelete(index: number): void {
    if (!detail) return;
    const updated = detail.widgets.filter((_, i) => i !== index);
    setDetail({ ...detail, widgets: updated });
    scheduleSave(updated);
  }

  function handleAddWidget(type: WidgetType): void {
    if (!detail) return;
    const updated = [...detail.widgets, newWidget(type)];
    setDetail({ ...detail, widgets: updated });
    scheduleSave(updated);
  }

  function renderWidgetBody(widget: WidgetDto, index: number): JSX.Element {
    switch (widget.type) {
      case 'TEXTBOX':
        return (
          <WidgetTextbox
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDelete(index)}
          />
        );
      case 'KPI':
        return (
          <WidgetKpi
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDelete(index)}
          />
        );
      // Unbekannter Typ — minimaler Fallback ohne Crash. Backend-Enum und Frontend-Switch
      // sind theoretisch immer in Sync, das Default-Branch ist defensiv für Schema-Drift.
      default:
        return (
          <Paper variant="outlined" sx={{ p: 2, height: '100%', overflow: 'hidden' }}>
            <Typography variant="caption" color="text.secondary">
              {widget.type}
            </Typography>
          </Paper>
        );
    }
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
        <Stack direction="row" alignItems="center" spacing={2}>
          <Stack direction="row" spacing={1}>
            <Button
              size="small"
              variant="outlined"
              onClick={() => handleAddWidget('TEXTBOX')}
            >
              Textbox hinzufügen
            </Button>
            <Button
              size="small"
              variant="outlined"
              onClick={() => handleAddWidget('KPI')}
            >
              KPI hinzufügen
            </Button>
          </Stack>
          <SaveStatus state={saveState} />
        </Stack>
      </Stack>

      {detail.widgets.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
          <Typography>Dieses Dashboard ist noch leer.</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>
            Klicke oben rechts auf „Textbox hinzufügen" oder „KPI hinzufügen", um zu starten.
          </Typography>
        </Paper>
      )}

      {detail.widgets.length > 0 && (
        <Box
          sx={{
            // Dezentes Punkte-Raster im Hintergrund — radial-gradient mit divider-Farbe
            // und gleichmäßiger Tile-Größe (Spaltenbreite × Row-Höhe). Macht die 12-Spalten-
            // Struktur sichtbar, ohne aufdringlich zu sein. In #80 wird das auf den
            // Edit-Modus eingeschränkt.
            backgroundImage:
              'radial-gradient(circle, rgba(0, 0, 0, 0.18) 1px, transparent 1.5px)',
            backgroundSize: `calc(100% / ${GRID_COLS}) ${GRID_ROW_HEIGHT}px`,
            backgroundPosition: '0 0',
          }}
        >
          <ResponsiveGridLayout
            className="layout"
            layouts={{ lg: toLayouts(detail.widgets) }}
            breakpoints={{ lg: DESKTOP_MIN_WIDTH }}
            cols={{ lg: GRID_COLS }}
            rowHeight={GRID_ROW_HEIGHT}
            compactType="vertical"
            preventCollision={false}
            onLayoutChange={handleLayoutChange}
            draggableCancel=".MuiIconButton-root,.MuiDrawer-root,.MuiButtonBase-root[role='button']"
          >
            {detail.widgets.map((w, i) => (
              <Box key={widgetKey(w, i)}>{renderWidgetBody(w, i)}</Box>
            ))}
          </ResponsiveGridLayout>
        </Box>
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
