import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
} from '@mui/material';

import { bulkImportCsv, type BulkImportResult } from '../../api/timeseries';

interface Props {
  open: boolean;
  onClose: () => void;
  timeSeriesId: number;
  onSuccess: () => void;
}

interface FileState {
  name: string;
  content: string;
  previewLines: string[];
}

export default function BulkImportDialog({
  open,
  onClose,
  timeSeriesId,
  onSuccess,
}: Props): JSX.Element {
  const [file, setFile] = useState<FileState | null>(null);
  const [pending, setPending] = useState(false);
  const [result, setResult] = useState<BulkImportResult | null>(null);

  function reset(): void {
    setFile(null);
    setResult(null);
    setPending(false);
  }

  function handleClose(): void {
    reset();
    onClose();
  }

  async function handleFile(e: React.ChangeEvent<HTMLInputElement>): Promise<void> {
    const f = e.target.files?.[0];
    if (!f) return;
    const content = await f.text();
    const lines = content.split(/\r?\n/).filter((l) => l.length > 0);
    setFile({
      name: f.name,
      content,
      previewLines: lines.slice(0, 5),
    });
    setResult(null);
  }

  async function handleImport(): Promise<void> {
    if (!file || pending) return;
    setPending(true);
    try {
      const r = await bulkImportCsv(timeSeriesId, file.content);
      setResult(r);
      if (r.errors.length === 0) {
        onSuccess();
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="md">
      <DialogTitle>CSV-Import</DialogTitle>
      <DialogContent>
        <Stack spacing={2}>
          <Typography variant="body2" color="text.secondary">
            CSV-Format: zwei Spalten <code>timestamp,value</code>. Trennzeichen: Komma. Punkt
            als Dezimaltrenner. Header-Zeile wird automatisch erkannt. Max 50 000 Zeilen.
          </Typography>

          <Button variant="outlined" component="label">
            CSV-Datei wählen
            <input
              type="file"
              accept=".csv,text/csv"
              hidden
              onChange={(e) => void handleFile(e)}
              aria-label="CSV-Datei wählen"
            />
          </Button>

          {file && (
            <>
              <Typography variant="caption">
                Datei: <strong>{file.name}</strong>
              </Typography>
              <Box
                component="pre"
                sx={{
                  m: 0,
                  p: 1.5,
                  bgcolor: 'action.hover',
                  fontFamily: 'monospace',
                  fontSize: 12,
                  borderRadius: 1,
                  overflow: 'auto',
                  maxHeight: 160,
                }}
                aria-label="CSV-Vorschau"
              >
                {file.previewLines.join('\n')}
              </Box>
            </>
          )}

          {result && result.errors.length === 0 && (
            <Alert severity="success">
              {result.inserted} Einträge erfolgreich importiert.
            </Alert>
          )}

          {result && result.errors.length > 0 && (
            <Alert severity="error">
              <Typography variant="body2" sx={{ mb: 1 }}>
                Import abgelehnt — {result.errors.length} Fehler. Es wurde nichts persistiert.
              </Typography>
              <List dense sx={{ maxHeight: 200, overflow: 'auto' }}>
                {result.errors.slice(0, 20).map((err, idx) => (
                  <ListItem key={idx} sx={{ py: 0.25 }}>
                    <ListItemText
                      primary={`Zeile ${err.line}: ${err.reason}`}
                      primaryTypographyProps={{
                        variant: 'caption',
                        sx: { fontFamily: 'monospace' },
                      }}
                    />
                  </ListItem>
                ))}
                {result.errors.length > 20 && (
                  <ListItem>
                    <ListItemText
                      primary={`… und ${result.errors.length - 20} weitere`}
                      primaryTypographyProps={{ variant: 'caption' }}
                    />
                  </ListItem>
                )}
              </List>
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Schließen</Button>
        <Button
          variant="contained"
          onClick={() => void handleImport()}
          disabled={!file || pending}
        >
          Importieren
        </Button>
      </DialogActions>
    </Dialog>
  );
}
