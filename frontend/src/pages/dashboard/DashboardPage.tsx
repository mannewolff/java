import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import CloseIcon from '@mui/icons-material/Close';
import { Responsive, WidthProvider } from 'react-grid-layout';
import type { Layout } from 'react-grid-layout';

import {
  getDashboard,
  updateDashboard,
  type DashboardDetail,
  type WidgetDto,
} from '../../api/dashboard';
import { ApiError } from '../../api/client';
import { DESKTOP_MIN_WIDTH, GRID_COLS, GRID_ROW_HEIGHT, newWidget } from './widgetDefaults';
import useViewportWidth from './useViewportWidth';
import { useEditMode } from './EditModeContext';
import WidgetKpi from './widgets/WidgetKpi';
import WidgetTextbox from './widgets/WidgetTextbox';

// `Responsive` allein kennt die Container-Breite nicht und berechnet Spalten aus einem Default —
// Folge: horizontales Resize/Drag wird unzuverlässig. `WidthProvider` injiziert die Breite via
// ResizeObserver und reagiert auch live auf Browser-Window-Resize.
const ResponsiveGridLayout = WidthProvider(Responsive);

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

/** Vergleicht zwei Widget-Arrays inhaltlich — Position, Größe, Config, Typ. */
function widgetsEqual(a: WidgetDto[], b: WidgetDto[]): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i += 1) {
    const x = a[i];
    const y = b[i];
    if (
      x.id !== y.id ||
      x.type !== y.type ||
      x.posX !== y.posX ||
      x.posY !== y.posY ||
      x.width !== y.width ||
      x.height !== y.height ||
      x.config !== y.config
    ) {
      return false;
    }
  }
  return true;
}

export default function DashboardPage(): JSX.Element {
  const { id } = useParams<{ id: string }>();
  const dashboardId = Number(id);
  const navigate = useNavigate();
  const viewportWidth = useViewportWidth();
  const isDesktop = viewportWidth >= DESKTOP_MIN_WIDTH;
  const { editMode, setEditMode, draggingType, setDraggingType } = useEditMode();

  const [detail, setDetail] = useState<DashboardDetail | null>(null);
  const [draft, setDraft] = useState<DashboardDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saveState, setSaveState] = useState<SaveState>('idle');

  // Initial-Load.
  useEffect(() => {
    if (!Number.isFinite(dashboardId)) {
      setError('Ungültige Dashboard-ID');
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const data = await getDashboard(dashboardId);
        if (!cancelled) {
          setDetail(data);
          setDraft(data);
        }
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

  // Auto-Edit-Modus bei leerem Dashboard: erspart den ersten Klick auf "Bearbeiten".
  useEffect(() => {
    if (detail && detail.widgets.length === 0 && !editMode) {
      setEditMode(true);
    }
    // setEditMode bewusst nicht in deps — sonst Endlos-Trigger über Provider-Re-Renders.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [detail]);

  // Beim Wechsel der Dashboard-ID den Edit-Modus zurücksetzen.
  useEffect(() => {
    setEditMode(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dashboardId]);

  // useBeforeUnload-Light: Tab schließen / Browser-Reload mit ungespeicherten Änderungen.
  useEffect(() => {
    const dirty = detail != null && draft != null && !widgetsEqual(detail.widgets, draft.widgets);
    if (!dirty) return;
    const handler = (e: BeforeUnloadEvent): void => {
      e.preventDefault();
      // Moderne Browser ignorieren returnValue, zeigen aber dennoch den Dialog.
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [detail, draft]);

  function handleLayoutChange(newLayout: Layout[]): void {
    if (!draft || !editMode) return;
    const updated: WidgetDto[] = draft.widgets.map((w, i) => {
      const item = newLayout.find((l) => l.i === widgetKey(w, i));
      if (!item) return w;
      return { ...w, posX: item.x, posY: item.y, width: item.w, height: item.h };
    });
    setDraft({ ...draft, widgets: updated });
  }

  function handleWidgetChange(index: number, next: WidgetDto): void {
    if (!draft) return;
    const updated = draft.widgets.map((w, i) => (i === index ? next : w));
    setDraft({ ...draft, widgets: updated });
  }

  function handleWidgetDelete(index: number): void {
    if (!draft) return;
    const updated = draft.widgets.filter((_, i) => i !== index);
    setDraft({ ...draft, widgets: updated });
  }

  /**
   * Drop aus der Widget-Palette aufs Canvas. react-grid-layout liefert das `item` mit
   * berechneten Grid-Koordinaten (x/y) basierend auf der Maus-Position. Der Widget-Typ
   * kommt aus dem Context (von der Palette beim onDragStart gesetzt).
   */
  function handleDrop(_layout: Layout[], item: Layout): void {
    if (!draft || !draggingType) return;
    const fresh = newWidget(draggingType);
    fresh.posX = item.x;
    fresh.posY = item.y;
    setDraft({ ...draft, widgets: [...draft.widgets, fresh] });
    setDraggingType(null);
  }

  async function handleSave(): Promise<void> {
    if (!draft) return;
    setSaveState('pending');
    try {
      const updated = await updateDashboard(dashboardId, draft.widgets);
      setDetail(updated);
      setDraft(updated);
      setSaveState('saved');
      setEditMode(false);
      // Status nach kurzer Zeit zurück auf idle.
      setTimeout(() => setSaveState('idle'), 1500);
    } catch (e) {
      setSaveState({
        kind: 'error',
        message: e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen',
      });
    }
  }

  function handleCancel(): void {
    const dirty =
      detail != null && draft != null && !widgetsEqual(detail.widgets, draft.widgets);
    if (dirty) {
      // eslint-disable-next-line no-alert
      const ok = window.confirm('Ungespeicherte Änderungen verwerfen?');
      if (!ok) return;
    }
    if (detail) setDraft(detail);
    setEditMode(false);
  }

  function renderWidgetBody(widget: WidgetDto, index: number): JSX.Element {
    switch (widget.type) {
      case 'TEXTBOX':
        return (
          <WidgetTextbox
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDelete(index)}
            readOnly={!editMode}
          />
        );
      case 'KPI':
        return (
          <WidgetKpi
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDelete(index)}
            readOnly={!editMode}
          />
        );
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

  if (!detail || !draft) {
    return (
      <Box
        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 200 }}
        aria-busy="true"
      >
        <CircularProgress aria-label="Dashboard wird geladen" />
      </Box>
    );
  }

  // Im Edit-Modus arbeiten wir auf `draft`, im Read-Modus auf dem persistierten `detail`.
  const visibleWidgets = editMode ? draft.widgets : detail.widgets;

  // Punkte-Raster nur im Edit-Modus sichtbar.
  const canvasBackground = editMode
    ? {
        backgroundImage:
          'radial-gradient(circle, rgba(0, 0, 0, 0.18) 1px, transparent 1.5px)',
        backgroundSize: `calc(100% / ${GRID_COLS}) ${GRID_ROW_HEIGHT}px`,
        backgroundPosition: '0 0',
        minHeight: 240,
      }
    : {};

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
          {!editMode && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<EditIcon />}
              onClick={() => setEditMode(true)}
            >
              Bearbeiten
            </Button>
          )}
          {editMode && (
            <Stack direction="row" spacing={1}>
              <Button
                size="small"
                variant="outlined"
                startIcon={<CloseIcon />}
                onClick={handleCancel}
              >
                Abbrechen
              </Button>
              <Button
                size="small"
                variant="contained"
                startIcon={<SaveIcon />}
                onClick={handleSave}
                disabled={saveState === 'pending'}
              >
                Speichern
              </Button>
            </Stack>
          )}
          <SaveStatus state={saveState} />
        </Stack>
      </Stack>

      {visibleWidgets.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
          <Typography>Dieses Dashboard ist noch leer.</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>
            {editMode
              ? 'Ziehe ein Widget aus der linken Palette auf das Dashboard.'
              : 'Klicke oben rechts auf „Bearbeiten", um Widgets hinzuzufügen.'}
          </Typography>
        </Paper>
      )}

      {/* Grid ist immer eingehängt im Edit-Modus, auch wenn keine Widgets da sind —
          sonst kann react-grid-layout das Drop-Target nicht aufspannen. */}
      {(visibleWidgets.length > 0 || editMode) && (
        <Box sx={canvasBackground}>
          <ResponsiveGridLayout
            className="layout"
            layouts={{ lg: toLayouts(visibleWidgets) }}
            breakpoints={{ lg: DESKTOP_MIN_WIDTH }}
            cols={{ lg: GRID_COLS }}
            rowHeight={GRID_ROW_HEIGHT}
            compactType="vertical"
            preventCollision={false}
            onLayoutChange={handleLayoutChange}
            isDraggable={editMode}
            isResizable={editMode}
            isDroppable={editMode}
            droppingItem={
              draggingType
                ? {
                    i: '__dropping__',
                    w: newWidget(draggingType).width,
                    h: newWidget(draggingType).height,
                  }
                : undefined
            }
            onDrop={handleDrop}
            draggableCancel=".MuiIconButton-root,.MuiDrawer-root,.MuiButtonBase-root[role='button']"
          >
            {visibleWidgets.map((w, i) => (
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
