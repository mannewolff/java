import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Divider,
  Drawer,
  Slider,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';

const MIN = 1;
const MAX = 30;

interface KanbanSettingsDrawerProps {
  open: boolean;
  /** Aktuell persistierter Wert — Initial-Befüllung des Sliders. */
  currentRetentionDays: number;
  onClose: () => void;
  /** Liefert den neuen Wert; Persistenz übernimmt die Eltern-Komponente. */
  onSubmit: (doneRetentionDays: number) => Promise<void> | void;
}

/**
 * Drawer für die per-User Kanban-Settings. Aktuell genau ein Wert: doneRetentionDays als
 * Slider 1..30 Tage.
 */
export default function KanbanSettingsDrawer({
  open,
  currentRetentionDays,
  onClose,
  onSubmit,
}: KanbanSettingsDrawerProps): JSX.Element {
  const [draft, setDraft] = useState(currentRetentionDays);

  // Beim Öffnen Draft auf den aktuellen persistierten Wert zurücksetzen — sonst hängt der
  // Slider noch auf dem letzten Cancel-Wert.
  useEffect(() => {
    if (open) setDraft(currentRetentionDays);
  }, [open, currentRetentionDays]);

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{ sx: { width: { xs: '100%', sm: 420 } } }}
    >
      <Toolbar />
      <Box sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Kanban-Einstellungen
        </Typography>
        <Stack spacing={2}>
          <Box>
            <Typography gutterBottom>
              Done-Items nach <strong>{draft}</strong> Tagen löschen
            </Typography>
            <Slider
              value={draft}
              min={MIN}
              max={MAX}
              step={1}
              onChange={(_, value) =>
                setDraft(Array.isArray(value) ? value[0] : value)
              }
              valueLabelDisplay="auto"
              aria-label="Done-Retention in Tagen"
            />
            <Typography variant="caption" color="text.secondary">
              Items, die länger als {draft}{' '}
              {draft === 1 ? 'Tag' : 'Tage'} in der Done-Spalte liegen, werden automatisch
              gelöscht. Die Prüfung läuft täglich um 03:00 UTC.
            </Typography>
          </Box>
          <Divider />
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={onClose}>Abbrechen</Button>
            <Button variant="contained" onClick={() => void onSubmit(draft)}>
              Übernehmen
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Drawer>
  );
}
