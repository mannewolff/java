import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Paper,
  Skeleton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DesktopMacIcon from '@mui/icons-material/DesktopMac';
import EditIcon from '@mui/icons-material/Edit';
import FullscreenIcon from '@mui/icons-material/Fullscreen';
import FullscreenExitIcon from '@mui/icons-material/FullscreenExit';
import PaletteIcon from '@mui/icons-material/Palette';
import SaveIcon from '@mui/icons-material/Save';
import { Responsive, WidthProvider } from 'react-grid-layout';
import type { Layout } from 'react-grid-layout';

import {
  getDashboard,
  renameDashboard,
  setDashboardBackgroundColor,
  updateDashboard,
  type DashboardDetail,
  type WidgetDto,
} from '../../api/dashboard';
import { ApiError } from '../../api/client';
import { useNotify } from '../../notify/NotifyProvider';
import {
  DESKTOP_MIN_WIDTH,
  GRID_COLS,
  GRID_ROW_HEIGHT,
  newWidget,
  pxToRows,
} from './widgetDefaults';
import useViewportWidth from './useViewportWidth';
import { useEditMode } from './EditModeContext';
import { useKioskMode } from './KioskModeContext';
import WidgetDivider from './widgets/WidgetDivider';
import WidgetKanbanList from './widgets/WidgetKanbanList';
import WidgetKpi from './widgets/WidgetKpi';
import WidgetPlot from './widgets/WidgetPlot';
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

function toLayouts(
  widgets: WidgetDto[],
  overrides: ReadonlyMap<string, number> = new Map(),
): Layout[] {
  return widgets.map((w, i) => {
    const key = widgetKey(w, i);
    const overrideH = overrides.get(key);
    return {
      i: key,
      x: w.posX,
      y: w.posY,
      w: w.width,
      // Override expandiert das Widget visuell, nie kleiner als der persistierte Wert.
      h: overrideH != null && overrideH > w.height ? overrideH : w.height,
    };
  });
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
  const { kioskMode, setKioskMode } = useKioskMode();
  const notify = useNotify();

  const [detail, setDetail] = useState<DashboardDetail | null>(null);
  const [draft, setDraft] = useState<DashboardDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saveState, setSaveState] = useState<SaveState>('idle');
  const [renaming, setRenaming] = useState(false);
  const [renameDraft, setRenameDraft] = useState('');
  const [renameError, setRenameError] = useState<string | null>(null);
  const [colorDialogOpen, setColorDialogOpen] = useState(false);
  const [colorDraft, setColorDraft] = useState('');
  const [colorError, setColorError] = useState<string | null>(null);
  const [pendingDeleteIndex, setPendingDeleteIndex] = useState<number | null>(null);
  // Read-Modus-only: visueller Override für die Widget-Höhe, wenn das gerenderte
  // Markdown bei schmaler Spaltenbreite mehr Zeilen wrappt als das Grid-Slot hoch ist.
  // Wächst monoton (nie schrumpfen → kein Yoyo-Effekt bei Row-Grenzen), wird beim
  // Dashboard-Wechsel resetet, NICHT persistiert.
  const [readHeightOverrides, setReadHeightOverrides] = useState<
    ReadonlyMap<string, number>
  >(new Map());

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

  // ESC beendet den Kiosk-Modus.
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent): void => {
      if (e.key === 'Escape' && kioskMode) {
        setKioskMode(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [kioskMode, setKioskMode]);

  // Kiosk-Modus beim Verlassen des Dashboards zurücksetzen.
  useEffect(() => {
    return () => {
      setKioskMode(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dashboardId]);

  // Beim Wechsel der Dashboard-ID den Edit-Modus zurücksetzen.
  useEffect(() => {
    setEditMode(false);
    setReadHeightOverrides(new Map());
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

  /**
   * Hinweis: alle Mutator-Funktionen unten benutzen den functional-setState-Form
   * (`setDraft(prev => ...)`). Grund: react-grid-layout feuert `onLayoutChange`
   * gelegentlich direkt nach `onDrop`. Beide schreiben auf `draft`. Mit normaler
   * Setter-Form sieht der zweite Aufruf das `draft` aus dem Closure (Zustand
   * vor dem Drop) und überschreibt das frisch gedropte Widget wieder weg —
   * Folge: das Widget "flippt zurück". Functional-setState liest jeweils den
   * aktuellsten State und vermeidet die Race.
   */
  function handleLayoutChange(newLayout: Layout[]): void {
    if (!editMode) return;
    setDraft((prev) => {
      if (!prev) return prev;
      const updated: WidgetDto[] = prev.widgets.map((w, i) => {
        const item = newLayout.find((l) => l.i === widgetKey(w, i));
        if (!item) return w;
        return { ...w, posX: item.x, posY: item.y, width: item.w, height: item.h };
      });
      return { ...prev, widgets: updated };
    });
  }

  function handleWidgetChange(index: number, next: WidgetDto): void {
    setDraft((prev) => {
      if (!prev) return prev;
      const updated = prev.widgets.map((w, i) => (i === index ? next : w));
      return { ...prev, widgets: updated };
    });
  }

  /**
   * Löschen über Confirm-Dialog. Erst Index merken, Dialog rendert; bei Bestätigung
   * wird die eigentliche Löschung gegen den Draft durchgeführt. Persistiert wird
   * weiterhin erst beim "Speichern".
   */
  function handleWidgetDeleteRequest(index: number): void {
    setPendingDeleteIndex(index);
  }

  function confirmWidgetDelete(): void {
    const idx = pendingDeleteIndex;
    if (idx == null) return;
    setDraft((prev) => {
      if (!prev) return prev;
      const updated = prev.widgets.filter((_, i) => i !== idx);
      return { ...prev, widgets: updated };
    });
    setPendingDeleteIndex(null);
  }

  function cancelWidgetDelete(): void {
    setPendingDeleteIndex(null);
  }

  /** Inline-Rename des Dashboard-Namens. */
  function startRename(): void {
    if (!detail) return;
    setRenameDraft(detail.name);
    setRenameError(null);
    setRenaming(true);
  }

  function cancelRename(): void {
    setRenaming(false);
    setRenameError(null);
  }

  async function commitRename(): Promise<void> {
    if (!detail) return;
    const trimmed = renameDraft.trim();
    if (trimmed.length === 0) {
      setRenameError('Name darf nicht leer sein.');
      return;
    }
    if (trimmed.length > 100) {
      setRenameError('Name maximal 100 Zeichen.');
      return;
    }
    if (trimmed === detail.name) {
      // Keine Änderung — einfach schließen ohne Round-Trip.
      setRenaming(false);
      return;
    }
    try {
      const updated = await renameDashboard(detail.id, trimmed);
      setDetail({ ...detail, name: updated.name });
      // Auch im Draft den Namen mit-aktualisieren, damit "Abbrechen" nicht zurück-rollt.
      setDraft((prev) => (prev ? { ...prev, name: updated.name } : prev));
      setRenaming(false);
      notify.success('Dashboard umbenannt.');
    } catch (e) {
      setRenameError(
        e instanceof ApiError
          ? e.message
          : 'Umbenennen fehlgeschlagen, bitte später erneut versuchen.',
      );
    }
  }

  /** Hintergrundfarbe des Dashboards bearbeiten (eigener Endpoint, kein Layout-Save). */
  function startBackgroundEdit(): void {
    if (!detail) return;
    setColorDraft(detail.backgroundColor ?? '');
    setColorError(null);
    setColorDialogOpen(true);
  }

  function cancelBackgroundEdit(): void {
    setColorDialogOpen(false);
    setColorError(null);
  }

  async function commitBackgroundColor(): Promise<void> {
    if (!detail) return;
    // Leer = Theme-Default. Backend normalisiert blank ebenfalls auf null.
    const trimmed = colorDraft.trim();
    const value = trimmed.length === 0 ? null : trimmed;
    try {
      await setDashboardBackgroundColor(detail.id, value);
      setDetail({ ...detail, backgroundColor: value });
      setDraft((prev) => (prev ? { ...prev, backgroundColor: value } : prev));
      setColorDialogOpen(false);
      notify.success('Hintergrundfarbe gespeichert.');
    } catch (e) {
      setColorError(
        e instanceof ApiError
          ? e.message
          : 'Speichern fehlgeschlagen, bitte später erneut versuchen.',
      );
    }
  }

  /**
   * Drop aus der Widget-Palette aufs Canvas. react-grid-layout liefert das `item`
   * mit den berechneten Grid-Koordinaten (x/y) basierend auf der Maus-Position.
   * Der Widget-Typ kommt aus dem Context (von der Palette beim onDragStart gesetzt).
   *
   * Achtung: `setDraggingType(null)` muss in den nächsten Tick verzögert werden,
   * sonst wechselt `droppingItem` (siehe Render unten) im selben Frame auf
   * undefined, was react-grid-layout's Drop-Animation abbricht und das Widget
   * visuell wieder verschwinden lässt ("flip back"-Bug).
   */
  function handleDrop(_layout: Layout[], item: Layout): void {
    if (!draggingType) return;
    const type = draggingType;
    setDraft((prev) => {
      if (!prev) return prev;
      const fresh = newWidget(type);
      fresh.posX = item.x;
      fresh.posY = item.y;
      return { ...prev, widgets: [...prev.widgets, fresh] };
    });
    setTimeout(() => setDraggingType(null), 0);
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
      notify.success('Dashboard gespeichert.');
      // Status nach kurzer Zeit zurück auf idle.
      setTimeout(() => setSaveState('idle'), 1500);
    } catch (e) {
      const message = e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen';
      setSaveState({ kind: 'error', message });
      notify.error(message);
    }
  }

  function handleCancel(): void {
    const dirty =
      detail != null && draft != null && !widgetsEqual(detail.widgets, draft.widgets);
    if (dirty) {
      const ok = window.confirm('Ungespeicherte Änderungen verwerfen?');
      if (!ok) return;
    }
    if (detail) setDraft(detail);
    setEditMode(false);
  }

  function handleContentHeight(key: string, pxHeight: number): void {
    const neededRows = pxToRows(pxHeight);
    setReadHeightOverrides((prev) => {
      const current = prev.get(key) ?? 0;
      if (neededRows <= current) return prev;
      const next = new Map(prev);
      next.set(key, neededRows);
      return next;
    });
  }

  function renderWidgetBody(widget: WidgetDto, index: number): JSX.Element {
    switch (widget.type) {
      case 'TEXTBOX':
        return (
          <WidgetTextbox
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDeleteRequest(index)}
            readOnly={!editMode}
            onContentHeight={
              editMode
                ? undefined
                : (px) => handleContentHeight(widgetKey(widget, index), px)
            }
          />
        );
      case 'KPI':
        return (
          <WidgetKpi
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDeleteRequest(index)}
            readOnly={!editMode}
          />
        );
      case 'PLOT':
        return (
          <WidgetPlot
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDeleteRequest(index)}
            readOnly={!editMode}
          />
        );
      case 'KANBAN_LIST':
        return (
          <WidgetKanbanList
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDeleteRequest(index)}
            readOnly={!editMode}
          />
        );
      case 'DIVIDER':
        return (
          <WidgetDivider
            widget={widget}
            onChange={(next) => handleWidgetChange(index, next)}
            onDelete={() => handleWidgetDeleteRequest(index)}
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
        <Alert severity="info" role="alert" icon={<DesktopMacIcon />}>
          Dashboards sind für Desktop-Ansicht (Breite ab {DESKTOP_MIN_WIDTH} px) optimiert.
          Bitte am größeren Bildschirm öffnen.
        </Alert>
      </Box>
    );
  }

  if (!detail || !draft) {
    // Skeleton-Loader statt CircularProgress: zeigt grob die Form des Dashboards
    // (Header-Streifen + Grid-Tiles) waehrend der Initial-Load laeuft.
    return (
      <Box aria-busy="true" aria-label="Dashboard wird geladen">
        <Stack direction="row" justifyContent="space-between" sx={{ mb: 2 }}>
          <Skeleton variant="text" width={280} height={48} />
          <Skeleton variant="rectangular" width={140} height={36} />
        </Stack>
        <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
          <Skeleton variant="rectangular" width="33%" height={180} />
          <Skeleton variant="rectangular" width="33%" height={180} />
          <Skeleton variant="rectangular" width="33%" height={180} />
        </Stack>
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
      }
    : {};

  // Mindest-Höhe für das Grid selbst im Edit-Modus — sonst kollabiert das Element
  // auf 0 px wenn 0 Widgets da sind oder die existierenden Widgets nur die ersten
  // paar Reihen belegen. Drops außerhalb existierender Widgets würden dann keinen
  // Placeholder triggern, weil die Lib keinen gültigen Drop-Bereich findet. 480 px
  // (~ 12 Grid-Reihen bei 40 px row-height) ist groß genug für eine sichtbare
  // leere Drop-Fläche, aber so klein dass es bei vollen Dashboards nicht stört.
  const gridMinHeight = editMode ? 480 : undefined;

  // Dashboard-Hintergrundfarbe; leer/null fällt auf den Theme-Default zurück.
  // minHeight füllt den Inhaltsbereich, damit die Farbe im Kiosk-Modus voll trägt.
  const appliedBackground = detail.backgroundColor ?? 'background.default';

  return (
    <Box sx={{ bgcolor: appliedBackground, minHeight: '100%' }}>
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ mb: 2, display: kioskMode ? 'none' : 'flex' }}
      >
        <Stack direction="row" alignItems="center" spacing={1}>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/dashboards')}
            size="small"
            color="inherit"
          >
            Liste
          </Button>
          {!renaming && (
            <Stack direction="row" alignItems="center" spacing={0.5}>
              <Typography variant="h4">{detail.name}</Typography>
              <IconButton
                size="small"
                aria-label="Dashboard umbenennen"
                onClick={startRename}
              >
                <EditIcon fontSize="small" />
              </IconButton>
              <IconButton
                size="small"
                aria-label="Hintergrundfarbe ändern"
                onClick={startBackgroundEdit}
              >
                <PaletteIcon fontSize="small" />
              </IconButton>
            </Stack>
          )}
          {renaming && (
            <Stack direction="row" alignItems="center" spacing={0.5}>
              <TextField
                value={renameDraft}
                onChange={(e) => {
                  setRenameDraft(e.target.value);
                  if (renameError) setRenameError(null);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void commitRename();
                  if (e.key === 'Escape') cancelRename();
                }}
                size="small"
                autoFocus
                error={renameError != null}
                helperText={renameError}
                inputProps={{ 'aria-label': 'Neuer Dashboard-Name', maxLength: 100 }}
              />
              <IconButton
                size="small"
                aria-label="Umbenennen speichern"
                onClick={() => void commitRename()}
                color="primary"
              >
                <CheckIcon fontSize="small" />
              </IconButton>
              <IconButton
                size="small"
                aria-label="Umbenennen abbrechen"
                onClick={cancelRename}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Stack>
          )}
        </Stack>
        <Stack direction="row" alignItems="center" spacing={2}>
          {!editMode && (
            <>
              <Button
                size="small"
                variant={kioskMode ? 'contained' : 'outlined'}
                startIcon={kioskMode ? <FullscreenExitIcon /> : <FullscreenIcon />}
                onClick={() => setKioskMode(!kioskMode)}
                aria-pressed={kioskMode}
              >
                Kiosk
              </Button>
              <Button
                size="small"
                variant="outlined"
                startIcon={<EditIcon />}
                onClick={() => setEditMode(true)}
              >
                Bearbeiten
              </Button>
            </>
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
              : 'Klicke oben rechts auf „Bearbeiten”, um Widgets hinzuzufügen.'}
          </Typography>
        </Paper>
      )}

      {/* Grid ist immer eingehängt im Edit-Modus, auch wenn keine Widgets da sind —
          sonst kann react-grid-layout das Drop-Target nicht aufspannen. */}
      {(visibleWidgets.length > 0 || editMode) && (
        <Box sx={canvasBackground}>
          <ResponsiveGridLayout
            className="layout"
            style={gridMinHeight != null ? { minHeight: gridMinHeight } : undefined}
            layouts={{ lg: toLayouts(visibleWidgets, editMode ? undefined : readHeightOverrides) }}
            breakpoints={{ lg: DESKTOP_MIN_WIDTH }}
            cols={{ lg: GRID_COLS }}
            rowHeight={GRID_ROW_HEIGHT}
            compactType="vertical"
            preventCollision={false}
            onLayoutChange={handleLayoutChange}
            isDraggable={editMode}
            isResizable={editMode}
            isDroppable={editMode}
            // `droppingItem` muss während des gesamten Drag/Drop-Vorgangs stabil
            // gesetzt sein. Wenn es undefined wird (z. B. weil draggingType
            // synchron auf null wechselt), bricht die Drop-Animation ab und das
            // Widget verschwindet wieder. Default-Maße (2x2) als Fallback.
            droppingItem={{
              i: '__dropping__',
              w: draggingType ? newWidget(draggingType).width : 2,
              h: draggingType ? newWidget(draggingType).height : 2,
            }}
            onDrop={handleDrop}
            draggableCancel=".MuiIconButton-root,.MuiDrawer-root,.MuiButtonBase-root[role='button']"
          >
            {visibleWidgets.map((w, i) => {
              const key = widgetKey(w, i);
              const overrideH = editMode ? undefined : readHeightOverrides.get(key);
              const effectiveH =
                overrideH != null && overrideH > w.height ? overrideH : w.height;
              return (
                // `data-grid` macht die Position auch an den Children explizit —
                // hilft react-grid-layout beim Synchronisieren wenn ein neues Item
                // direkt nach einem Drop in den State kommt und das Layout-Prop
                // noch nicht durch ist.
                <Box
                  key={key}
                  data-grid={{
                    i: key,
                    x: w.posX,
                    y: w.posY,
                    w: w.width,
                    h: effectiveH,
                  }}
                >
                  {renderWidgetBody(w, i)}
                </Box>
              );
            })}
          </ResponsiveGridLayout>
        </Box>
      )}

      <Dialog
        open={pendingDeleteIndex !== null}
        onClose={cancelWidgetDelete}
        aria-labelledby="widget-delete-title"
      >
        <DialogTitle id="widget-delete-title">Widget löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Das Widget wird aus dem Layout entfernt. Die Änderung wird erst mit „Speichern”
            persistiert — bis dahin kannst du sie über „Abbrechen” zurückrollen.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={cancelWidgetDelete}>Abbrechen</Button>
          <Button color="error" variant="contained" onClick={confirmWidgetDelete}>
            Löschen
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={colorDialogOpen}
        onClose={cancelBackgroundEdit}
        aria-labelledby="dashboard-background-title"
      >
        <DialogTitle id="dashboard-background-title">Hintergrundfarbe</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            CSS-Farbwert (z. B. <code>#1a1a2e</code>, <code>rebeccapurple</code> oder ein
            Verlauf). Leer lassen für den Theme-Standard.
          </DialogContentText>
          <Stack direction="row" alignItems="center" spacing={2}>
            <TextField
              value={colorDraft}
              onChange={(e) => {
                setColorDraft(e.target.value);
                if (colorError) setColorError(null);
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') void commitBackgroundColor();
              }}
              size="small"
              autoFocus
              fullWidth
              label="Hintergrundfarbe"
              placeholder="#1a1a2e"
              error={colorError != null}
              helperText={colorError}
              inputProps={{ 'aria-label': 'Hintergrundfarbe (CSS)', maxLength: 64 }}
            />
            <Box
              aria-label="Farbvorschau"
              sx={{
                width: 40,
                height: 40,
                flexShrink: 0,
                borderRadius: 1,
                border: '1px solid',
                borderColor: 'divider',
                background: colorDraft.trim() || 'transparent',
              }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={cancelBackgroundEdit}>Abbrechen</Button>
          <Button variant="contained" onClick={() => void commitBackgroundColor()}>
            Speichern
          </Button>
        </DialogActions>
      </Dialog>
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
