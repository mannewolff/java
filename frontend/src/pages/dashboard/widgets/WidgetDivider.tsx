import { useEffect, useState } from 'react';
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
import {
  Box,
  Button,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Switch,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import FormControl from '@mui/material/FormControl';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';

import type { WidgetDto } from '../../../api/dashboard';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

type Orientation = 'horizontal' | 'vertical';

interface DividerConfig {
  orientation: Orientation;
  /** CSS-Farbwert; leer = Theme-Standard (`divider`). */
  color: string;
  /** Linienbreite in Pixeln. */
  thickness: number;
  showBorder: boolean;
  backgroundColor?: string;
}

const DEFAULT_THICKNESS = 2;

function parseConfig(raw: string): DividerConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const orientation: Orientation = parsed.orientation === 'vertical' ? 'vertical' : 'horizontal';
    const thickness =
      typeof parsed.thickness === 'number' && parsed.thickness > 0
        ? parsed.thickness
        : DEFAULT_THICKNESS;
    return {
      orientation,
      color: typeof parsed.color === 'string' ? parsed.color : '',
      thickness,
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return { orientation: 'horizontal', color: '', thickness: DEFAULT_THICKNESS, showBorder: false };
  }
}

interface Props {
  widget: WidgetDto;
  onChange: (next: WidgetDto) => void;
  onDelete: () => void;
  /** Read-Modus: nur die Linie, keine Aktions-Icons. Default `false` (Edit). */
  readOnly?: boolean;
}

/**
 * Trennlinien-Widget. Zeichnet eine horizontale oder vertikale Linie in der Kachel.
 * Die Stop-Propagation auf `onMouseDown` der Aktions-Buttons verhindert, dass
 * react-grid-layout den Klick als Drag-Start interpretiert.
 */
export default function WidgetDivider({
  widget,
  onChange,
  onDelete,
  readOnly = false,
}: Props): JSX.Element {
  const config = parseConfig(widget.config);
  const surface = widgetSurface(readOnly, config);
  const isHorizontal = config.orientation === 'horizontal';
  const [open, setOpen] = useState(false);
  const [draftOrientation, setDraftOrientation] = useState<Orientation>(config.orientation);
  const [draftColor, setDraftColor] = useState(config.color);
  const [draftThickness, setDraftThickness] = useState(String(config.thickness));
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

  useEffect(() => {
    if (open) {
      setDraftOrientation(config.orientation);
      setDraftColor(config.color);
      setDraftThickness(String(config.thickness));
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
  }, [
    open,
    config.orientation,
    config.color,
    config.thickness,
    config.showBorder,
    config.backgroundColor,
  ]);

  function handleApply(): void {
    const parsedThickness = Number.parseInt(draftThickness, 10);
    const next: DividerConfig = {
      orientation: draftOrientation,
      color: draftColor.trim(),
      thickness: Number.isFinite(parsedThickness) && parsedThickness > 0 ? parsedThickness : DEFAULT_THICKNESS,
      showBorder: draftShowBorder,
      ...(draftBackgroundColor.trim() !== ''
        ? { backgroundColor: draftBackgroundColor.trim() }
        : {}),
    };
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  function handleCancel(): void {
    setOpen(false);
  }

  return (
    <Paper
      variant={surface.variant}
      elevation={surface.elevation}
      sx={{
        height: '100%',
        width: '100%',
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        px: isHorizontal ? 1 : 0,
        py: isHorizontal ? 0 : 1,
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
            aria-label="Trennlinie bearbeiten"
            onClick={() => setOpen(true)}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            aria-label="Trennlinie löschen"
            onClick={onDelete}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <DeleteOutlineIcon fontSize="small" />
          </IconButton>
        </Stack>
      )}

      <Box
        aria-label="Trennlinie"
        sx={(theme) => {
          const c = config.color.trim() !== '' ? config.color : theme.palette.divider;
          return isHorizontal
            ? { width: '100%', borderTop: `${config.thickness}px solid ${c}` }
            : { height: '100%', borderLeft: `${config.thickness}px solid ${c}` };
        }}
      />

      <Drawer
        anchor="right"
        open={open}
        onClose={handleCancel}
        PaperProps={{ sx: { width: CONFIG_DRAWER_WIDTH } }}
      >
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Trennlinie bearbeiten
          </Typography>
          <Stack spacing={2}>
            <FormControl fullWidth>
              <InputLabel id="divider-orientation-label">Orientierung</InputLabel>
              <Select
                labelId="divider-orientation-label"
                label="Orientierung"
                value={draftOrientation}
                onChange={(e) => setDraftOrientation(e.target.value as Orientation)}
              >
                <MenuItem value="horizontal">Horizontal</MenuItem>
                <MenuItem value="vertical">Vertikal</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Linienfarbe (leer = Theme-Standard)"
              value={draftColor}
              onChange={(e) => setDraftColor(e.target.value)}
              fullWidth
              placeholder="z. B. #cccccc oder rgba(255,255,255,0.2)"
            />
            <TextField
              label="Linienbreite (px)"
              type="number"
              value={draftThickness}
              onChange={(e) => setDraftThickness(e.target.value)}
              fullWidth
              inputProps={{ min: 1, max: 40, 'aria-label': 'Linienbreite (px)' }}
            />
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
              <Button onClick={handleCancel}>Abbrechen</Button>
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
