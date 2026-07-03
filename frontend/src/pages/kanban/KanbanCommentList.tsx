import { useState } from 'react';
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
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import ReactMarkdown from 'react-markdown';

import type { KanbanComment } from '../../api/kanban';
import { relativeTime } from './relativeTime';
import { COMMENT_BG, MODAL_BORDER } from './statusColors';

interface KanbanCommentListProps {
  comments: KanbanComment[];
  /** Username des eingeloggten Users; bestimmt, welche Kommentare Edit/Delete anbieten. */
  currentUsername: string | undefined;
  /** Speichert einen geaenderten Body; liefert {@code true} bei Erfolg (beendet Edit-Modus). */
  onUpdate: (id: number, body: string) => Promise<boolean>;
  onDelete: (id: number) => Promise<void>;
}

/** Liste bestehender Kommentare (neueste zuerst) mit Inline-Edit und Loesch-Bestaetigung. */
export default function KanbanCommentList({
  comments,
  currentUsername,
  onUpdate,
  onDelete,
}: KanbanCommentListProps): JSX.Element {
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editBody, setEditBody] = useState('');
  const [pendingDeleteId, setPendingDeleteId] = useState<number | null>(null);

  const startEdit = (comment: KanbanComment): void => {
    setEditingId(comment.id);
    setEditBody(comment.body);
  };

  const cancelEdit = (): void => {
    setEditingId(null);
    setEditBody('');
  };

  const saveEdit = async (id: number): Promise<void> => {
    const trimmed = editBody.trim();
    if (trimmed.length === 0) return;
    const ok = await onUpdate(id, trimmed);
    if (ok) cancelEdit();
  };

  const confirmDelete = async (): Promise<void> => {
    if (pendingDeleteId == null) return;
    await onDelete(pendingDeleteId);
    setPendingDeleteId(null);
  };

  if (comments.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        Noch keine Kommentare.
      </Typography>
    );
  }

  return (
    <Stack spacing={1.5}>
      {comments.map((comment) => {
        const isOwn = currentUsername != null && comment.author === currentUsername;
        const isEditing = editingId === comment.id;
        return (
          <Paper
            key={comment.id}
            variant="outlined"
            data-testid={`kanban-comment-card-${comment.id}`}
            sx={{ p: 1.5 }}
            style={{ backgroundColor: COMMENT_BG, borderColor: MODAL_BORDER }}
          >
            <Box
              sx={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 1 }}
            >
              <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, minWidth: 0 }}>
                <Typography variant="subtitle2" component="span" noWrap>
                  {comment.author}
                </Typography>
                <Typography variant="caption" color="text.secondary" component="span">
                  {relativeTime(comment.createdAt)}
                </Typography>
              </Box>
              {isOwn && !isEditing && (
                <Box sx={{ flexShrink: 0 }}>
                  <IconButton
                    size="small"
                    onClick={() => startEdit(comment)}
                    aria-label="Kommentar bearbeiten"
                  >
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => setPendingDeleteId(comment.id)}
                    aria-label="Kommentar löschen"
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Box>
              )}
            </Box>
            {isEditing ? (
              <Stack spacing={1} sx={{ mt: 1 }}>
                <TextField
                  multiline
                  minRows={2}
                  value={editBody}
                  onChange={(e) => setEditBody(e.target.value)}
                  inputProps={{ maxLength: 10_000, 'aria-label': 'Kommentar bearbeiten' }}
                  fullWidth
                />
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                  <Button size="small" onClick={cancelEdit}>
                    Abbrechen
                  </Button>
                  <Button
                    size="small"
                    variant="contained"
                    onClick={() => void saveEdit(comment.id)}
                    disabled={editBody.trim().length === 0}
                  >
                    Speichern
                  </Button>
                </Box>
              </Stack>
            ) : (
              <Box sx={{ mt: 0.5 }}>
                <ReactMarkdown>{comment.body}</ReactMarkdown>
              </Box>
            )}
          </Paper>
        );
      })}

      <Dialog open={pendingDeleteId != null} onClose={() => setPendingDeleteId(null)}>
        <DialogTitle>Kommentar löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Dieser Kommentar wird dauerhaft gelöscht. Das lässt sich nicht rückgängig machen.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendingDeleteId(null)}>Abbrechen</Button>
          <Button color="error" onClick={() => void confirmDelete()}>
            Löschen
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
