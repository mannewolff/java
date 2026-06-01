import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Switch,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';

import type { WidgetDto } from '../../../api/dashboard';
import { fetchImageObjectUrl } from '../../../api/images';
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
import ImageUploader from './ImageUploader';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

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

function parseOffset(v: unknown): number {
  return typeof v === 'number' && Number.isFinite(v) ? Math.min(1, Math.max(0, v)) : 0;
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

  const [open, setOpen] = useState(false);
  const [draftImageId, setDraftImageId] = useState<number | null>(config.imageId);
  const [draftObjectFit, setDraftObjectFit] = useState<ImageObjectFit>(config.objectFit);
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

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

  useEffect(() => {
    if (open) {
      setDraftImageId(config.imageId);
      setDraftObjectFit(config.objectFit);
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
  }, [open, config.imageId, config.objectFit, config.showBorder, config.backgroundColor]);

  function handleApply(): void {
    const next: ImageConfig = {
      ...config,
      imageId: draftImageId,
      objectFit: draftObjectFit,
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

      <Box sx={{ flex: 1, minHeight: 0, display: 'flex', overflow: 'hidden' }}>
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
                    objectFit: draftObjectFit,
                    display: 'block',
                    borderRadius: 1,
                    border: (theme) => `1px solid ${theme.palette.divider}`,
                  }}
                />
              </Box>
            )}
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
