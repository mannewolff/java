import { useEffect, useState } from 'react';
import {
  Button,
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

import type { KanbanItem } from '../../api/kanban';
import { cleanupCountdownLabel, cleanupDaysRemaining } from './cleanupCountdown';

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
  const [title, setTitle] = useState(item.title);
  const [body, setBody] = useState(item.body);
  const [saving, setSaving] = useState(false);

  // Beim Oeffnen den Draft aus dem Item uebernehmen — verhindert, dass ein verworfener Draft
  // beim naechsten Oeffnen nachklingt.
  useEffect(() => {
    if (open) {
      setTitle(item.title);
      setBody(item.body);
    }
  }, [open, item.title, item.body]);

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
    >
      <DialogTitle id="kanban-detail-title">Item bearbeiten</DialogTitle>
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
