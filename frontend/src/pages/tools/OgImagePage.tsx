import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Divider,
  Drawer,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import AspectRatioIcon from '@mui/icons-material/AspectRatio';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import SettingsIcon from '@mui/icons-material/Settings';
import { ApiError } from '../../api/client';
import { cropOg, extractPalette } from '../../api/ogImage';
import InteractiveCropFrame, { type CropOffsets } from '../../components/InteractiveCropFrame';
import { useNotify } from '../../notify/NotifyProvider';

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];
const MAX_BYTES = 10 * 1024 * 1024;
const CROP_DEBOUNCE_MS = 250;
const MIN_DIMENSION = 200;
const MAX_DIMENSION = 4096;

interface Preset {
  label: string;
  width: number;
  height: number;
}

const PRESETS: Preset[] = [
  { label: 'OpenGraph (1200×630)', width: 1200, height: 630 },
  { label: 'Twitter Card (1200×675)', width: 1200, height: 675 },
  { label: 'LinkedIn (1200×627)', width: 1200, height: 627 },
  { label: 'Quadrat (1080×1080)', width: 1080, height: 1080 },
  { label: 'Story (1080×1920)', width: 1080, height: 1920 },
];
const CUSTOM_LABEL = 'Benutzerdefiniert';

function presetLabelFor(width: number, height: number): string {
  return PRESETS.find((p) => p.width === width && p.height === height)?.label ?? CUSTOM_LABEL;
}

function buildOutputFilename(
  input: File,
  width: number,
  height: number,
  format: 'jpeg' | 'png' = 'jpeg',
): string {
  const ext = format === 'png' ? 'png' : 'jpg';
  const lastDot = input.name.lastIndexOf('.');
  const base = lastDot > 0 ? input.name.slice(0, lastDot) : input.name;
  return `${base || 'featured'}-${width}x${height}.${ext}`;
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

export default function OgImagePage() {
  const [file, setFile] = useState<File | null>(null);
  const [sourceUrl, setSourceUrl] = useState<string | null>(null);
  const [offsets, setOffsets] = useState<CropOffsets>({ xOffset: 0.5, yOffset: 0.5 });
  const [targetWidth, setTargetWidth] = useState(1200);
  const [targetHeight, setTargetHeight] = useState(630);
  const [cropUrl, setCropUrl] = useState<string | null>(null);
  const [palette, setPalette] = useState<string[] | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isPngDownloading, setIsPngDownloading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const notify = useNotify();
  const [draftPreset, setDraftPreset] = useState(presetLabelFor(1200, 630));
  const [draftWidth, setDraftWidth] = useState('1200');
  const [draftHeight, setDraftHeight] = useState('630');
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (cropUrl) URL.revokeObjectURL(cropUrl);
    };
  }, [cropUrl]);

  useEffect(() => {
    return () => {
      if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    };
  }, [sourceUrl]);

  const runCrop = useCallback(
    async (incoming: File, next: CropOffsets, w: number, h: number) => {
      setIsProcessing(true);
      try {
        const blob = await cropOg(incoming, next.yOffset, {
          xOffset: next.xOffset,
          width: w,
          height: h,
        });
        setCropUrl((prev) => {
          if (prev) URL.revokeObjectURL(prev);
          return URL.createObjectURL(blob);
        });
      } catch (err) {
        notify.error(errorMessage(err));
      } finally {
        setIsProcessing(false);
      }
    },
    [notify],
  );

  const runPalette = useCallback(
    async (incoming: File) => {
      try {
        const colors = await extractPalette(incoming, 6);
        setPalette(colors);
      } catch (err) {
        notify.error(errorMessage(err));
      }
    },
    [notify],
  );

  const downloadPng = useCallback(async () => {
    if (!file) return;
    setIsPngDownloading(true);
    try {
      const blob = await cropOg(file, offsets.yOffset, {
        xOffset: offsets.xOffset,
        width: targetWidth,
        height: targetHeight,
        format: 'png',
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = buildOutputFilename(file, targetWidth, targetHeight, 'png');
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      // Defer revoke so the browser has time to start the download before the URL expires.
      // Revoking synchronously after click() causes a race on Firefox and Safari (0-byte file).
      setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (err) {
      notify.error(errorMessage(err));
    } finally {
      setIsPngDownloading(false);
    }
  }, [file, offsets, targetWidth, targetHeight, notify]);

  // Debounced re-crop whenever inputs change.
  useEffect(() => {
    if (!file) return;
    const handle = window.setTimeout(() => {
      void runCrop(file, offsets, targetWidth, targetHeight);
    }, CROP_DEBOUNCE_MS);
    return () => window.clearTimeout(handle);
  }, [file, offsets, targetWidth, targetHeight, runCrop]);

  const reset = () => {
    if (cropUrl) URL.revokeObjectURL(cropUrl);
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    setFile(null);
    setSourceUrl(null);
    setOffsets({ xOffset: 0.5, yOffset: 0.5 });
    setCropUrl(null);
    setPalette(null);
  };

  const acceptFile = (incoming: File) => {
    if (!ACCEPTED_TYPES.includes(incoming.type)) {
      notify.error(`Format nicht unterstützt: ${incoming.type || 'unbekannt'}`);
      return;
    }
    if (incoming.size > MAX_BYTES) {
      notify.error(`Datei zu groß (${(incoming.size / 1024 / 1024).toFixed(1)} MB, max 10 MB)`);
      return;
    }
    if (cropUrl) URL.revokeObjectURL(cropUrl);
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    setCropUrl(null);
    setPalette(null);
    setOffsets({ xOffset: 0.5, yOffset: 0.5 });
    setFile(incoming);
    setSourceUrl(URL.createObjectURL(incoming));
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
      notify.info(`${hex} kopiert`);
    } catch {
      notify.error('Konnte nicht in Zwischenablage kopieren');
    }
  };

  const openSettings = () => {
    setDraftPreset(presetLabelFor(targetWidth, targetHeight));
    setDraftWidth(String(targetWidth));
    setDraftHeight(String(targetHeight));
    setSettingsOpen(true);
  };

  const handlePresetChange = (label: string) => {
    setDraftPreset(label);
    const preset = PRESETS.find((p) => p.label === label);
    if (preset) {
      setDraftWidth(String(preset.width));
      setDraftHeight(String(preset.height));
    }
  };

  const handleCustomDimensionChange = (which: 'width' | 'height', value: string) => {
    if (which === 'width') setDraftWidth(value);
    else setDraftHeight(value);
    setDraftPreset(CUSTOM_LABEL);
  };

  const applySettings = () => {
    const w = Number.parseInt(draftWidth, 10);
    const h = Number.parseInt(draftHeight, 10);
    if (!Number.isFinite(w) || !Number.isFinite(h)) return;
    if (w < MIN_DIMENSION || w > MAX_DIMENSION) return;
    if (h < MIN_DIMENSION || h > MAX_DIMENSION) return;
    setTargetWidth(w);
    setTargetHeight(h);
    setSettingsOpen(false);
  };

  const draftWidthNum = Number.parseInt(draftWidth, 10);
  const draftHeightNum = Number.parseInt(draftHeight, 10);
  const draftValid =
    Number.isFinite(draftWidthNum) &&
    Number.isFinite(draftHeightNum) &&
    draftWidthNum >= MIN_DIMENSION &&
    draftWidthNum <= MAX_DIMENSION &&
    draftHeightNum >= MIN_DIMENSION &&
    draftHeightNum <= MAX_DIMENSION;

  return (
    <>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="h4">Beitragsbild</Typography>
        <Tooltip title="Zielgröße einstellen">
          <IconButton onClick={openSettings} aria-label="Einstellungen öffnen">
            <SettingsIcon />
          </IconButton>
        </Tooltip>
      </Stack>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Bild hochladen, Crop-Rahmen mit der Maus verschieben, JPEG oder PNG downloaden.
        Aktuelle Zielgröße: <strong>{targetWidth}×{targetHeight}</strong>.
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

      {file && sourceUrl && (
        <>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Ausschnitt ({targetWidth}×{targetHeight})
            </Typography>
            <InteractiveCropFrame
              imageUrl={sourceUrl}
              targetWidth={targetWidth}
              targetHeight={targetHeight}
              xOffset={offsets.xOffset}
              yOffset={offsets.yOffset}
              onChange={setOffsets}
            />
            <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
              <Button
                variant="outlined"
                startIcon={<RestartAltIcon />}
                onClick={reset}
                disabled={isProcessing}
              >
                Zurücksetzen
              </Button>
              {cropUrl && (
                <>
                  <Button
                    variant="contained"
                    startIcon={<DownloadIcon />}
                    component="a"
                    href={cropUrl}
                    download={buildOutputFilename(file, targetWidth, targetHeight, 'jpeg')}
                    sx={{ pointerEvents: isPngDownloading ? 'none' : 'auto' }}
                    aria-disabled={isPngDownloading}
                  >
                    JPEG herunterladen
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={isPngDownloading ? <CircularProgress size={18} /> : <DownloadIcon />}
                    onClick={() => void downloadPng()}
                    disabled={isPngDownloading}
                  >
                    PNG herunterladen
                  </Button>
                </>
              )}
            </Stack>
          </Paper>

          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Vorschau (Ergebnis)
            </Typography>
            {isProcessing && !cropUrl ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : cropUrl ? (
              <Box
                component="img"
                src={cropUrl}
                alt="Beitragsbild Vorschau"
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

      <Drawer
        anchor="right"
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 360 } } }}
      >
        {/* Spacer in AppBar-Hoehe, sonst klebt der Inhalt unter dem fixed Header. */}
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Zielgröße
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Preset wählen oder benutzerdefinierte Maße eintragen.
          </Typography>
          <Stack spacing={2}>
            <TextField
              select
              label="Preset"
              value={draftPreset}
              onChange={(e) => handlePresetChange(e.target.value)}
              fullWidth
            >
              {PRESETS.map((p) => (
                <MenuItem key={p.label} value={p.label}>
                  {p.label}
                </MenuItem>
              ))}
              <MenuItem value={CUSTOM_LABEL}>{CUSTOM_LABEL}</MenuItem>
            </TextField>
            <TextField
              label="Breite (px)"
              type="number"
              value={draftWidth}
              onChange={(e) => handleCustomDimensionChange('width', e.target.value)}
              inputProps={{ min: MIN_DIMENSION, max: MAX_DIMENSION }}
              fullWidth
            />
            <TextField
              label="Höhe (px)"
              type="number"
              value={draftHeight}
              onChange={(e) => handleCustomDimensionChange('height', e.target.value)}
              inputProps={{ min: MIN_DIMENSION, max: MAX_DIMENSION }}
              fullWidth
            />
            <Typography variant="caption" color="text.secondary">
              Erlaubt: {MIN_DIMENSION}–{MAX_DIMENSION} px je Achse.
            </Typography>
            <Divider />
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={() => setSettingsOpen(false)}>Abbrechen</Button>
              <Button
                variant="contained"
                onClick={applySettings}
                disabled={!draftValid}
              >
                Übernehmen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>

    </>
  );
}
