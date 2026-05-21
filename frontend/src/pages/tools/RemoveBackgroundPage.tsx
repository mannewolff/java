import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import { ApiError } from '../../api/client';
import { removeBackground } from '../../api/tools';

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];
const MAX_BYTES = 10 * 1024 * 1024;

const CHECKERBOARD_SX = {
  backgroundImage:
    'linear-gradient(45deg, #e0e0e0 25%, transparent 25%), linear-gradient(-45deg, #e0e0e0 25%, transparent 25%), linear-gradient(45deg, transparent 75%, #e0e0e0 75%), linear-gradient(-45deg, transparent 75%, #e0e0e0 75%)',
  backgroundSize: '20px 20px',
  backgroundPosition: '0 0, 0 10px, 10px -10px, -10px 0px',
};

function buildOutputFilename(input: File): string {
  const lastDot = input.name.lastIndexOf('.');
  const base = lastDot > 0 ? input.name.slice(0, lastDot) : input.name;
  return `${base || 'image'}-transparent.png`;
}

export default function RemoveBackgroundPage() {
  const [file, setFile] = useState<File | null>(null);
  const [beforeUrl, setBeforeUrl] = useState<string | null>(null);
  const [afterUrl, setAfterUrl] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (beforeUrl) URL.revokeObjectURL(beforeUrl);
      if (afterUrl) URL.revokeObjectURL(afterUrl);
    };
  }, [beforeUrl, afterUrl]);

  const reset = () => {
    if (beforeUrl) URL.revokeObjectURL(beforeUrl);
    if (afterUrl) URL.revokeObjectURL(afterUrl);
    setFile(null);
    setBeforeUrl(null);
    setAfterUrl(null);
    setError(null);
  };

  const acceptFile = (incoming: File) => {
    setError(null);
    if (!ACCEPTED_TYPES.includes(incoming.type)) {
      setError(`Format nicht unterstützt: ${incoming.type || 'unbekannt'}`);
      return;
    }
    if (incoming.size > MAX_BYTES) {
      setError(`Datei zu groß (${(incoming.size / 1024 / 1024).toFixed(1)} MB, max 10 MB)`);
      return;
    }
    if (beforeUrl) URL.revokeObjectURL(beforeUrl);
    if (afterUrl) URL.revokeObjectURL(afterUrl);
    setFile(incoming);
    setBeforeUrl(URL.createObjectURL(incoming));
    setAfterUrl(null);
  };

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = event.target.files?.[0];
    if (selected) acceptFile(selected);
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setIsDragging(false);
    const dropped = event.dataTransfer.files?.[0];
    if (dropped) acceptFile(dropped);
  };

  const handleSubmit = async () => {
    if (!file) return;
    setIsProcessing(true);
    setError(null);
    try {
      const blob = await removeBackground(file);
      if (afterUrl) URL.revokeObjectURL(afterUrl);
      setAfterUrl(URL.createObjectURL(blob));
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Unbekannter Fehler');
      }
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Hintergrund entfernen
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Bild hochladen, das Tool entfernt den Hintergrund per KI (rembg) und liefert ein
        PNG mit Alpha-Kanal zurück.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} role="alert">
          {error}
        </Alert>
      )}

      <Paper
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        sx={{
          p: 4,
          mb: 3,
          textAlign: 'center',
          cursor: 'pointer',
          borderStyle: 'dashed',
          borderWidth: 2,
          borderColor: isDragging ? 'primary.main' : 'divider',
          bgcolor: isDragging ? 'action.hover' : 'background.paper',
        }}
        data-testid="drop-zone"
      >
        <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
        <Typography>
          Bild hier ablegen oder klicken zum Auswählen
        </Typography>
        <Typography variant="caption" color="text.secondary">
          PNG, JPEG oder WEBP, max 10 MB
        </Typography>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(',')}
          onChange={handleInputChange}
          hidden
          aria-label="Bild auswählen"
        />
      </Paper>

      {file && (
        <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
          <Button
            variant="contained"
            startIcon={isProcessing ? <CircularProgress size={16} color="inherit" /> : null}
            disabled={isProcessing}
            onClick={handleSubmit}
          >
            Hintergrund entfernen
          </Button>
          <Button
            variant="outlined"
            startIcon={<RestartAltIcon />}
            onClick={reset}
            disabled={isProcessing}
          >
            Zurücksetzen
          </Button>
          {afterUrl && (
            <Button
              variant="outlined"
              startIcon={<DownloadIcon />}
              component="a"
              href={afterUrl}
              download={buildOutputFilename(file)}
            >
              PNG herunterladen
            </Button>
          )}
        </Stack>
      )}

      {(beforeUrl || afterUrl) && (
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
          {beforeUrl && (
            <Paper sx={{ p: 2, flex: 1 }}>
              <Typography variant="h6" gutterBottom>Vorher</Typography>
              <Box
                component="img"
                src={beforeUrl}
                alt="Original-Bild"
                sx={{ maxWidth: '100%', maxHeight: 400, display: 'block', mx: 'auto' }}
              />
            </Paper>
          )}
          {afterUrl && (
            <Paper sx={{ p: 2, flex: 1 }}>
              <Typography variant="h6" gutterBottom>Nachher</Typography>
              <Box sx={{ ...CHECKERBOARD_SX, p: 1, display: 'flex', justifyContent: 'center' }}>
                <Box
                  component="img"
                  src={afterUrl}
                  alt="Bild mit transparentem Hintergrund"
                  sx={{ maxWidth: '100%', maxHeight: 400, display: 'block' }}
                />
              </Box>
            </Paper>
          )}
        </Stack>
      )}
    </>
  );
}
