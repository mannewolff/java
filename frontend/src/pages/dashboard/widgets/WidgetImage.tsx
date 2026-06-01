import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
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
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
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
 * Bild-Widget (#183, Skeleton). Zeigt aktuell einen Platzhalter — Upload (#184) sowie
 * Resize- (#185) und Crop-Modus (#186) folgen. Erfüllt den Widget-Props-Vertrag und das
 * Config-Muster inkl. Darstellung-Abschnitt.
 */
export default function WidgetImage({
  widget,
  onChange,
  onDelete,
  readOnly = false,
}: Props): JSX.Element {
  const config = parseImageConfig(widget.config);
  const surface = widgetSurface(readOnly, config);
  const [open, setOpen] = useState(false);
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

  useEffect(() => {
    if (open) {
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
  }, [open, config.showBorder, config.backgroundColor]);

  function handleApply(): void {
    const next: ImageConfig = {
      ...config,
      showBorder: draftShowBorder,
      ...(draftBackgroundColor.trim() !== ''
        ? { backgroundColor: draftBackgroundColor.trim() }
        : {}),
    };
    if (draftBackgroundColor.trim() === '') {
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
