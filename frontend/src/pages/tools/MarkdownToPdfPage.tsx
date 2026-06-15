import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';

import { ApiError } from '../../api/client';
import { convertMarkdownToPdf } from '../../api/markdownToPdf';
import { useNotify } from '../../notify/NotifyProvider';

const MAX_CHARS = 1_000_000;

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

export default function MarkdownToPdfPage(): JSX.Element {
  const [markdown, setMarkdown] = useState('');
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const notify = useNotify();
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (pdfUrl) URL.revokeObjectURL(pdfUrl);
    };
  }, [pdfUrl]);

  const handleFile = (event: ChangeEvent<HTMLInputElement>): void => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_CHARS) {
      notify.error('Datei zu groß (max 1 MB).');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => setMarkdown(String(reader.result ?? ''));
    reader.onerror = () => notify.error('Datei konnte nicht gelesen werden.');
    reader.readAsText(file);
    // Erlaubt erneutes Auswählen derselben Datei.
    event.target.value = '';
  };

  const handleConvert = async (): Promise<void> => {
    if (markdown.trim() === '') return;
    setIsProcessing(true);
    try {
      const blob = await convertMarkdownToPdf(markdown);
      if (pdfUrl) URL.revokeObjectURL(pdfUrl);
      setPdfUrl(URL.createObjectURL(blob));
    } catch (err) {
      notify.error(errorMessage(err));
    } finally {
      setIsProcessing(false);
    }
  };

  const canConvert = markdown.trim() !== '' && !isProcessing;

  return (
    <>
      <Typography variant="h4" sx={{ mb: 1 }}>
        Markdown zu PDF
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Markdown-Datei hochladen oder Text direkt eingeben, dann ein PDF erzeugen, in der Vorschau
        ansehen und herunterladen.
      </Typography>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="stretch">
        <Paper sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column' }}>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
            <Button
              variant="outlined"
              startIcon={<CloudUploadIcon />}
              onClick={() => inputRef.current?.click()}
            >
              Markdown-Datei
            </Button>
            <input
              ref={inputRef}
              type="file"
              accept=".md,.markdown,text/markdown,text/plain"
              onChange={handleFile}
              hidden
              aria-label="Markdown-Datei auswählen"
            />
            <Typography variant="caption" color="text.secondary">
              .md / .markdown, max 1 MB
            </Typography>
          </Stack>
          <TextField
            label="Markdown"
            value={markdown}
            onChange={(e) => setMarkdown(e.target.value)}
            multiline
            minRows={16}
            fullWidth
            placeholder={'# Überschrift\n\nText, **fett**, Listen, Tabellen …'}
            inputProps={{ 'aria-label': 'Markdown', maxLength: MAX_CHARS }}
            sx={{ flex: 1 }}
          />
          <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
            <Button
              variant="contained"
              onClick={handleConvert}
              disabled={!canConvert}
              startIcon={
                isProcessing ? (
                  <CircularProgress size={16} color="inherit" />
                ) : (
                  <PictureAsPdfIcon />
                )
              }
            >
              PDF erzeugen
            </Button>
            {pdfUrl && (
              <Button
                variant="outlined"
                startIcon={<DownloadIcon />}
                component="a"
                href={pdfUrl}
                download="document.pdf"
              >
                PDF herunterladen
              </Button>
            )}
          </Stack>
        </Paper>

        <Paper sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column', minHeight: 480 }}>
          <Typography variant="h6" gutterBottom>
            Vorschau
          </Typography>
          {pdfUrl ? (
            <Box
              component="iframe"
              title="PDF-Vorschau"
              src={pdfUrl}
              sx={{ flex: 1, width: '100%', border: 1, borderColor: 'divider', borderRadius: 1 }}
            />
          ) : (
            <Box
              sx={{
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'text.secondary',
                textAlign: 'center',
              }}
            >
              <Typography>Noch kein PDF. Links Markdown eingeben und „PDF erzeugen".</Typography>
            </Box>
          )}
        </Paper>
      </Stack>
    </>
  );
}
