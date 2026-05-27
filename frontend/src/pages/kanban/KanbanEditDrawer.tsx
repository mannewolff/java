import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Divider,
  Drawer,
  Paper,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import ReactMarkdown from 'react-markdown';

interface KanbanEditDrawerProps {
  open: boolean;
  initialTitle: string;
  initialBody: string;
  /** Heading-Text: "Neues Kanban-Item" beim Anlegen, "Item bearbeiten" beim Edit. */
  heading: string;
  onClose: () => void;
  onSubmit: (title: string, body: string) => Promise<void> | void;
}

/**
 * Drawer zum Anlegen und Bearbeiten eines Kanban-Items. Analog WidgetTextbox: Title-Field +
 * Markdown-Textarea mit Live-Preview.
 */
export default function KanbanEditDrawer({
  open,
  initialTitle,
  initialBody,
  heading,
  onClose,
  onSubmit,
}: KanbanEditDrawerProps): JSX.Element {
  const [title, setTitle] = useState(initialTitle);
  const [body, setBody] = useState(initialBody);

  // Beim Oeffnen den Draft aus den Props uebernehmen — verhindert, dass ein vorhergehender
  // Cancel-Draft den naechsten Edit verschmutzt.
  useEffect(() => {
    if (open) {
      setTitle(initialTitle);
      setBody(initialBody);
    }
  }, [open, initialTitle, initialBody]);

  const canSubmit = title.trim().length > 0;

  const handleApply = async (): Promise<void> => {
    if (!canSubmit) return;
    await onSubmit(title.trim(), body);
  };

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{ sx: { width: { xs: '100%', sm: 520 } } }}
    >
      <Toolbar />
      <Box sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          {heading}
        </Typography>
        <Stack spacing={2}>
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
          <Box>
            <Typography variant="caption" color="text.secondary">
              Live-Vorschau
            </Typography>
            <Paper
              variant="outlined"
              sx={{ p: 2, mt: 0.5, minHeight: 80 }}
              aria-label="Live-Vorschau"
            >
              <ReactMarkdown>{body}</ReactMarkdown>
            </Paper>
          </Box>
          <Divider />
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={onClose}>Abbrechen</Button>
            <Button
              variant="contained"
              onClick={() => void handleApply()}
              disabled={!canSubmit}
            >
              Übernehmen
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Drawer>
  );
}
