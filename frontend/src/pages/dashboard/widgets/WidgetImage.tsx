import { useEffect, useRef, useState } from 'react';
import type { PointerEvent as ReactPointerEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  Drawer,
  FormControl,
  FormControlLabel,
  FormLabel,
  IconButton,
  MenuItem,
  Paper,
  Radio,
  RadioGroup,
  Slider,
  Stack,
  Switch,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import DownloadIcon from '@mui/icons-material/Download';
import EditIcon from '@mui/icons-material/Edit';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';

import type { WidgetDto } from '../../../api/dashboard';
import { fetchImageObjectUrl } from '../../../api/images';
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
import { cropSourceRect, exportFilename, exportSize } from './imageExport';
import ImageUploader from './ImageUploader';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

type ExportFormat = 'png' | 'jpeg';

export type ImageMode = 'crop' | 'resize';
export type ImageObjectFit = 'contain' | 'cover' | 'fill';

export interface ImageConfig {
  /** id eines im Image-Store (#182) gespeicherten Bildes; `null` = noch keins hochgeladen. */
  imageId: number | null;
  /** Anzeigemodus: skaliert (`resize`) oder per Pan verschiebbar (`crop`). */
  mode: ImageMode;
  /** object-fit für den Resize-Modus. */
  objectFit: ImageObjectFit;
  /** Pan-Offsets (0..1) für den Crop-Modus. */
  cropOffsetX: number;
  cropOffsetY: number;
  showBorder: boolean;
  backgroundColor?: string;
}

function parseMode(v: unknown): ImageMode {
  return v === 'crop' ? 'crop' : 'resize';
}

function parseObjectFit(v: unknown): ImageObjectFit {
  return v === 'cover' || v === 'fill' ? v : 'contain';
}

/** Klemmt einen Offset auf [0, 1]. */
export function clamp01(v: number): number {
  return Math.min(1, Math.max(0, v));
}

/**
 * Neuer Pan-Offset (0..1) nach einer Maus-Verschiebung um `deltaPx` in einem `containerPx` breiten
 * Container. Ein Zug nach rechts (positives delta) verschiebt den sichtbaren Ausschnitt nach links,
 * daher das Minus. Limits sind inhärent [0, 1] (objectPosition-Prozent) — resize-unabhängig (#186).
 */
export function panBy(current: number, deltaPx: number, containerPx: number): number {
  if (containerPx <= 0) return current;
  return clamp01(current - deltaPx / containerPx);
}

function parseOffset(v: unknown): number {
  return typeof v === 'number' && Number.isFinite(v) ? clamp01(v) : 0;
}

/** Defensiv parsen; fehlende/ungültige Felder fallen auf Defaults (rückwärtskompatibel). */
export function parseImageConfig(raw: string): ImageConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      imageId: typeof parsed.imageId === 'number' ? parsed.imageId : null,
      mode: parseMode(parsed.mode),
      objectFit: parseObjectFit(parsed.objectFit),
      cropOffsetX: parseOffset(parsed.cropOffsetX),
      cropOffsetY: parseOffset(parsed.cropOffsetY),
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return {
      imageId: null,
      mode: 'resize',
      objectFit: 'contain',
      cropOffsetX: 0,
      cropOffsetY: 0,
      showBorder: false,
    };
  }
}

interface Props {
  widget: WidgetDto;
  onChange: (next: WidgetDto) => void;
  onDelete: () => void;
  readOnly?: boolean;
}

/**
 * Bild-Widget. Lädt das Bild authentifiziert als Blob (#182/#184) und zeigt es an. Upload und
 * Entfernen laufen über den Bearbeiten-Drawer. Resize-Feinheiten (#185) und Crop-Modus (#186)
 * folgen; aktuell wird das Bild mit `objectFit` dargestellt.
 */
export default function WidgetImage({
  widget,
  onChange,
  onDelete,
  readOnly = false,
}: Props): JSX.Element {
  const config = parseImageConfig(widget.config);
  const surface = widgetSurface(readOnly, config);

  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [imageError, setImageError] = useState(false);

  // Live-Pan im Crop-Modus: lokal während des Ziehens, persistiert beim Loslassen.
  const [liveOffset, setLiveOffset] = useState({ x: config.cropOffsetX, y: config.cropOffsetY });
  const containerRef = useRef<HTMLDivElement | null>(null);
  const displayRef = useRef<HTMLDivElement | null>(null);
  const dragRef = useRef<{ x: number; y: number } | null>(null);

  // Geladenes Bild-Element + Naturgröße für den Canvas-Export (#192).
  const imageElRef = useRef<HTMLImageElement | null>(null);
  const [naturalSize, setNaturalSize] = useState<{ width: number; height: number } | null>(null);
  // Sichtbarer Bereich (Widget-Kachel) für die Crop-Export-Größe; beim Drawer-Öffnen gemessen.
  const [viewport, setViewport] = useState<{ width: number; height: number } | null>(null);
  const [draftFormat, setDraftFormat] = useState<ExportFormat>('png');
  const [draftQuality, setDraftQuality] = useState(90);

  const [open, setOpen] = useState(false);
  const [draftImageId, setDraftImageId] = useState<number | null>(config.imageId);
  const [draftMode, setDraftMode] = useState<ImageMode>(config.mode);
  const [draftObjectFit, setDraftObjectFit] = useState<ImageObjectFit>(config.objectFit);
  const [draftCropX, setDraftCropX] = useState(config.cropOffsetX);
  const [draftCropY, setDraftCropY] = useState(config.cropOffsetY);
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

  useEffect(() => {
    setLiveOffset({ x: config.cropOffsetX, y: config.cropOffsetY });
  }, [config.cropOffsetX, config.cropOffsetY]);

  function handlePointerDown(e: ReactPointerEvent<HTMLDivElement>): void {
    if (config.mode !== 'crop') return;
    e.stopPropagation(); // Drag-Guard gegen react-grid-layout
    dragRef.current = { x: e.clientX, y: e.clientY };
    e.currentTarget.setPointerCapture(e.pointerId);
  }

  function handlePointerMove(e: ReactPointerEvent<HTMLDivElement>): void {
    if (dragRef.current === null) return;
    const container = containerRef.current;
    if (container === null) return;
    const dx = e.clientX - dragRef.current.x;
    const dy = e.clientY - dragRef.current.y;
    dragRef.current = { x: e.clientX, y: e.clientY };
    setLiveOffset((o) => ({
      x: panBy(o.x, dx, container.clientWidth),
      y: panBy(o.y, dy, container.clientHeight),
    }));
  }

  function handlePointerUp(): void {
    if (dragRef.current === null) return;
    dragRef.current = null;
    onChange({
      ...widget,
      config: JSON.stringify({
        ...config,
        cropOffsetX: liveOffset.x,
        cropOffsetY: liveOffset.y,
      }),
    });
  }

  // Bild authentifiziert als Object-URL laden, wenn sich die imageId ändert; URL aufräumen.
  useEffect(() => {
    if (config.imageId == null) {
      setImageUrl(null);
      setImageError(false);
      return;
    }
    let revoked = false;
    let url: string | null = null;
    setImageError(false);
    setImageUrl(null);
    fetchImageObjectUrl(config.imageId)
      .then((objectUrl) => {
        if (revoked) {
          URL.revokeObjectURL(objectUrl);
          return;
        }
        url = objectUrl;
        setImageUrl(objectUrl);
      })
      .catch(() => {
        if (!revoked) setImageError(true);
      });
    return () => {
      revoked = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [config.imageId]);

  // Naturgröße + dekodiertes Bild-Element für den Export (#192) laden.
  useEffect(() => {
    if (imageUrl == null) {
      imageElRef.current = null;
      setNaturalSize(null);
      return;
    }
    const img = new Image();
    img.onload = () => {
      imageElRef.current = img;
      setNaturalSize({ width: img.naturalWidth, height: img.naturalHeight });
    };
    img.src = imageUrl;
  }, [imageUrl]);

  useEffect(() => {
    if (open) {
      const el = displayRef.current;
      if (el != null) setViewport({ width: el.clientWidth, height: el.clientHeight });
      setDraftImageId(config.imageId);
      setDraftMode(config.mode);
      setDraftObjectFit(config.objectFit);
      setDraftCropX(config.cropOffsetX);
      setDraftCropY(config.cropOffsetY);
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
  }, [
    open,
    config.imageId,
    config.mode,
    config.objectFit,
    config.cropOffsetX,
    config.cropOffsetY,
    config.showBorder,
    config.backgroundColor,
  ]);

  function handleApply(): void {
    const next: ImageConfig = {
      ...config,
      imageId: draftImageId,
      mode: draftMode,
      objectFit: draftObjectFit,
      cropOffsetX: draftCropX,
      cropOffsetY: draftCropY,
      showBorder: draftShowBorder,
    };
    if (draftBackgroundColor.trim() !== '') {
      next.backgroundColor = draftBackgroundColor.trim();
    } else {
      delete next.backgroundColor;
    }
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  // Tatsächliche Export-Größe aus Draft-Modus + Naturgröße + gemessenem Bereich (#192).
  const exportDim =
    naturalSize == null ? null : exportSize(draftMode, naturalSize, viewport ?? naturalSize);

  function handleDownload(): void {
    const img = imageElRef.current;
    if (img == null || naturalSize == null) return;
    const out = exportSize(draftMode, naturalSize, viewport ?? naturalSize);
    const canvas = document.createElement('canvas');
    canvas.width = out.width;
    canvas.height = out.height;
    const ctx = canvas.getContext('2d');
    if (ctx == null) return;
    if (draftMode === 'crop') {
      const src = cropSourceRect(naturalSize, out, draftCropX, draftCropY);
      ctx.drawImage(img, src.x, src.y, src.width, src.height, 0, 0, out.width, out.height);
    } else {
      // Resize-Export: ganzes Bild in Naturgröße.
      ctx.drawImage(img, 0, 0, out.width, out.height);
    }
    const mime = draftFormat === 'jpeg' ? 'image/jpeg' : 'image/png';
    const quality = draftFormat === 'jpeg' ? draftQuality / 100 : undefined;
    canvas.toBlob(
      (blob) => {
        if (blob == null) return;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = exportFilename(draftFormat, Date.now());
        a.click();
        URL.revokeObjectURL(url);
      },
      mime,
      quality,
    );
  }

  return (
    <Paper
      variant={surface.variant}
      elevation={surface.elevation}
      sx={{
        p: 1,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        position: 'relative',
        ...surface.sx,
      }}
    >
      {!readOnly && (
        <Stack
          direction="row"
          spacing={0.5}
          sx={{ position: 'absolute', top: 4, right: 4, zIndex: 1 }}
        >
          <IconButton
            size="small"
            aria-label="Bild bearbeiten"
            onClick={() => setOpen(true)}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            aria-label="Bild löschen"
            onClick={onDelete}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <DeleteOutlineIcon fontSize="small" />
          </IconButton>
        </Stack>
      )}

      <Box ref={displayRef} sx={{ flex: 1, minHeight: 0, display: 'flex', overflow: 'hidden' }}>
        {config.imageId == null ? (
          <Stack
            alignItems="center"
            justifyContent="center"
            spacing={1}
            sx={{ flex: 1, color: 'text.secondary', textAlign: 'center', px: 2 }}
          >
            <ImageOutlinedIcon fontSize="large" />
            <Typography variant="body2" color="text.secondary">
              Kein Bild — im Bearbeiten-Drawer hochladen
            </Typography>
          </Stack>
        ) : imageError ? (
          <Alert severity="error" sx={{ m: 'auto' }}>
            Bild konnte nicht geladen werden.
          </Alert>
        ) : imageUrl == null ? (
          <Stack alignItems="center" justifyContent="center" sx={{ flex: 1 }}>
            <CircularProgress size={24} aria-label="Bild wird geladen" />
          </Stack>
        ) : config.mode === 'crop' ? (
          <Box
            ref={containerRef}
            // Pan nur im Bearbeitungsmodus (!readOnly). mousedown stoppen, sonst startet
            // react-grid-layout einen Tile-Drag (RGL lauscht auf mousedown, nicht auf pointer).
            onMouseDown={readOnly ? undefined : (e) => e.stopPropagation()}
            onPointerDown={readOnly ? undefined : handlePointerDown}
            onPointerMove={readOnly ? undefined : handlePointerMove}
            onPointerUp={readOnly ? undefined : handlePointerUp}
            sx={{
              width: '100%',
              height: '100%',
              overflow: 'hidden',
              cursor: readOnly ? 'default' : 'grab',
              touchAction: readOnly ? 'auto' : 'none',
            }}
          >
            <Box
              component="img"
              src={imageUrl}
              alt="Widget-Bild"
              draggable={false}
              sx={{
                width: '100%',
                height: '100%',
                objectFit: 'none',
                objectPosition: `${liveOffset.x * 100}% ${liveOffset.y * 100}%`,
                display: 'block',
                userSelect: 'none',
              }}
            />
          </Box>
        ) : (
          <Box
            component="img"
            src={imageUrl}
            alt="Widget-Bild"
            sx={{ width: '100%', height: '100%', objectFit: config.objectFit, display: 'block' }}
          />
        )}
      </Box>

      <Drawer
        anchor="right"
        open={open}
        onClose={() => setOpen(false)}
        PaperProps={{ sx: { width: CONFIG_DRAWER_WIDTH } }}
      >
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Bild bearbeiten
          </Typography>
          <Stack spacing={2}>
            <ImageUploader
              label={draftImageId == null ? 'Bild hochladen' : 'Bild ersetzen'}
              onUploaded={(info) => setDraftImageId(info.id)}
            />
            {draftImageId != null && (
              <Button
                size="small"
                color="error"
                onClick={() => setDraftImageId(null)}
                sx={{ alignSelf: 'flex-start' }}
              >
                Bild entfernen
              </Button>
            )}
            <FormControl>
              <FormLabel id="image-mode-label">Anzeigemodus</FormLabel>
              <RadioGroup
                row
                aria-labelledby="image-mode-label"
                value={draftMode}
                onChange={(e) => setDraftMode(e.target.value as ImageMode)}
              >
                <FormControlLabel value="resize" control={<Radio />} label="Skalieren" />
                <FormControlLabel value="crop" control={<Radio />} label="Ausschnitt" />
              </RadioGroup>
            </FormControl>

            {draftMode === 'resize' ? (
              <TextField
                label="Anpassung"
                select
                value={draftObjectFit}
                onChange={(e) => setDraftObjectFit(e.target.value as ImageObjectFit)}
                helperText="Wie das Bild in die Kachel eingepasst wird."
              >
                <MenuItem value="contain">Einpassen (vollständig sichtbar)</MenuItem>
                <MenuItem value="cover">Füllen (Kachel ausfüllen, ggf. beschnitten)</MenuItem>
                <MenuItem value="fill">Strecken (verzerrt auf Kachelmaß)</MenuItem>
              </TextField>
            ) : (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Ausschnitt verschieben (oder das Bild im Widget direkt ziehen)
                </Typography>
                <Typography variant="body2" sx={{ mt: 1 }}>
                  Horizontal
                </Typography>
                <Slider
                  value={Math.round(draftCropX * 100)}
                  onChange={(_, v) => setDraftCropX((v as number) / 100)}
                  min={0}
                  max={100}
                  aria-label="Ausschnitt horizontal"
                />
                <Typography variant="body2">Vertikal</Typography>
                <Slider
                  value={Math.round(draftCropY * 100)}
                  onChange={(_, v) => setDraftCropY((v as number) / 100)}
                  min={0}
                  max={100}
                  aria-label="Ausschnitt vertikal"
                />
              </Box>
            )}

            {imageUrl != null && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Live-Vorschau
                </Typography>
                <Box
                  component="img"
                  src={imageUrl}
                  alt="Live-Vorschau"
                  sx={{
                    mt: 0.5,
                    width: '100%',
                    height: 140,
                    objectFit: draftMode === 'crop' ? 'none' : draftObjectFit,
                    objectPosition:
                      draftMode === 'crop'
                        ? `${draftCropX * 100}% ${draftCropY * 100}%`
                        : undefined,
                    display: 'block',
                    borderRadius: 1,
                    overflow: 'hidden',
                    border: (theme) => `1px solid ${theme.palette.divider}`,
                  }}
                />
              </Box>
            )}
            <Divider textAlign="left">Herunterladen</Divider>
            <FormControl>
              <FormLabel id="image-format-label">Format</FormLabel>
              <RadioGroup
                row
                aria-labelledby="image-format-label"
                value={draftFormat}
                onChange={(e) => setDraftFormat(e.target.value as ExportFormat)}
              >
                <FormControlLabel value="png" control={<Radio />} label="PNG" />
                <FormControlLabel value="jpeg" control={<Radio />} label="JPEG" />
              </RadioGroup>
            </FormControl>
            {draftFormat === 'jpeg' && (
              <Box>
                <Typography variant="body2">Qualität: {draftQuality}</Typography>
                <Slider
                  value={draftQuality}
                  onChange={(_, v) => setDraftQuality(v as number)}
                  min={50}
                  max={100}
                  aria-label="JPEG-Qualität"
                />
              </Box>
            )}
            <Typography variant="caption" color="text.secondary">
              Export-Größe: {exportDim ? `${exportDim.width}×${exportDim.height}px` : '–'}
            </Typography>
            <Button
              variant="outlined"
              startIcon={<DownloadIcon />}
              onClick={handleDownload}
              disabled={draftImageId == null || naturalSize == null}
              sx={{ alignSelf: 'flex-start' }}
            >
              Bild herunterladen
            </Button>
            <Divider textAlign="left">Darstellung</Divider>
            <FormControlLabel
              control={
                <Switch
                  checked={draftShowBorder}
                  onChange={(e) => setDraftShowBorder(e.target.checked)}
                />
              }
              label="Rahmen anzeigen"
            />
            <TextField
              label="Hintergrundfarbe (leer = transparent)"
              value={draftBackgroundColor}
              onChange={(e) => setDraftBackgroundColor(e.target.value)}
              fullWidth
              placeholder="z. B. #1e1e1e oder rgba(255,255,255,0.05)"
            />
            <Divider />
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={() => setOpen(false)}>Abbrechen</Button>
              <Button variant="contained" onClick={handleApply}>
                Übernehmen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>
    </Paper>
  );
}
