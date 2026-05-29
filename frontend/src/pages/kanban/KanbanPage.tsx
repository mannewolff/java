import { useCallback, useEffect, useState } from 'react';
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
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SettingsIcon from '@mui/icons-material/Settings';
import ViewKanbanIcon from '@mui/icons-material/ViewKanban';
import {
  DndContext,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';

import {
  KANBAN_COLUMNS,
  createKanbanItem,
  deleteKanbanItem,
  getKanbanSettings,
  listKanbanItems,
  moveKanbanItem,
  updateKanbanItem,
  updateKanbanSettings,
  type KanbanBoard,
  type KanbanColumn as KanbanColumnId,
  type KanbanItem,
} from '../../api/kanban';
import { ApiError } from '../../api/client';
import { useNotify } from '../../notify/NotifyProvider';
import KanbanColumnView from './KanbanColumn';
import KanbanDetailModal from './KanbanDetailModal';
import KanbanEditDrawer from './KanbanEditDrawer';
import KanbanSettingsDrawer from './KanbanSettingsDrawer';
import { emptyBoard, moveItem } from './boardOps';

/** Default-Retention (auch Backend-Default), bis das initiale GET den echten Wert liefert. */
const DEFAULT_RETENTION_DAYS = 5;

const COLUMN_LABELS: Record<KanbanColumnId, string> = {
  BACKLOG: 'Backlog',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
};

type LoadState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; board: KanbanBoard };

interface EditTarget {
  /** {@code null} = neues Item, sonst Edit eines bestehenden. */
  item: KanbanItem | null;
  defaultColumn: KanbanColumnId;
}

/**
 * Vier-Spalten-Kanban-Board mit Drag&Drop und Edit-Drawer.
 *
 * - DnD per dnd-kit: jede Spalte ist {@link useDroppable}, jede Karte {@link useSortable}.
 * - Move-Logik liegt als pure Funktion in {@code boardOps.moveItem} (so isoliert testbar).
 * - Updates sind optimistisch: lokales Board sofort umsortieren, API-Call im Hintergrund.
 *   Bei Fehler reverten und Toast.
 * - Settings (retentionDays) kommen aus dem Issue Kanban-3 — hier wird ein Default-Stub
 *   verwendet, das Zahnrad ist auf einen Toast verdrahtet.
 */
export default function KanbanPage(): JSX.Element {
  const notify = useNotify();
  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [editTarget, setEditTarget] = useState<EditTarget | null>(null);
  const [detailItem, setDetailItem] = useState<KanbanItem | null>(null);
  const [pendingDelete, setPendingDelete] = useState<KanbanItem | null>(null);
  const [retentionDays, setRetentionDays] = useState(DEFAULT_RETENTION_DAYS);
  const [settingsOpen, setSettingsOpen] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 5 },
    }),
  );

  const reload = useCallback(async (): Promise<void> => {
    try {
      const board = await listKanbanItems();
      // Backend liefert keys; sicherheitshalber mit leeren Spalten mergen.
      const safeBoard: KanbanBoard = { ...emptyBoard(), ...board };
      setState({ kind: 'ready', board: safeBoard });
    } catch (e) {
      setState({
        kind: 'error',
        message: e instanceof ApiError ? e.message : 'Kanban-Items konnten nicht geladen werden.',
      });
    }
  }, []);

  // Initial-Load: Items und Settings parallel. Settings-Failure soll den Board-Load nicht
  // sprengen — wir fallen einfach auf den Default-Retention-Wert zurueck.
  useEffect(() => {
    void reload();
    void getKanbanSettings()
      .then((s) => setRetentionDays(s.doneRetentionDays))
      .catch(() => {
        // Default bleibt; KEIN Toast, weil das nur die Countdown-Anzeige beeinflusst.
      });
  }, [reload]);

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
    setEditTarget({ item: null, defaultColumn });
  }

  function startEdit(item: KanbanItem): void {
    setEditTarget({ item, defaultColumn: item.column });
  }

  async function handleSubmitDetail(title: string, body: string): Promise<void> {
    if (!detailItem) return;
    try {
      await updateKanbanItem(detailItem.id, title, body);
      notify.success('Item gespeichert.');
      setDetailItem(null);
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen.');
    }
  }

  async function handleSubmitEdit(title: string, body: string): Promise<void> {
    if (!editTarget) return;
    try {
      if (editTarget.item == null) {
        await createKanbanItem(title, body, editTarget.defaultColumn);
        notify.success('Item angelegt.');
      } else {
        await updateKanbanItem(editTarget.item.id, title, body);
        notify.success('Item gespeichert.');
      }
      setEditTarget(null);
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen.');
    }
  }

  async function confirmDelete(): Promise<void> {
    if (!pendingDelete) return;
    const target = pendingDelete;
    setPendingDelete(null);
    try {
      await deleteKanbanItem(target.id);
      notify.success('Item gelöscht.');
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Löschen fehlgeschlagen.');
    }
  }

  async function handleDragEnd(event: DragEndEvent): Promise<void> {
    if (state.kind !== 'ready') return;
    const { active, over } = event;
    if (!over) return;

    const itemId = Number(active.id);
    // Zielspalte ableiten: entweder ueber die Drop-Zone der Spalte (data.column gesetzt) oder
    // ueber das Karten-Item, ueber dem wir gedroppt haben (data.column ebenfalls gesetzt).
    const overData = over.data.current as
      | { type: 'column'; column: KanbanColumnId }
      | { type: 'item'; column: KanbanColumnId; position: number }
      | undefined;
    if (!overData) return;

    // Zielposition: bei Drop ueber Karte → deren Position, bei Drop in leere Spalte → ans Ende.
    const targetColumn = overData.column;
    const targetPosition =
      overData.type === 'item' ? overData.position : state.board[targetColumn].length;

    const previousBoard = state.board;
    const optimistic = moveItem(previousBoard, itemId, targetColumn, targetPosition);
    if (optimistic === previousBoard) return;
    setState({ kind: 'ready', board: optimistic });

    try {
      await moveKanbanItem(itemId, targetColumn, targetPosition);
      // Backend ist Source of Truth — kurz refetchen, damit movedToDoneAt & Co stimmen.
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Verschieben fehlgeschlagen.');
      setState({ kind: 'ready', board: previousBoard });
    }
  }

  if (state.kind === 'loading') {
    return (
      <Box>
        <Stack direction="row" justifyContent="space-between" sx={{ mb: 3 }}>
          <Skeleton variant="text" width={180} height={42} />
          <Skeleton variant="rectangular" width={140} height={36} />
        </Stack>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} aria-busy="true">
          <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
          <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
          <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
          <Skeleton variant="rectangular" height={220} sx={{ flex: 1 }} />
        </Stack>
      </Box>
    );
  }

  if (state.kind === 'error') {
    return (
      <Box sx={{ p: 3 }}>
        <Paper variant="outlined" sx={{ p: 3, color: 'error.main' }} role="alert">
          {state.message}
        </Paper>
      </Box>
    );
  }

  const totalItems = KANBAN_COLUMNS.reduce(
    (sum, col) => sum + state.board[col].length,
    0,
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
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => startCreate('BACKLOG')}
          >
            Neues Item
          </Button>
          <Tooltip title="Einstellungen (Cleanup-Retention)">
            <IconButton
              aria-label="Kanban-Einstellungen"
              onClick={() => setSettingsOpen(true)}
            >
              <SettingsIcon />
            </IconButton>
          </Tooltip>
        </Stack>
      </Stack>

      {totalItems === 0 ? (
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
            Leg dein erstes Item an — Drag&amp;Drop zwischen den vier Spalten organisiert deinen
            Workflow.
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => startCreate('BACKLOG')}
          >
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
                onCreate={startCreate}
                onOpenDetail={setDetailItem}
                onEdit={startEdit}
                onDelete={setPendingDelete}
              />
            ))}
          </Stack>
        </DndContext>
      )}

      <KanbanEditDrawer
        open={editTarget != null}
        heading={editTarget?.item == null ? 'Neues Item' : 'Item bearbeiten'}
        initialTitle={editTarget?.item?.title ?? ''}
        initialBody={editTarget?.item?.body ?? ''}
        onClose={() => setEditTarget(null)}
        onSubmit={handleSubmitEdit}
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
        open={pendingDelete != null}
        onClose={() => setPendingDelete(null)}
        aria-labelledby="kanban-delete-title"
      >
        <DialogTitle id="kanban-delete-title">Item löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            „{pendingDelete?.title}" wird unwiderruflich entfernt.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendingDelete(null)}>Abbrechen</Button>
          <Button color="error" variant="contained" onClick={() => void confirmDelete()}>
            Löschen
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
