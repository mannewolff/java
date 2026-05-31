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
  archiveKanbanItem,
  createKanbanItem,
  forceDeleteKanbanItem,
  getKanbanSettings,
  listKanbanItems,
  moveKanbanItem,
  restoreKanbanItem,
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

const DEFAULT_RETENTION_DAYS = 5;
const SHOW_ARCHIVED_KEY = 'kanban.showArchived';

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
  item: KanbanItem | null;
  defaultColumn: KanbanColumnId;
}

function loadShowArchived(): boolean {
  try {
    return localStorage.getItem(SHOW_ARCHIVED_KEY) === 'true';
  } catch {
    return false;
  }
}

function saveShowArchived(value: boolean): void {
  try {
    localStorage.setItem(SHOW_ARCHIVED_KEY, String(value));
  } catch {
    // localStorage nicht verfügbar
  }
}

export default function KanbanPage(): JSX.Element {
  const notify = useNotify();
  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [editTarget, setEditTarget] = useState<EditTarget | null>(null);
  const [detailItem, setDetailItem] = useState<KanbanItem | null>(null);
  const [pendingArchive, setPendingArchive] = useState<KanbanItem | null>(null);
  const [pendingForceDelete, setPendingForceDelete] = useState<KanbanItem | null>(null);
  const [retentionDays, setRetentionDays] = useState(DEFAULT_RETENTION_DAYS);
  const [showArchived, setShowArchived] = useState(loadShowArchived);
  const [settingsOpen, setSettingsOpen] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 5 },
    }),
  );

  const reload = useCallback(async (withArchived: boolean): Promise<void> => {
    try {
      const board = await listKanbanItems(withArchived);
      const safeBoard: KanbanBoard = { ...emptyBoard(), ...board };
      setState({ kind: 'ready', board: safeBoard });
    } catch (e) {
      setState({
        kind: 'error',
        message: e instanceof ApiError ? e.message : 'Kanban-Items konnten nicht geladen werden.',
      });
    }
  }, []);

  useEffect(() => {
    void reload(showArchived);
    void getKanbanSettings()
      .then((s) => setRetentionDays(s.doneRetentionDays))
      .catch(() => {
        // Default bleibt
      });
  }, [reload, showArchived]);

  async function handleSettingsSubmit(
    doneRetentionDays: number,
    newShowArchived: boolean,
  ): Promise<void> {
    try {
      const saved = await updateKanbanSettings(doneRetentionDays);
      setRetentionDays(saved.doneRetentionDays);
      if (newShowArchived !== showArchived) {
        saveShowArchived(newShowArchived);
        setShowArchived(newShowArchived);
      }
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
      await reload(showArchived);
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
      await reload(showArchived);
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
      await reload(showArchived);
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Archivieren fehlgeschlagen.');
    }
  }

  async function handleRestore(item: KanbanItem): Promise<void> {
    try {
      await restoreKanbanItem(item.id);
      notify.success('Item wiederhergestellt.');
      await reload(showArchived);
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
      await reload(showArchived);
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

    try {
      await moveKanbanItem(itemId, targetColumn, targetPosition);
      await reload(showArchived);
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
                onArchive={setPendingArchive}
                onRestore={(item) => void handleRestore(item)}
                onForceDelete={setPendingForceDelete}
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
        showArchived={showArchived}
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
