import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Slider,
  Snackbar,
  Stack,
  Typography,
} from '@mui/material';
import AspectRatioIcon from '@mui/icons-material/AspectRatio';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import { ApiError } from '../../api/client';
import { cropOg, extractPalette } from '../../api/ogImage';

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];
const MAX_BYTES = 10 * 1024 * 1024;
const SLIDER_DEBOUNCE_MS = 300;

function buildOutputFilename(input: File): string {
  const lastDot = input.name.lastIndexOf('.');
  const base = lastDot > 0 ? input.name.slice(0, lastDot) : input.name;
  return `${base || 'featured'}-1200x630.jpg`;
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

export default function OgImagePage() {
  const [file, setFile] = useState<File | null>(null);
  const [yOffset, setYOffset] = useState(0.5);
  const [cropUrl, setCropUrl] = useState<string | null>(null);
  const [palette, setPalette] = useState<string[] | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [snackbar, setSnackbar] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (cropUrl) URL.revokeObjectURL(cropUrl);
    };
  }, [cropUrl]);

  const runCrop = useCallback(async (incoming: File, offset: number) => {
    setIsProcessing(true);
    setError(null);
    try {
      const blob = await cropOg(incoming, offset);
      setCropUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return URL.createObjectURL(blob);
      });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setIsProcessing(false);
    }
  }, []);

  const runPalette = useCallback(async (incoming: File) => {
    try {
      const colors = await extractPalette(incoming, 6);
      setPalette(colors);
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  // Debounced re-crop when slider moves
  useEffect(() => {
    if (!file) return;
    const handle = window.setTimeout(() => {
      void runCrop(file, yOffset);
    }, SLIDER_DEBOUNCE_MS);
    return () => window.clearTimeout(handle);
  }, [file, yOffset, runCrop]);

  const reset = () => {
    if (cropUrl) URL.revokeObjectURL(cropUrl);
    setFile(null);
    setYOffset(0.5);
    setCropUrl(null);
    setPalette(null);
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
    if (cropUrl) URL.revokeObjectURL(cropUrl);
    setCropUrl(null);
    setPalette(null);
    setYOffset(0.5);
    setFile(incoming);
    // initial crop + palette fire via effect (yOffset effect) + manual palette call
    void runPalette(incoming);
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

  const handleCopyHex = async (hex: string) => {
    try {
      await navigator.clipboard.writeText(hex);
      setSnackbar(`${hex} kopiert`);
    } catch {
      setSnackbar('Konnte nicht in Zwischenablage kopieren');
    }
  };

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Beitragsbild (1200×630)
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Bild hochladen, vertikalen Ausschnitt per Slider wählen, JPEG downloaden.
        Außerdem: sechs dominante Farben als Brandpalette zum Kopieren.
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
        <AspectRatioIcon sx={{ fontSize: 32, color: 'text.secondary', mr: 1, verticalAlign: 'middle' }} />
        <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
        <Typography>Bild hier ablegen oder klicken zum Auswählen</Typography>
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
        <>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography gutterBottom>
              Vertikaler Ausschnitt: <strong>{Math.round(yOffset * 100)} %</strong>{' '}
              {yOffset === 0 ? '(oben)' : yOffset === 1 ? '(unten)' : '(Mitte)'}
            </Typography>
            <Slider
              value={yOffset}
              min={0}
              max={1}
              step={0.05}
              onChange={(_, v) => setYOffset(Array.isArray(v) ? v[0] : v)}
              aria-label="Vertikaler Ausschnitt"
            />
            <Stack direction="row" spacing={2}>
              <Button
                variant="outlined"
                startIcon={<RestartAltIcon />}
                onClick={reset}
                disabled={isProcessing}
              >
                Zurücksetzen
              </Button>
              {cropUrl && (
                <Button
                  variant="contained"
                  startIcon={<DownloadIcon />}
                  component="a"
                  href={cropUrl}
                  download={buildOutputFilename(file)}
                >
                  JPEG herunterladen
                </Button>
              )}
            </Stack>
          </Paper>

          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Vorschau (1200×630)
            </Typography>
            {isProcessing && !cropUrl ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : cropUrl ? (
              <Box
                component="img"
                src={cropUrl}
                alt="1200x630 Beitragsbild Vorschau"
                sx={{ maxWidth: '100%', display: 'block', mx: 'auto', borderRadius: 1 }}
              />
            ) : null}
          </Paper>

          {palette && (
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Brandpalette
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {palette.map((hex) => (
                  <Box
                    key={hex}
                    onClick={() => void handleCopyHex(hex)}
                    role="button"
                    tabIndex={0}
                    aria-label={`Farbe ${hex} kopieren`}
                    sx={{
                      width: 96,
                      cursor: 'pointer',
                      textAlign: 'center',
                      '&:hover': { opacity: 0.85 },
                    }}
                  >
                    <Box sx={{ bgcolor: hex, height: 96, borderRadius: 1, border: 1, borderColor: 'divider' }} />
                    <Typography variant="caption" sx={{ display: 'block', mt: 0.5, fontFamily: 'monospace' }}>
                      {hex}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            </Paper>
          )}
        </>
      )}

      <Snackbar
        open={Boolean(snackbar)}
        autoHideDuration={2000}
        onClose={() => setSnackbar(null)}
        message={snackbar ?? ''}
      />
    </>
  );
}
