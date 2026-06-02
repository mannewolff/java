import { useState } from 'react';
import type { FormEvent } from 'react';
import { Box, Button, Chip, Paper, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SmartphoneIcon from '@mui/icons-material/Smartphone';
import LinkOffIcon from '@mui/icons-material/LinkOff';

import { ApiError } from '../../api/client';
import { createKanbanItem } from '../../api/kanban';
import { clearMobileDevice, isMobileDevice } from '../../auth/mobileDevice';
import { useAuth } from '../../auth/useAuth';
import { useNotify } from '../../notify/NotifyProvider';

const MAX_TITLE = 200;
const MAX_BODY = 10_000;

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

/**
 * Minimale Mobile-Seite zur schnellen Ideen-Erfassung (#195). Erstellt ein Kanban-Item
 * direkt im BACKLOG. Die Sidebar wird auf dieser Route automatisch eingeklappt (AppShell).
 */
export default function MobilePage(): JSX.Element {
  const notify = useNotify();
  const { signOut } = useAuth();
  const paired = isMobileDevice();
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const trimmedTitle = title.trim();
  const canSubmit = trimmedTitle.length > 0 && !submitting;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    try {
      await createKanbanItem(trimmedTitle, body.trim(), 'BACKLOG');
      setTitle('');
      setBody('');
      notify.success('Item im Backlog erstellt');
    } catch (err) {
      notify.error(`Erstellen fehlgeschlagen: ${errorMessage(err)}`);
    } finally {
      setSubmitting(false);
    }
  };

  const handleUnpair = (): void => {
    clearMobileDevice();
    notify.success('Kopplung aufgehoben');
    signOut();
  };

  return (
    <Box sx={{ maxWidth: 520, mx: 'auto' }}>
      {paired && (
        <Stack
          direction="row"
          spacing={1}
          alignItems="center"
          justifyContent="space-between"
          sx={{ mb: 2 }}
        >
          <Chip
            icon={<SmartphoneIcon />}
            label="Gerät gekoppelt (30 Tage)"
            color="success"
            variant="outlined"
            size="small"
          />
          <Button
            size="small"
            color="inherit"
            startIcon={<LinkOffIcon />}
            onClick={handleUnpair}
          >
            Kopplung aufheben
          </Button>
        </Stack>
      )}
      <Typography variant="h5" component="h1" gutterBottom>
        Item erstellen
      </Typography>
      <Paper sx={{ p: 2 }}>
        <Box component="form" onSubmit={handleSubmit} noValidate>
          <Stack spacing={2}>
            <TextField
              label="Titel"
              placeholder="Titel"
              value={title}
              onChange={(e) => setTitle(e.target.value.slice(0, MAX_TITLE))}
              required
              fullWidth
              autoFocus
              inputProps={{ maxLength: MAX_TITLE, 'aria-label': 'Titel' }}
            />
            <TextField
              label="Beschreibung (optional)"
              placeholder="Beschreibung (optional)"
              value={body}
              onChange={(e) => setBody(e.target.value.slice(0, MAX_BODY))}
              multiline
              minRows={4}
              fullWidth
              inputProps={{ maxLength: MAX_BODY, 'aria-label': 'Beschreibung' }}
            />
            <Button
              type="submit"
              variant="contained"
              startIcon={<AddIcon />}
              disabled={!canSubmit}
              fullWidth
            >
              Item erstellen
            </Button>
          </Stack>
        </Box>
      </Paper>
    </Box>
  );
}
