import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Divider,
  Drawer,
  FormControlLabel,
  Slider,
  Stack,
  Switch,
  Toolbar,
  Typography,
} from '@mui/material';

const MIN = 1;
const MAX = 30;

interface KanbanSettingsDrawerProps {
  open: boolean;
  currentRetentionDays: number;
  showArchived: boolean;
  onClose: () => void;
  onSubmit: (doneRetentionDays: number, showArchived: boolean) => Promise<void> | void;
}

export default function KanbanSettingsDrawer({
  open,
  currentRetentionDays,
  showArchived,
  onClose,
  onSubmit,
}: KanbanSettingsDrawerProps): JSX.Element {
  const [draft, setDraft] = useState(currentRetentionDays);
  const [draftShowArchived, setDraftShowArchived] = useState(showArchived);

  useEffect(() => {
    if (open) {
      setDraft(currentRetentionDays);
      setDraftShowArchived(showArchived);
    }
  }, [open, currentRetentionDays, showArchived]);

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
          <FormControlLabel
            control={
              <Switch
                checked={draftShowArchived}
                onChange={(e) => setDraftShowArchived(e.target.checked)}
                aria-label="Archivierte Items anzeigen"
              />
            }
            label="Archivierte Items anzeigen"
          />
          <Divider />
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={onClose}>Abbrechen</Button>
            <Button
              variant="contained"
              onClick={() => void onSubmit(draft, draftShowArchived)}
            >
              Übernehmen
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Drawer>
  );
}
