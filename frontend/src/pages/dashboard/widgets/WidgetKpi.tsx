import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';

import type { WidgetDto } from '../../../api/dashboard';

type KpiColor = 'neutral' | 'success' | 'warning' | 'error';

const COLORS: ReadonlyArray<KpiColor> = ['neutral', 'success', 'warning', 'error'];

interface KpiConfig {
  value: number;
  label: string;
  /** Trend in percent. `null` = no trend shown. */
  trend: number | null;
  color: KpiColor;
}

function isKpiColor(v: unknown): v is KpiColor {
  return typeof v === 'string' && (COLORS as readonly string[]).includes(v);
}

function parseConfig(raw: string): KpiConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      value: typeof parsed.value === 'number' ? parsed.value : 0,
      label: typeof parsed.label === 'string' ? parsed.label : '',
      trend: typeof parsed.trend === 'number' ? parsed.trend : null,
      color: isKpiColor(parsed.color) ? parsed.color : 'neutral',
    };
  } catch {
    return { value: 0, label: '', trend: null, color: 'neutral' };
  }
}

/** MUI palette key for the color accent. `neutral` maps to a grey-ish divider color. */
function accentBorderColor(color: KpiColor): string {
  switch (color) {
    case 'success':
      return 'success.main';
    case 'warning':
      return 'warning.main';
    case 'error':
      return 'error.main';
    case 'neutral':
    default:
      return 'divider';
  }
}

interface Props {
  widget: WidgetDto;
  onChange: (next: WidgetDto) => void;
  onDelete: () => void;
}

export default function WidgetKpi({ widget, onChange, onDelete }: Props): JSX.Element {
  const config = parseConfig(widget.config);
  const [open, setOpen] = useState(false);

  // Drawer-Drafts als Strings, damit "leeres Feld" sauber abbildbar bleibt
  // (TextField type=number leert sich beim Tippen sonst seltsam).
  const [draftValue, setDraftValue] = useState(String(config.value));
  const [draftLabel, setDraftLabel] = useState(config.label);
  const [draftTrend, setDraftTrend] = useState<string>(
    config.trend == null ? '' : String(config.trend),
  );
  const [draftColor, setDraftColor] = useState<KpiColor>(config.color);

  useEffect(() => {
    if (open) {
      setDraftValue(String(config.value));
      setDraftLabel(config.label);
      setDraftTrend(config.trend == null ? '' : String(config.trend));
      setDraftColor(config.color);
    }
  }, [open, config.value, config.label, config.trend, config.color]);

  function handleApply(): void {
    const valueNum = Number.parseFloat(draftValue);
    const trendNum = draftTrend.trim() === '' ? null : Number.parseFloat(draftTrend);
    const next: KpiConfig = {
      value: Number.isFinite(valueNum) ? valueNum : 0,
      label: draftLabel,
      trend: trendNum != null && Number.isFinite(trendNum) ? trendNum : null,
      color: draftColor,
    };
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  function handleCancel(): void {
    setOpen(false);
  }

  const trendPositive = config.trend != null && config.trend >= 0;

  return (
    <Paper
      variant="outlined"
      sx={{
        height: '100%',
        position: 'relative',
        overflow: 'hidden',
        borderLeftWidth: 4,
        borderLeftColor: accentBorderColor(config.color),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        p: 1,
      }}
    >
      <Stack
        direction="row"
        spacing={0.5}
        sx={{ position: 'absolute', top: 4, right: 4 }}
      >
        <IconButton
          size="small"
          aria-label="KPI bearbeiten"
          onClick={() => setOpen(true)}
          onMouseDown={(e) => e.stopPropagation()}
        >
          <EditIcon fontSize="small" />
        </IconButton>
        <IconButton
          size="small"
          aria-label="KPI löschen"
          onClick={onDelete}
          onMouseDown={(e) => e.stopPropagation()}
        >
          <DeleteOutlineIcon fontSize="small" />
        </IconButton>
      </Stack>

      <Typography
        variant="h3"
        component="div"
        sx={{ fontWeight: 700, lineHeight: 1.1, textAlign: 'center' }}
        aria-label="KPI-Wert"
      >
        {config.value}
      </Typography>

      {config.label && (
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ mt: 0.5, textAlign: 'center' }}
        >
          {config.label}
        </Typography>
      )}

      {config.trend != null && (
        <Stack
          direction="row"
          spacing={0.25}
          alignItems="center"
          sx={{
            mt: 0.5,
            color: trendPositive ? 'success.main' : 'error.main',
          }}
          aria-label="KPI-Trend"
        >
          {trendPositive ? (
            <ArrowUpwardIcon fontSize="inherit" />
          ) : (
            <ArrowDownwardIcon fontSize="inherit" />
          )}
          <Typography variant="caption" sx={{ fontWeight: 600 }}>
            {Math.abs(config.trend)}%
          </Typography>
        </Stack>
      )}

      <Drawer
        anchor="right"
        open={open}
        onClose={handleCancel}
        PaperProps={{ sx: { width: { xs: '100%', sm: 400 } } }}
      >
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            KPI bearbeiten
          </Typography>
          <Stack spacing={2}>
            <TextField
              label="Label"
              value={draftLabel}
              onChange={(e) => setDraftLabel(e.target.value)}
              fullWidth
            />
            <TextField
              label="Wert"
              type="number"
              value={draftValue}
              onChange={(e) => setDraftValue(e.target.value)}
              fullWidth
              inputProps={{ step: 'any' }}
            />
            <TextField
              label="Trend in % (leer = kein Trend)"
              type="number"
              value={draftTrend}
              onChange={(e) => setDraftTrend(e.target.value)}
              fullWidth
              inputProps={{ step: 'any' }}
              helperText="Positive Werte zeigen einen grünen Aufwärtspfeil, negative einen roten Abwärtspfeil."
            />
            <TextField
              select
              label="Farb-Akzent"
              value={draftColor}
              onChange={(e) => setDraftColor(e.target.value as KpiColor)}
              fullWidth
            >
              {COLORS.map((c) => (
                <MenuItem key={c} value={c}>
                  {c}
                </MenuItem>
              ))}
            </TextField>
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
