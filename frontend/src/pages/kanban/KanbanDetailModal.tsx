import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ReactMarkdown from 'react-markdown';

import {
  addKanbanComment,
  deleteKanbanComment,
  listKanbanComments,
  updateKanbanComment,
} from '../../api/kanban';
import type { KanbanComment, KanbanItem } from '../../api/kanban';
import { useAuth } from '../../auth/useAuth';
import { cleanupCountdownLabel, cleanupDaysRemaining } from './cleanupCountdown';
import { COLUMN_LABELS } from './columnMeta';
import { MODAL_BORDER, MODAL_TEXT_PRIMARY, MODAL_TEXT_SECONDARY, STATUS_COLORS } from './statusColors';
import KanbanCommentForm from './KanbanCommentForm';
import KanbanCommentList from './KanbanCommentList';

interface KanbanDetailModalProps {
  open: boolean;
  item: KanbanItem;
  retentionDays: number;
  onClose: () => void;
  onSubmit: (title: string, body: string) => Promise<void> | void;
}

/**
 * Detail-Modal eines Kanban-Items: zeigt Titel + Markdown-Body sofort bearbeitbar (analog
 * {@link KanbanEditDrawer}, aber als zentrierter {@link Dialog} statt Drawer). Standalone und
 * controlled — Open/Item-State liegt beim Aufrufer, damit das Modal auch ausserhalb des Boards
 * (Dashboard-Widget) wiederverwendbar ist.
 *
 * {@code DialogContent} scrollt vertikal, falls der Inhalt hoeher als der Viewport ist; der
 * Dialog waechst dank {@code scroll="paper"} nicht ueber das Browserfenster hinaus.
 */
export default function KanbanDetailModal({
  open,
  item,
  retentionDays,
  onClose,
  onSubmit,
}: KanbanDetailModalProps): JSX.Element {
  const { username } = useAuth();
  const [title, setTitle] = useState(item.title);
  const [body, setBody] = useState(item.body);
  const [saving, setSaving] = useState(false);

  const [comments, setComments] = useState<KanbanComment[]>([]);
  const [loadingComments, setLoadingComments] = useState(false);
  const [commentBusy, setCommentBusy] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);

  // Beim Oeffnen den Draft aus dem Item uebernehmen — verhindert, dass ein verworfener Draft
  // beim naechsten Oeffnen nachklingt.
  useEffect(() => {
    if (open) {
      setTitle(item.title);
      setBody(item.body);
    }
  }, [open, item.title, item.body]);

  const refreshComments = useCallback(async (): Promise<void> => {
    setComments(await listKanbanComments(item.id));
  }, [item.id]);

  // Kommentare beim Oeffnen laden. Ein cancelled-Flag verhindert State-Updates, falls das Modal
  // vor Abschluss des Requests wieder geschlossen (bzw. die Komponente neu gerendert) wird.
  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoadingComments(true);
    setCommentError(null);
    listKanbanComments(item.id)
      .then((loaded) => {
        if (!cancelled) setComments(loaded);
      })
      .catch(() => {
        if (!cancelled) setCommentError('Kommentare konnten nicht geladen werden.');
      })
      .finally(() => {
        if (!cancelled) setLoadingComments(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open, item.id]);

  const handleAddComment = async (text: string): Promise<boolean> => {
    setCommentBusy(true);
    setCommentError(null);
    try {
      await addKanbanComment(item.id, text);
      await refreshComments();
      return true;
    } catch {
      setCommentError('Kommentar konnte nicht gespeichert werden.');
      return false;
    } finally {
      setCommentBusy(false);
    }
  };

  const handleUpdateComment = async (id: number, text: string): Promise<boolean> => {
    setCommentError(null);
    try {
      await updateKanbanComment(item.id, id, text);
      await refreshComments();
      return true;
    } catch {
      setCommentError('Kommentar konnte nicht aktualisiert werden.');
      return false;
    }
  };

  const handleDeleteComment = async (id: number): Promise<void> => {
    setCommentError(null);
    try {
      await deleteKanbanComment(item.id, id);
      await refreshComments();
    } catch {
      setCommentError('Kommentar konnte nicht gelöscht werden.');
    }
  };

  const canSubmit = title.trim().length > 0;
  const showDoneHint = item.column === 'DONE' && item.movedToDoneAt != null;
  const daysRemaining = showDoneHint
    ? cleanupDaysRemaining(item.movedToDoneAt!, retentionDays)
    : 0;

  const handleSave = async (): Promise<void> => {
    if (!canSubmit || saving) return;
    setSaving(true);
    try {
      await onSubmit(title.trim(), body);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      scroll="paper"
      maxWidth="sm"
      fullWidth
      aria-labelledby="kanban-detail-title"
      PaperProps={{ sx: { borderRadius: '8px', boxShadow: '0 8px 32px rgba(0,0,0,.24)' } }}
    >
      <DialogTitle
        id="kanban-detail-title"
        data-testid="kanban-detail-header"
        style={{ borderBottom: `1px solid ${MODAL_BORDER}` }}
      >
        <Stack direction="row" alignItems="center" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Chip
            label={COLUMN_LABELS[item.column]}
            size="small"
            sx={{
              bgcolor: STATUS_COLORS[item.column].bg,
              color: STATUS_COLORS[item.column].text,
              fontWeight: 600,
              borderRadius: '12px',
            }}
          />
          {item.number > 0 && (
            <Typography component="span" variant="body2" style={{ color: MODAL_TEXT_SECONDARY }}>
              #{item.number}
            </Typography>
          )}
          <Typography component="span" style={{ fontWeight: 600, color: MODAL_TEXT_PRIMARY }}>
            {item.title}
          </Typography>
        </Stack>
      </DialogTitle>
      <DialogContent dividers sx={{ overflowY: 'auto' }}>
        <Stack spacing={2} sx={{ mt: 0.5 }}>
          <TextField
            label="Titel"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            inputProps={{ maxLength: 200, 'aria-label': 'Titel' }}
            fullWidth
            autoFocus
          />
          <TextField
            label="Markdown-Beschreibung"
            multiline
            minRows={6}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            inputProps={{ maxLength: 10_000, 'aria-label': 'Markdown-Beschreibung' }}
            fullWidth
          />
          <Stack spacing={0.5}>
            <Typography variant="caption" color="text.secondary">
              Live-Vorschau
            </Typography>
            <Paper variant="outlined" sx={{ p: 2, minHeight: 80 }} aria-label="Live-Vorschau">
              <ReactMarkdown>{body}</ReactMarkdown>
            </Paper>
          </Stack>
          {showDoneHint && (
            <Typography variant="caption" color="text.secondary">
              {cleanupCountdownLabel(daysRemaining)}
            </Typography>
          )}

          <Divider />

          <Stack spacing={1.5}>
            <Typography variant="subtitle1">Kommentare</Typography>
            {commentError && <Alert severity="error">{commentError}</Alert>}
            {loadingComments ? (
              <Stack alignItems="center" sx={{ py: 2 }}>
                <CircularProgress size={24} aria-label="Kommentare werden geladen" />
              </Stack>
            ) : (
              <KanbanCommentList
                comments={comments}
                currentUsername={username}
                onUpdate={handleUpdateComment}
                onDelete={handleDeleteComment}
              />
            )}
            <KanbanCommentForm onAdd={handleAddComment} busy={commentBusy} />
          </Stack>
        </Stack>
      </DialogContent>
      <Divider />
      <DialogActions>
        <Button onClick={onClose}>Abbrechen</Button>
        <Button
          variant="contained"
          onClick={() => void handleSave()}
          disabled={!canSubmit || saving}
        >
          Speichern
        </Button>
      </DialogActions>
    </Dialog>
  );
}
