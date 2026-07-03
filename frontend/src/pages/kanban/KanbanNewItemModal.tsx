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

import { CREATE_BUTTON_BG, CREATE_BUTTON_HOVER } from './statusColors';

const BODY_TEMPLATE =
  '## Kontext\n\n## Aufgabe\n\n## Akzeptanzkriterium\n\n## Abhängigkeiten\n';

interface KanbanNewItemModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (title: string, body: string) => Promise<void> | void;
}

/**
 * Zentriertes Anlage-Modal fuer neue Kanban-Items (Issue #303), analog der Kit-Referenz
 * `kit/board-ui.mjs`: Titel + Beschreibung vorbefuellt mit einer vierteiligen Vorlage,
 * keine Live-Vorschau. Ersetzt {@link KanbanEditDrawer} nur fuer den Anlage-Fall — der
 * Drawer bleibt bis Issue #304 fuer das Bearbeiten bestehen.
 */
export default function KanbanNewItemModal({
  open,
  onClose,
  onSubmit,
}: KanbanNewItemModalProps): JSX.Element {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState(BODY_TEMPLATE);
  const [saving, setSaving] = useState(false);

  // Bei jedem Oeffnen frisch starten — verhindert, dass ein verworfener Draft beim
  // naechsten Anlegen nachklingt.
  useEffect(() => {
    if (open) {
      setTitle('');
      setBody(BODY_TEMPLATE);
    }
  }, [open]);

  const canSubmit = title.trim().length > 0;

  const handleCreate = async (): Promise<void> => {
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
      maxWidth="sm"
      fullWidth
      aria-labelledby="kanban-new-item-title"
    >
      <DialogTitle id="kanban-new-item-title">Neues Item</DialogTitle>
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
            label="Beschreibung"
            multiline
            minRows={8}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            inputProps={{ maxLength: 10_000, 'aria-label': 'Beschreibung' }}
            sx={{ '& textarea': { fontFamily: 'monospace' } }}
            fullWidth
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Abbrechen</Button>
        <Button
          variant="contained"
          onClick={() => void handleCreate()}
          disabled={!canSubmit || saving}
          sx={{
            backgroundColor: CREATE_BUTTON_BG,
            '&:hover': { backgroundColor: CREATE_BUTTON_HOVER },
          }}
        >
          Anlegen
        </Button>
      </DialogActions>
    </Dialog>
  );
}
