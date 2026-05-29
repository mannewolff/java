import { useState } from 'react';
import { Box, Button, TextField } from '@mui/material';

interface KanbanCommentFormProps {
  /** Legt einen Kommentar an; liefert {@code true} bei Erfolg, damit das Feld geleert wird. */
  onAdd: (body: string) => Promise<boolean>;
  busy: boolean;
}

/** Eingabebereich am Fuss der Kommentarliste: Markdown-Textfeld + Absende-Button. */
export default function KanbanCommentForm({ onAdd, busy }: KanbanCommentFormProps): JSX.Element {
  const [body, setBody] = useState('');

  const trimmed = body.trim();
  const canSubmit = trimmed.length > 0 && !busy;

  const handleSubmit = async (): Promise<void> => {
    if (!canSubmit) return;
    const ok = await onAdd(trimmed);
    if (ok) setBody('');
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
      <TextField
        label="Kommentar"
        multiline
        minRows={2}
        value={body}
        onChange={(e) => setBody(e.target.value)}
        inputProps={{ maxLength: 10_000, 'aria-label': 'Neuer Kommentar' }}
        fullWidth
      />
      <Button
        variant="contained"
        onClick={() => void handleSubmit()}
        disabled={!canSubmit}
        sx={{ alignSelf: 'flex-end' }}
      >
        Kommentar hinzufügen
      </Button>
    </Box>
  );
}
