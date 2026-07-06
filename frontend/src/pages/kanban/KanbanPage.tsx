import { type MouseEvent, useCallback, useEffect, useRef, useState } from 'react';
import {
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
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SettingsIcon from '@mui/icons-material/Settings';
import ViewKanbanIcon from '@mui/icons-material/ViewKanban';
import ViewColumnIcon from '@mui/icons-material/ViewColumn';
import ViewListIcon from '@mui/icons-material/ViewList';
import {
  DndContext,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';

import {
  KANBAN_COLUMNS,
  archiveKanbanItem,
  createKanbanItem,
  forceDeleteKanbanItem,
  getKanbanEpics,
  getKanbanSettings,
  listKanbanItems,
  moveKanbanItem,
  restoreKanbanItem,
  updateKanbanItem,
  updateKanbanSettings,
  type KanbanBoard,
  type KanbanColumn as KanbanColumnId,
  type KanbanEpic,
  type KanbanItem,
  type KanbanItemType,
} from '../../api/kanban';
import { ApiError } from '../../api/client';
import { useNotify } from '../../notify/NotifyProvider';
import { COLUMN_LABELS } from './columnMeta';
import KanbanColumnView from './KanbanColumn';
import KanbanDetailModal from './KanbanDetailModal';
import KanbanListView from './KanbanListView';
import KanbanNewItemModal from './KanbanNewItemModal';
import KanbanSettingsDrawer from './KanbanSettingsDrawer';
import { emptyBoard, moveItem } from './boardOps';

const DEFAULT_RETENTION_DAYS = 5;
const VIEW_KEY = 'kanban.view';

type KanbanView = 'board' | 'list';

function loadView(): KanbanView {
  try {
    return localStorage.getItem(VIEW_KEY) === 'list' ? 'list' : 'board';
  } catch {
    return 'board';
  }
}

function saveView(value: KanbanView): void {
  try {
    localStorage.setItem(VIEW_KEY, value);
  } catch {
    // localStorage nicht verfügbar
  }
}

type LoadState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; board: KanbanBoard };

export default function KanbanPage(): JSX.Element {
  const notify = useNotify();
  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [createColumn, setCreateColumn] = useState<KanbanColumnId | null>(null);
  const [detailItem, setDetailItem] = useState<KanbanItem | null>(null);
  const [pendingArchive, setPendingArchive] = useState<KanbanItem | null>(null);
  const [pendingForceDelete, setPendingForceDelete] = useState<KanbanItem | null>(null);
  const [retentionDays, setRetentionDays] = useState(DEFAULT_RETENTION_DAYS);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [view, setView] = useState<KanbanView>(loadView);
  // Reload-Trigger für die Listenansicht (#308): die Liste besitzt eigenen Daten-State,
  // den reload() (Board) nicht erreicht. Jede Mutation inkrementiert diesen Key.
  const [listReloadKey, setListReloadKey] = useState(0);
  // Epics nach ID für die Board-Karten-Badges (#325). Best-effort: schlägt das Laden fehl,
  // bleiben die Karten ohne Badge (das Board selbst funktioniert weiter).
  const [epicsById, setEpicsById] = useState<Record<number, KanbanEpic>>({});

  // Laufende Nummer je Drag (#316): nur der zuletzt gestartete Move darf reloaden bzw. bei Fehler
  // zurückrollen. So lässt ein verspäteter reload eines älteren Moves die Items nicht zurückspringen
  // und ein Rollback macht keinen inzwischen erfolgten neueren Move rückgängig.
  const moveSeqRef = useRef(0);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 5 },
    }),
  );

  // Board-Ansicht enthält nie archivierte Items (#283) — Archiv ist nur über den
  // Listen-Filter erreichbar.
  const reload = useCallback(async (): Promise<void> => {
    try {
      const board = await listKanbanItems(false);
      const safeBoard: KanbanBoard = { ...emptyBoard(), ...board };
      setState({ kind: 'ready', board: safeBoard });
    } catch (e) {
      setState({
        kind: 'error',
        message: e instanceof ApiError ? e.message : 'Kanban-Items konnten nicht geladen werden.',
      });
    }
    // Epics best-effort für die Badges — Fehler dürfen das Board nicht stören.
    try {
      const epics = await getKanbanEpics();
      setEpicsById(Object.fromEntries(epics.map((e) => [e.id, e])));
    } catch {
      setEpicsById({});
    }
  }, []);

  // Aktualisiert beide Ansichten nach einer Mutation: Board-State neu laden und den
  // Listen-Reload-Key hochzählen, damit die selbstständige Liste (#308) nachzieht.
  const refresh = useCallback(async (): Promise<void> => {
    setListReloadKey((k) => k + 1);
    await reload();
  }, [reload]);

  // Retention-Setting wird in beiden Ansichten gebraucht (Board-Countdown + Listen-Modal).
  useEffect(() => {
    void getKanbanSettings()
      .then((s) => setRetentionDays(s.doneRetentionDays))
      .catch(() => {
        // Default bleibt
      });
  }, []);

  // Board-Daten nur laden, wenn die Board-Ansicht aktiv ist — die Listenansicht lädt selbst.
  useEffect(() => {
    if (view === 'board') void reload();
  }, [reload, view]);

  function handleViewChange(_event: MouseEvent<HTMLElement>, next: KanbanView | null): void {
    if (next != null) {
      setView(next);
      saveView(next);
    }
  }

  async function handleSettingsSubmit(doneRetentionDays: number): Promise<void> {
    try {
      const saved = await updateKanbanSettings(doneRetentionDays);
      setRetentionDays(saved.doneRetentionDays);
      setSettingsOpen(false);
      notify.success('Einstellungen gespeichert.');
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen.');
    }
  }

  function startCreate(defaultColumn: KanbanColumnId): void {
    setCreateColumn(defaultColumn);
  }

  async function handleSubmitDetail(title: string, body: string): Promise<void> {
    if (!detailItem) return;
    try {
      await updateKanbanItem(detailItem.id, title, body);
      notify.success('Item gespeichert.');
      setDetailItem(null);
      await refresh();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen.');
    }
  }

  async function handleSubmitCreate(
    title: string,
    body: string,
    type: KanbanItemType,
    parentId: number | null,
  ): Promise<void> {
    if (!createColumn) return;
    try {
      await createKanbanItem(title, body, createColumn, type, parentId);
      notify.success(type === 'EPIC' ? 'Epic angelegt.' : 'Item angelegt.');
      setCreateColumn(null);
      await refresh();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen.');
    }
  }

  async function confirmArchive(): Promise<void> {
    if (!pendingArchive) return;
    const target = pendingArchive;
    setPendingArchive(null);
    try {
      await archiveKanbanItem(target.id);
      notify.success('Item archiviert.');
      await refresh();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Archivieren fehlgeschlagen.');
    }
  }

  // Tastaturbedienbarer Statuswechsel über das Karten-Menü (#316): verschiebt das Item ans Ende
  // der Zielspalte. Ergänzt das reine Maus-Drag&Drop für Tastatur- und Screenreader-Nutzung.
  async function handleMoveToColumn(item: KanbanItem, targetColumn: KanbanColumnId): Promise<void> {
    if (item.column === targetColumn) return;
    const targetPosition = state.kind === 'ready' ? state.board[targetColumn].length : 0;
    try {
      await moveKanbanItem(item.id, targetColumn, targetPosition);
      await refresh();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Verschieben fehlgeschlagen.');
    }
  }

  async function handleRestore(item: KanbanItem): Promise<void> {
    try {
      await restoreKanbanItem(item.id);
      notify.success('Item wiederhergestellt.');
      await refresh();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Wiederherstellen fehlgeschlagen.');
    }
  }

  async function confirmForceDelete(): Promise<void> {
    if (!pendingForceDelete) return;
    const target = pendingForceDelete;
    setPendingForceDelete(null);
    try {
      await forceDeleteKanbanItem(target.id);
      notify.success('Item endgültig gelöscht.');
      await refresh();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Löschen fehlgeschlagen.');
    }
  }

  async function handleDragEnd(event: DragEndEvent): Promise<void> {
    if (state.kind !== 'ready') return;
    const { active, over } = event;
    if (!over) return;

    const itemId = Number(active.id);
    const overData = over.data.current as
      | { type: 'column'; column: KanbanColumnId }
      | { type: 'item'; column: KanbanColumnId; position: number }
      | undefined;
    if (!overData) return;

    const targetColumn = overData.column;
    const targetPosition =
      overData.type === 'item' ? overData.position : state.board[targetColumn].length;

    const previousBoard = state.board;
    const optimistic = moveItem(previousBoard, itemId, targetColumn, targetPosition);
    if (optimistic === previousBoard) return;
    setState({ kind: 'ready', board: optimistic });

    const seq = ++moveSeqRef.current;
    try {
      await moveKanbanItem(itemId, targetColumn, targetPosition);
      // Nur der jüngste Move lädt neu — ein älterer, verspäteter reload würde die Anordnung
      // eines inzwischen erfolgten neueren Moves überschreiben (#316).
      if (seq === moveSeqRef.current) {
        await reload();
      }
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Verschieben fehlgeschlagen.');
      // Nur zurückrollen, wenn seither kein neuerer Move gestartet wurde — sonst würde der
      // Rollback dessen (optimistische) Anordnung zerstören (#316).
      if (seq === moveSeqRef.current) {
        setState({ kind: 'ready', board: previousBoard });
      }
    }
  }

  const totalItems =
    state.kind === 'ready'
      ? KANBAN_COLUMNS.reduce((sum, col) => sum + state.board[col].length, 0)
      : 0;

  const boardBody =
    state.kind === 'loading' ? (
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} aria-busy="true">
        <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
        <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
        <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
        <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
      </Stack>
    ) : state.kind === 'error' ? (
      <Paper variant="outlined" sx={{ p: 3, color: 'error.main' }} role="alert">
        {state.message}
      </Paper>
    ) : totalItems === 0 ? (
      <Paper
        variant="outlined"
        sx={{
          p: 6,
          textAlign: 'center',
          color: 'text.secondary',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 2,
        }}
      >
        <ViewKanbanIcon sx={{ fontSize: 56, color: 'action.disabled' }} />
        <Typography variant="h6">Noch keine Kanban-Items</Typography>
        <Typography variant="body2">
          Leg dein erstes Item an — Drag&amp;Drop zwischen den fünf Spalten organisiert deinen
          Workflow.
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => startCreate('BACKLOG')}>
          Erstes Item anlegen
        </Button>
      </Paper>
    ) : (
      <DndContext sensors={sensors} onDragEnd={(e) => void handleDragEnd(e)}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="stretch">
          {KANBAN_COLUMNS.map((col) => (
            <KanbanColumnView
              key={col}
              column={col}
              label={COLUMN_LABELS[col]}
              items={state.board[col]}
              retentionDays={retentionDays}
              epicsById={epicsById}
              onCreate={startCreate}
              onOpenDetail={setDetailItem}
              onEdit={setDetailItem}
              onArchive={setPendingArchive}
              onRestore={(item) => void handleRestore(item)}
              onForceDelete={setPendingForceDelete}
              onMove={(item, targetColumn) => void handleMoveToColumn(item, targetColumn)}
            />
          ))}
        </Stack>
      </DndContext>
    );

  return (
    <Box>
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ mb: 3 }}
      >
        <Typography variant="h4">Kanban</Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          <ToggleButtonGroup
            value={view}
            exclusive
            onChange={handleViewChange}
            size="small"
            aria-label="Ansicht"
          >
            <ToggleButton value="board" aria-label="Board">
              <ViewColumnIcon fontSize="small" sx={{ mr: 0.5 }} />
              Board
            </ToggleButton>
            <ToggleButton value="list" aria-label="Liste">
              <ViewListIcon fontSize="small" sx={{ mr: 0.5 }} />
              Liste
            </ToggleButton>
          </ToggleButtonGroup>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => startCreate('BACKLOG')}
          >
            Neues Item
          </Button>
          <Tooltip title="Einstellungen">
            <IconButton
              aria-label="Kanban-Einstellungen"
              onClick={() => setSettingsOpen(true)}
            >
              <SettingsIcon />
            </IconButton>
          </Tooltip>
        </Stack>
      </Stack>

      {view === 'list' ? (
        <KanbanListView retentionDays={retentionDays} reloadKey={listReloadKey} />
      ) : (
        boardBody
      )}

      <KanbanNewItemModal
        open={createColumn != null}
        onClose={() => setCreateColumn(null)}
        onSubmit={handleSubmitCreate}
      />

      {detailItem != null && (
        <KanbanDetailModal
          open
          item={detailItem}
          retentionDays={retentionDays}
          onClose={() => setDetailItem(null)}
          onSubmit={handleSubmitDetail}
        />
      )}

      <KanbanSettingsDrawer
        open={settingsOpen}
        currentRetentionDays={retentionDays}
        onClose={() => setSettingsOpen(false)}
        onSubmit={handleSettingsSubmit}
      />

      <Dialog
        open={pendingArchive != null}
        onClose={() => setPendingArchive(null)}
        aria-labelledby="kanban-archive-title"
      >
        <DialogTitle id="kanban-archive-title">Item archivieren?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            „{pendingArchive?.title}” wird archiviert und kann später wiederhergestellt werden.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendingArchive(null)}>Abbrechen</Button>
          <Button variant="contained" onClick={() => void confirmArchive()}>
            Archivieren
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={pendingForceDelete != null}
        onClose={() => setPendingForceDelete(null)}
        aria-labelledby="kanban-force-delete-title"
      >
        <DialogTitle id="kanban-force-delete-title">Item endgültig löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            „{pendingForceDelete?.title}” wird unwiderruflich entfernt.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendingForceDelete(null)}>Abbrechen</Button>
          <Button color="error" variant="contained" onClick={() => void confirmForceDelete()}>
            Endgültig löschen
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
