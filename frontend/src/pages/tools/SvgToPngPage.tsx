import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Divider,
  Drawer,
  IconButton,
  Paper,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import SettingsIcon from '@mui/icons-material/Settings';
import TransformIcon from '@mui/icons-material/Transform';

import { ApiError } from '../../api/client';
import { convertSvgToPng } from '../../api/svgToPng';
import { useNotify } from '../../notify/NotifyProvider';

const ACCEPTED_TYPES = ['image/svg+xml'];
const MAX_BYTES = 10 * 1024 * 1024;
const MIN_DIMENSION = 1;
const MAX_DIMENSION = 8192;
const BACKGROUND_PATTERN = /^(transparent|#[0-9a-fA-F]{6})$/;

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

function buildOutputFilename(input: File, width: number | null, height: number | null): string {
  const lastDot = input.name.lastIndexOf('.');
  const base = lastDot > 0 ? input.name.slice(0, lastDot) : input.name;
  if (width != null && height != null) return `${base || 'image'}-${width}x${height}.png`;
  if (width != null) return `${base || 'image'}-w${width}.png`;
  if (height != null) return `${base || 'image'}-h${height}.png`;
  return `${base || 'image'}.png`;
}

export default function SvgToPngPage(): JSX.Element {
  const [file, setFile] = useState<File | null>(null);
  const [sourceUrl, setSourceUrl] = useState<string | null>(null);
  const [resultUrl, setResultUrl] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const notify = useNotify();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [targetW, setTargetW] = useState('');
  const [targetH, setTargetH] = useState('');
  const [background, setBackground] = useState('transparent');
  const [draftBackground, setDraftBackground] = useState('transparent');
  const [draftBackgroundError, setDraftBackgroundError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    };
  }, [sourceUrl]);

  useEffect(() => {
    return () => {
      if (resultUrl) URL.revokeObjectURL(resultUrl);
    };
  }, [resultUrl]);

  const acceptFile = (incoming: File): void => {
    if (!ACCEPTED_TYPES.includes(incoming.type)) {
      notify.error(`Format nicht unterstützt: ${incoming.type || 'unbekannt'} (erwartet: SVG)`);
      return;
    }
    if (incoming.size > MAX_BYTES) {
      notify.error(`Datei zu groß (${(incoming.size / 1024 / 1024).toFixed(1)} MB, max 10 MB)`);
      return;
    }
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    if (resultUrl) URL.revokeObjectURL(resultUrl);
    setFile(incoming);
    setResultUrl(null);
    setTargetW('');
    setTargetH('');
    setSourceUrl(URL.createObjectURL(incoming));
  };

  const reset = (): void => {
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    if (resultUrl) URL.revokeObjectURL(resultUrl);
    setFile(null);
    setSourceUrl(null);
    setResultUrl(null);
    setTargetW('');
    setTargetH('');
  };

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const selected = event.target.files?.[0];
    if (selected) acceptFile(selected);
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>): void => {
    event.preventDefault();
    setIsDragging(false);
    const dropped = event.dataTransfer.files?.[0];
    if (dropped) acceptFile(dropped);
  };

  const parseDimension = (raw: string): number | null => {
    if (raw.trim() === '') return null;
    const v = Number.parseInt(raw, 10);
    return Number.isFinite(v) ? v : null;
  };

  const handleSubmit = async (): Promise<void> => {
    if (!file) return;
    const w = parseDimension(targetW);
    const h = parseDimension(targetH);
    if (w != null && (w < MIN_DIMENSION || w > MAX_DIMENSION)) {
      notify.error(`Breite außerhalb 1..${MAX_DIMENSION} px.`);
      return;
    }
    if (h != null && (h < MIN_DIMENSION || h > MAX_DIMENSION)) {
      notify.error(`Höhe außerhalb 1..${MAX_DIMENSION} px.`);
      return;
    }

    setIsProcessing(true);
    try {
      const { blob } = await convertSvgToPng(file, {
        width: w ?? undefined,
        height: h ?? undefined,
        background,
      });
      if (resultUrl) URL.revokeObjectURL(resultUrl);
      setResultUrl(URL.createObjectURL(blob));
    } catch (err) {
      notify.error(errorMessage(err));
    } finally {
      setIsProcessing(false);
    }
  };

  const openSettings = (): void => {
    setDraftBackground(background);
    setDraftBackgroundError(null);
    setSettingsOpen(true);
  };

  const applySettings = (): void => {
    if (!BACKGROUND_PATTERN.test(draftBackground)) {
      setDraftBackgroundError('Erlaubt: „transparent" oder #rrggbb (z. B. #ffffff)');
      return;
    }
    setBackground(draftBackground);
    setSettingsOpen(false);
  };

  const downloadFilename = file
    ? buildOutputFilename(file, parseDimension(targetW), parseDimension(targetH))
    : 'image.png';

  return (
    <>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="h4">SVG zu PNG</Typography>
        <Tooltip title="Hintergrundfarbe einstellen">
          <IconButton onClick={openSettings} aria-label="Einstellungen öffnen">
            <SettingsIcon />
          </IconButton>
        </Tooltip>
      </Stack>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        SVG hochladen, optional eine Zielgröße eintragen. Ohne Maße nimmt der Konverter die
        SVG-eigene Geometrie. Hintergrundfarbe per Einstellungen-Drawer.
      </Typography>

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
        <TransformIcon
          sx={{ fontSize: 32, color: 'text.secondary', mr: 1, verticalAlign: 'middle' }}
        />
        <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
        <Typography>SVG hier ablegen oder klicken zum Auswählen</Typography>
        <Typography variant="caption" color="text.secondary">
          image/svg+xml, max 10 MB
        </Typography>
        <input
          ref={inputRef}
          type="file"
          accept="image/svg+xml,.svg"
          onChange={handleInputChange}
          hidden
          aria-label="SVG auswählen"
        />
      </Paper>

      {file && (
        <>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography gutterBottom>
              Quelle: <strong>{file.name}</strong>{' '}
              <Typography component="span" variant="caption" color="text.secondary">
                ({(file.size / 1024).toFixed(1)} KB)
              </Typography>
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
              <TextField
                label="Breite (px, optional)"
                type="number"
                value={targetW}
                onChange={(e) => setTargetW(e.target.value)}
                inputProps={{
                  min: MIN_DIMENSION,
                  max: MAX_DIMENSION,
                  'aria-label': 'Breite',
                }}
                sx={{ flex: 1 }}
              />
              <TextField
                label="Höhe (px, optional)"
                type="number"
                value={targetH}
                onChange={(e) => setTargetH(e.target.value)}
                inputProps={{
                  min: MIN_DIMENSION,
                  max: MAX_DIMENSION,
                  'aria-label': 'Höhe',
                }}
                sx={{ flex: 1 }}
              />
            </Stack>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
              Hintergrund:{' '}
              <strong>{background === 'transparent' ? 'transparent' : background}</strong>{' '}
              (ändern über das Zahnrad oben rechts)
            </Typography>
            <Stack direction="row" spacing={2}>
              <Button
                variant="contained"
                onClick={handleSubmit}
                disabled={isProcessing}
                startIcon={
                  isProcessing ? <CircularProgress size={16} color="inherit" /> : <TransformIcon />
                }
              >
                Konvertieren
              </Button>
              <Button
                variant="outlined"
                startIcon={<RestartAltIcon />}
                onClick={reset}
                disabled={isProcessing}
              >
                Zurücksetzen
              </Button>
              {resultUrl && (
                <Button
                  variant="outlined"
                  startIcon={<DownloadIcon />}
                  component="a"
                  href={resultUrl}
                  download={downloadFilename}
                >
                  Herunterladen
                </Button>
              )}
            </Stack>
          </Paper>

          {resultUrl && (
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Ergebnis
              </Typography>
              <Box
                component="img"
                src={resultUrl}
                alt="Konvertiertes PNG"
                sx={{
                  width: '100%',
                  maxWidth: 800,
                  display: 'block',
                  mx: 'auto',
                  borderRadius: 1,
                  border: 1,
                  borderColor: 'divider',
                  imageRendering: 'auto',
                }}
              />
            </Paper>
          )}
        </>
      )}

      <Drawer
        anchor="right"
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 360 } } }}
      >
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Hintergrund
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Bestimmt die Hintergrundfarbe des PNGs. „transparent" lässt den Alpha-Kanal offen,
            sonst muss ein Hex-Code im Format <code>#rrggbb</code> verwendet werden.
          </Typography>
          <Stack spacing={2}>
            <TextField
              label="Hintergrund"
              value={draftBackground}
              onChange={(e) => {
                setDraftBackground(e.target.value);
                setDraftBackgroundError(null);
              }}
              error={draftBackgroundError != null}
              helperText={draftBackgroundError ?? 'z. B. transparent, #ffffff, #aabbcc'}
              fullWidth
              inputProps={{ 'aria-label': 'Hintergrund' }}
            />
            <Divider />
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={() => setSettingsOpen(false)}>Abbrechen</Button>
              <Button variant="contained" onClick={applySettings}>
                Übernehmen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>
    </>
  );
}
