import { useEffect, useState } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';

import type { KanbanEpic } from '../../api/kanban';
import { epicShortcode } from './epicMeta';
import { CREATE_BUTTON_BG, CREATE_BUTTON_HOVER } from './statusColors';

interface KanbanEpicEditModalProps {
  open: boolean;
  epic: KanbanEpic | null;
  onClose: () => void;
  onSubmit: (title: string, body: string, shortcode: string | null) => Promise<void> | void;
}

/**
 * Bearbeiten-Modal für ein Epic (#331): Titel, Beschreibung (Roh-Markdown) und optionales Kürzel.
 * Die Felder werden bei jedem Öffnen aus dem übergebenen Epic vorbefüllt. Leeres Kürzel → {@code
 * null} (die Ableitung aus dem Titel greift dann wieder).
 */
export default function KanbanEpicEditModal({
  open,
  epic,
  onClose,
  onSubmit,
}: KanbanEpicEditModalProps): JSX.Element {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [shortcode, setShortcode] = useState('');
  const [saving, setSaving] = useState(false);

  // Bei jedem Öffnen die Felder aus dem aktuellen Epic vorbefüllen.
  useEffect(() => {
    if (!open || !epic) return;
    setTitle(epic.title);
    setBody(epic.body);
    setShortcode(epic.shortcode ?? '');
  }, [open, epic]);

  const canSubmit = title.trim().length > 0;

  const handleSave = async (): Promise<void> => {
    if (!canSubmit || saving) return;
    setSaving(true);
    const effectiveShortcode = shortcode.trim() ? shortcode.trim() : null;
    try {
      await onSubmit(title.trim(), body, effectiveShortcode);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="kanban-epic-edit-title"
    >
      <DialogTitle id="kanban-epic-edit-title">Epic bearbeiten</DialogTitle>
      <DialogContent>
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
            label="Kürzel (optional)"
            value={shortcode}
            onChange={(e) => setShortcode(e.target.value)}
            placeholder={epicShortcode(title)}
            inputProps={{ maxLength: 16, 'aria-label': 'Kürzel' }}
            helperText="Leer lassen, um es aus dem Titel abzuleiten."
            fullWidth
          />
          <TextField
            label="Beschreibung"
            multiline
            rows={8}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            inputProps={{ maxLength: 10_000, 'aria-label': 'Beschreibung' }}
            sx={{ '& textarea': { fontFamily: 'monospace', resize: 'vertical' } }}
            fullWidth
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Abbrechen</Button>
        <Button
          variant="contained"
          onClick={() => void handleSave()}
          disabled={!canSubmit || saving}
          sx={{
            backgroundColor: CREATE_BUTTON_BG,
            '&:hover': { backgroundColor: CREATE_BUTTON_HOVER },
          }}
        >
          Speichern
        </Button>
      </DialogActions>
    </Dialog>
  );
}
