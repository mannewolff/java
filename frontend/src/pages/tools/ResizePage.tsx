import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
  MenuItem,
  Paper,
  Slider,
  Stack,
  Switch,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CollectionsIcon from '@mui/icons-material/Collections';
import CompressIcon from '@mui/icons-material/Compress';
import DownloadIcon from '@mui/icons-material/Download';
import LinkIcon from '@mui/icons-material/Link';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import SettingsIcon from '@mui/icons-material/Settings';
import { ApiError } from '../../api/client';
import { fetchImageFile } from '../../api/images';
import { resizeImage, type OutputFormat } from '../../api/resize';
import { useNotify } from '../../notify/NotifyProvider';
import ImageGallery from '../../components/ImageGallery';
import InteractiveResizeFrame from './components/InteractiveResizeFrame';

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];
const MAX_BYTES = 10 * 1024 * 1024;
const MIN_DIMENSION = 1;
const MAX_DIMENSION = 8192;

const FORMAT_LABELS: Record<OutputFormat, string> = {
  auto: 'Wie Original',
  png: 'PNG',
  jpeg: 'JPEG',
  webp: 'WEBP',
};

const EXTENSIONS: Record<string, string> = {
  'image/png': 'png',
  'image/jpeg': 'jpg',
  'image/webp': 'webp',
};

function buildOutputFilename(input: File, w: number, h: number, contentType: string): string {
  const lastDot = input.name.lastIndexOf('.');
  const base = lastDot > 0 ? input.name.slice(0, lastDot) : input.name;
  const ext = EXTENSIONS[contentType] ?? 'bin';
  return `${base || 'image'}-${w}x${h}.${ext}`;
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

type ImageSource = 'upload' | 'db';

export default function ResizePage() {
  const [source, setSource] = useState<ImageSource>('upload');
  const [selectedDbId, setSelectedDbId] = useState<number | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [sourceUrl, setSourceUrl] = useState<string | null>(null);
  const [natural, setNatural] = useState<{ w: number; h: number } | null>(null);
  const [targetW, setTargetW] = useState('');
  const [targetH, setTargetH] = useState('');
  const [aspectLocked, setAspectLocked] = useState(true);
  const [downsizeOnly, setDownsizeOnly] = useState(true);
  const [resultUrl, setResultUrl] = useState<string | null>(null);
  const [resultContentType, setResultContentType] = useState<string>('image/png');
  const [isProcessing, setIsProcessing] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const notify = useNotify();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [outputFormat, setOutputFormat] = useState<OutputFormat>('auto');
  const [quality, setQuality] = useState(90);
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

  const clampDownsize = (value: number, axis: 'w' | 'h'): number => {
    if (!downsizeOnly || !natural) return value;
    const max = axis === 'w' ? natural.w : natural.h;
    return Math.min(value, max);
  };

  const handleWidthChange = (raw: string) => {
    setTargetW(raw);
    if (!aspectLocked || !natural) return;
    const w = Number.parseInt(raw, 10);
    if (!Number.isFinite(w) || w <= 0) {
      setTargetH('');
      return;
    }
    const aspect = natural.w / natural.h;
    const h = Math.max(1, Math.round(w / aspect));
    setTargetH(String(h));
  };

  const handleHeightChange = (raw: string) => {
    setTargetH(raw);
    if (!aspectLocked || !natural) return;
    const h = Number.parseInt(raw, 10);
    if (!Number.isFinite(h) || h <= 0) {
      setTargetW('');
      return;
    }
    const aspect = natural.w / natural.h;
    const w = Math.max(1, Math.round(h * aspect));
    setTargetW(String(w));
  };

  const reset = () => {
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    if (resultUrl) URL.revokeObjectURL(resultUrl);
    setFile(null);
    setSourceUrl(null);
    setNatural(null);
    setTargetW('');
    setTargetH('');
    setResultUrl(null);
    setSelectedDbId(null);
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
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    if (resultUrl) URL.revokeObjectURL(resultUrl);
    setFile(incoming);
    setResultUrl(null);
    setNatural(null);
    setTargetW('');
    setTargetH('');
    setSourceUrl(URL.createObjectURL(incoming));
  };

  const selectFromDb = async (id: number) => {
    setSelectedDbId(id);
    try {
      const loaded = await fetchImageFile(id);
      if (sourceUrl) URL.revokeObjectURL(sourceUrl);
      if (resultUrl) URL.revokeObjectURL(resultUrl);
      setFile(loaded);
      setResultUrl(null);
      setNatural(null);
      setTargetW('');
      setTargetH('');
      setSourceUrl(URL.createObjectURL(loaded));
    } catch (err) {
      notify.error(errorMessage(err));
    }
  };

  const handleImageLoad = (event: ChangeEvent<HTMLImageElement>) => {
    const img = event.currentTarget as HTMLImageElement;
    if (img.naturalWidth > 0 && img.naturalHeight > 0) {
      setNatural({ w: img.naturalWidth, h: img.naturalHeight });
      // Pre-fill the target with the originals so the user can tweak from there.
      setTargetW(String(img.naturalWidth));
      setTargetH(String(img.naturalHeight));
    }
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
    let w = Number.parseInt(targetW, 10);
    let h = Number.parseInt(targetH, 10);
    if (!Number.isFinite(w) || !Number.isFinite(h) || w < 1 || h < 1) {
      notify.error('Bitte gültige Breite und Höhe eintragen.');
      return;
    }
    if (w > MAX_DIMENSION || h > MAX_DIMENSION) {
      notify.error(`Max ${MAX_DIMENSION} px je Achse.`);
      return;
    }
    w = clampDownsize(w, 'w');
    h = clampDownsize(h, 'h');
    setTargetW(String(w));
    setTargetH(String(h));

    setIsProcessing(true);
    try {
      const { blob, contentType } = await resizeImage(file, w, h, { outputFormat, quality });
      if (resultUrl) URL.revokeObjectURL(resultUrl);
      setResultUrl(URL.createObjectURL(blob));
      setResultContentType(contentType);
    } catch (err) {
      notify.error(errorMessage(err));
    } finally {
      setIsProcessing(false);
    }
  };

  const downloadFilename =
    file && natural ? buildOutputFilename(file, Number(targetW), Number(targetH), resultContentType) : 'resized';

  return (
    <>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="h4">Bild verkleinern</Typography>
        <Tooltip title="Format & Qualität einstellen">
          <IconButton onClick={() => setSettingsOpen(true)} aria-label="Einstellungen öffnen">
            <SettingsIcon />
          </IconButton>
        </Tooltip>
      </Stack>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Bild hochladen, neue Breite oder Höhe eintragen — die andere Seite folgt
        proportional. Standardmäßig wird nur verkleinert.
      </Typography>

      <ToggleButtonGroup
        value={source}
        exclusive
        onChange={(_, next: ImageSource | null) => {
          if (next) setSource(next);
        }}
        size="small"
        sx={{ mb: 2 }}
        aria-label="Bildquelle"
      >
        <ToggleButton value="upload" aria-label="Hochladen">
          <CloudUploadIcon sx={{ mr: 1 }} fontSize="small" />
          Hochladen
        </ToggleButton>
        <ToggleButton value="db" aria-label="Aus Datenbank laden">
          <CollectionsIcon sx={{ mr: 1 }} fontSize="small" />
          Aus Datenbank laden
        </ToggleButton>
      </ToggleButtonGroup>

      {source === 'upload' ? (
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
          <CompressIcon
            sx={{ fontSize: 32, color: 'text.secondary', mr: 1, verticalAlign: 'middle' }}
          />
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
      ) : (
        <Paper sx={{ p: 2, mb: 3 }}>
          <ImageGallery onSelect={(id) => void selectFromDb(id)} selectedId={selectedDbId} />
        </Paper>
      )}

      {/* Hidden image to read natural dimensions */}
      {sourceUrl && (
        <Box
          component="img"
          src={sourceUrl}
          alt=""
          onLoad={handleImageLoad as never}
          sx={{ display: 'none' }}
        />
      )}

      {file && natural && (
        <>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography gutterBottom>
              Original: <strong>{natural.w}×{natural.h}</strong> px
            </Typography>
            {sourceUrl &&
              Number.isFinite(Number.parseInt(targetW, 10)) &&
              Number.isFinite(Number.parseInt(targetH, 10)) && (
                <Box sx={{ mb: 2 }}>
                  <InteractiveResizeFrame
                    imageUrl={sourceUrl}
                    naturalWidth={natural.w}
                    naturalHeight={natural.h}
                    width={Number.parseInt(targetW, 10) || 1}
                    height={Number.parseInt(targetH, 10) || 1}
                    aspectLocked={aspectLocked}
                    onChange={({ width, height }) => {
                      setTargetW(String(width));
                      setTargetH(String(height));
                    }}
                  />
                </Box>
              )}
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
              <TextField
                label="Neue Breite (px)"
                type="number"
                value={targetW}
                onChange={(e) => handleWidthChange(e.target.value)}
                inputProps={{ min: MIN_DIMENSION, max: MAX_DIMENSION, 'aria-label': 'Neue Breite' }}
                sx={{ flex: 1 }}
              />
              <Tooltip title={aspectLocked ? 'Aspect Ratio gekoppelt' : 'Aspect Ratio entkoppelt'}>
                <IconButton
                  onClick={() => setAspectLocked((v) => !v)}
                  aria-label={aspectLocked ? 'Aspect Ratio entkoppeln' : 'Aspect Ratio koppeln'}
                  color={aspectLocked ? 'primary' : 'default'}
                >
                  {aspectLocked ? <LinkIcon /> : <LinkOffIcon />}
                </IconButton>
              </Tooltip>
              <TextField
                label="Neue Höhe (px)"
                type="number"
                value={targetH}
                onChange={(e) => handleHeightChange(e.target.value)}
                inputProps={{ min: MIN_DIMENSION, max: MAX_DIMENSION, 'aria-label': 'Neue Höhe' }}
                sx={{ flex: 1 }}
              />
            </Stack>
            <FormControlLabel
              control={
                <Switch
                  checked={downsizeOnly}
                  onChange={(e) => setDownsizeOnly(e.target.checked)}
                />
              }
              label="Nur verkleinern (Original-Maße als Obergrenze)"
            />
            <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
              <Button
                variant="contained"
                onClick={handleSubmit}
                disabled={isProcessing}
                startIcon={isProcessing ? <CircularProgress size={16} color="inherit" /> : <CompressIcon />}
              >
                Verkleinern
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
                Ergebnis: {targetW}×{targetH} px
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                Vorschau ist zur Sichtbarkeit auf die Spaltenbreite hochskaliert.
                Die heruntergeladene Datei hat die oben genannten Pixelmaße.
              </Typography>
              <Box
                component="img"
                src={resultUrl}
                alt="Verkleinertes Bild"
                sx={{
                  width: '100%',
                  maxWidth: 800,
                  display: 'block',
                  mx: 'auto',
                  borderRadius: 1,
                  border: 1,
                  borderColor: 'divider',
                  // Photos: glatt hochskalieren. Pixel-Art-Tools koennen das spaeter ueberschreiben.
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
            Output
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Format und (bei lossy Formaten) Qualität wählen.
          </Typography>
          <Stack spacing={2}>
            <TextField
              select
              label="Format"
              value={outputFormat}
              onChange={(e) => setOutputFormat(e.target.value as OutputFormat)}
              fullWidth
            >
              {(Object.keys(FORMAT_LABELS) as OutputFormat[]).map((k) => (
                <MenuItem key={k} value={k}>
                  {FORMAT_LABELS[k]}
                </MenuItem>
              ))}
            </TextField>
            <Box>
              <Typography gutterBottom>Qualität: {quality}</Typography>
              <Slider
                value={quality}
                min={50}
                max={95}
                step={1}
                onChange={(_, v) => setQuality(Array.isArray(v) ? v[0] : v)}
                disabled={outputFormat === 'auto' || outputFormat === 'png'}
                aria-label="Qualität"
              />
              <Typography variant="caption" color="text.secondary">
                Greift nur bei JPEG und WEBP.
              </Typography>
            </Box>
            <Divider />
            <Stack direction="row" justifyContent="flex-end">
              <Button variant="contained" onClick={() => setSettingsOpen(false)}>
                Schließen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>
    </>
  );
}
