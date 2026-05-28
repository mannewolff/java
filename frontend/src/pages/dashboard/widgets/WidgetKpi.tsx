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
  useTheme,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';

import type { WidgetDto } from '../../../api/dashboard';

type KpiColor = 'neutral' | 'success' | 'warning' | 'error';
type KpiStyle = 'number' | 'gauge';

const COLORS: ReadonlyArray<KpiColor> = ['neutral', 'success', 'warning', 'error'];
const STYLES: ReadonlyArray<{ value: KpiStyle; label: string }> = [
  { value: 'gauge', label: 'Gauge (Tacho)' },
  { value: 'number', label: 'Zahl (Number)' },
];

interface NumberConfig {
  style: 'number';
  value: number;
  label: string;
  /** Trend in percent. `null` = no trend shown. */
  trend: number | null;
  color: KpiColor;
}

interface GaugeConfig {
  style: 'gauge';
  value: number;
  label: string;
  min: number;
  max: number;
  lowEnd: number;
  mediumEnd: number;
  rangeLabel: string;
}

type KpiConfig = NumberConfig | GaugeConfig;

function isKpiColor(v: unknown): v is KpiColor {
  return typeof v === 'string' && (COLORS as readonly string[]).includes(v);
}

function isKpiStyle(v: unknown): v is KpiStyle {
  return v === 'gauge' || v === 'number';
}

const NUMBER_DEFAULTS = {
  value: 0,
  label: '',
  trend: null as number | null,
  color: 'neutral' as KpiColor,
};

const GAUGE_DEFAULTS = {
  value: 50,
  label: '',
  min: 0,
  max: 100,
  lowEnd: 33,
  mediumEnd: 66,
  rangeLabel: '',
};

function parseConfig(raw: string): KpiConfig {
  let parsed: Record<string, unknown> = {};
  try {
    parsed = JSON.parse(raw) as Record<string, unknown>;
  } catch {
    // fallback to number defaults
  }
  // Legacy: kein style-Feld -> "number" (rueckwaertskompatibel zu #42).
  const style: KpiStyle = isKpiStyle(parsed.style) ? parsed.style : 'number';
  if (style === 'gauge') {
    return {
      style: 'gauge',
      value: typeof parsed.value === 'number' ? parsed.value : GAUGE_DEFAULTS.value,
      label: typeof parsed.label === 'string' ? parsed.label : GAUGE_DEFAULTS.label,
      min: typeof parsed.min === 'number' ? parsed.min : GAUGE_DEFAULTS.min,
      max: typeof parsed.max === 'number' ? parsed.max : GAUGE_DEFAULTS.max,
      lowEnd: typeof parsed.lowEnd === 'number' ? parsed.lowEnd : GAUGE_DEFAULTS.lowEnd,
      mediumEnd:
        typeof parsed.mediumEnd === 'number' ? parsed.mediumEnd : GAUGE_DEFAULTS.mediumEnd,
      rangeLabel:
        typeof parsed.rangeLabel === 'string' ? parsed.rangeLabel : GAUGE_DEFAULTS.rangeLabel,
    };
  }
  return {
    style: 'number',
    value: typeof parsed.value === 'number' ? parsed.value : NUMBER_DEFAULTS.value,
    label: typeof parsed.label === 'string' ? parsed.label : NUMBER_DEFAULTS.label,
    trend: typeof parsed.trend === 'number' ? parsed.trend : NUMBER_DEFAULTS.trend,
    color: isKpiColor(parsed.color) ? parsed.color : NUMBER_DEFAULTS.color,
  };
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

/**
 * Polar-zu-kartesisch fuer ein SVG-Halbkreis-Gauge.
 *
 * @param cx Mittelpunkt X
 * @param cy Mittelpunkt Y (Basis-Linie)
 * @param radius Radius
 * @param angleDeg 0 = linker Rand, 180 = rechter Rand des Halbkreises
 */
function polar(cx: number, cy: number, radius: number, angleDeg: number) {
  const rad = ((angleDeg - 180) * Math.PI) / 180;
  return { x: cx + radius * Math.cos(rad), y: cy + radius * Math.sin(rad) };
}

function arcPath(
  cx: number,
  cy: number,
  radius: number,
  startAngle: number,
  endAngle: number,
): string {
  const start = polar(cx, cy, radius, startAngle);
  const end = polar(cx, cy, radius, endAngle);
  const largeArc = endAngle - startAngle <= 180 ? 0 : 1;
  return `M ${start.x} ${start.y} A ${radius} ${radius} 0 ${largeArc} 1 ${end.x} ${end.y}`;
}

interface Props {
  widget: WidgetDto;
  onChange: (next: WidgetDto) => void;
  onDelete: () => void;
  /** Read-Modus: keine Aktions-Icons, kein Drawer-Trigger. */
  readOnly?: boolean;
}

export default function WidgetKpi({
  widget,
  onChange,
  onDelete,
  readOnly = false,
}: Props): JSX.Element {
  const config = parseConfig(widget.config);
  const [open, setOpen] = useState(false);

  // Drawer-Drafts pro Sub-Type. Beim Style-Wechsel im Drawer bleiben die alten
  // Werte des jeweils anderen Style erhalten.
  const [draftStyle, setDraftStyle] = useState<KpiStyle>(config.style);
  const [draftLabel, setDraftLabel] = useState(config.label);

  // Number-Drafts
  const numberConfig: NumberConfig =
    config.style === 'number'
      ? config
      : { style: 'number', ...NUMBER_DEFAULTS };
  const [draftValue, setDraftValue] = useState(String(numberConfig.value));
  const [draftTrend, setDraftTrend] = useState<string>(
    numberConfig.trend == null ? '' : String(numberConfig.trend),
  );
  const [draftColor, setDraftColor] = useState<KpiColor>(numberConfig.color);

  // Gauge-Drafts
  const gaugeConfig: GaugeConfig =
    config.style === 'gauge'
      ? config
      : { style: 'gauge', ...GAUGE_DEFAULTS };
  const [draftGaugeValue, setDraftGaugeValue] = useState(String(gaugeConfig.value));
  const [draftMin, setDraftMin] = useState(String(gaugeConfig.min));
  const [draftMax, setDraftMax] = useState(String(gaugeConfig.max));
  const [draftLowEnd, setDraftLowEnd] = useState(String(gaugeConfig.lowEnd));
  const [draftMediumEnd, setDraftMediumEnd] = useState(String(gaugeConfig.mediumEnd));
  const [draftRangeLabel, setDraftRangeLabel] = useState(gaugeConfig.rangeLabel);

  useEffect(() => {
    if (!open) return;
    setDraftStyle(config.style);
    setDraftLabel(config.label);
    if (config.style === 'number') {
      setDraftValue(String(config.value));
      setDraftTrend(config.trend == null ? '' : String(config.trend));
      setDraftColor(config.color);
    } else {
      setDraftGaugeValue(String(config.value));
      setDraftMin(String(config.min));
      setDraftMax(String(config.max));
      setDraftLowEnd(String(config.lowEnd));
      setDraftMediumEnd(String(config.mediumEnd));
      setDraftRangeLabel(config.rangeLabel);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function handleApply(): void {
    let next: KpiConfig;
    if (draftStyle === 'number') {
      const valueNum = Number.parseFloat(draftValue);
      const trendNum = draftTrend.trim() === '' ? null : Number.parseFloat(draftTrend);
      next = {
        style: 'number',
        value: Number.isFinite(valueNum) ? valueNum : 0,
        label: draftLabel,
        trend: trendNum != null && Number.isFinite(trendNum) ? trendNum : null,
        color: draftColor,
      };
    } else {
      next = {
        style: 'gauge',
        value: parseOrDefault(draftGaugeValue, GAUGE_DEFAULTS.value),
        label: draftLabel,
        min: parseOrDefault(draftMin, GAUGE_DEFAULTS.min),
        max: parseOrDefault(draftMax, GAUGE_DEFAULTS.max),
        lowEnd: parseOrDefault(draftLowEnd, GAUGE_DEFAULTS.lowEnd),
        mediumEnd: parseOrDefault(draftMediumEnd, GAUGE_DEFAULTS.mediumEnd),
        rangeLabel: draftRangeLabel,
      };
    }
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  function handleCancel(): void {
    setOpen(false);
  }

  return (
    <Paper
      variant="outlined"
      sx={{
        height: '100%',
        position: 'relative',
        overflow: 'hidden',
        ...(config.style === 'number' && {
          borderLeftWidth: 4,
          borderLeftColor: accentBorderColor(config.color),
        }),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        p: 1,
      }}
    >
      {!readOnly && (
        <Stack direction="row" spacing={0.5} sx={{ position: 'absolute', top: 4, right: 4 }}>
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
      )}

      {config.style === 'number' ? <NumberView config={config} /> : <GaugeView config={config} />}

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
              select
              label="Darstellung"
              value={draftStyle}
              onChange={(e) => setDraftStyle(e.target.value as KpiStyle)}
              fullWidth
            >
              {STYLES.map((s) => (
                <MenuItem key={s.value} value={s.value}>
                  {s.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Label"
              value={draftLabel}
              onChange={(e) => setDraftLabel(e.target.value)}
              fullWidth
            />
            {draftStyle === 'number' && (
              <>
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
              </>
            )}
            {draftStyle === 'gauge' && (
              <>
                <TextField
                  label="Wert"
                  type="number"
                  value={draftGaugeValue}
                  onChange={(e) => setDraftGaugeValue(e.target.value)}
                  fullWidth
                  inputProps={{ step: 'any' }}
                />
                <Stack direction="row" spacing={1}>
                  <TextField
                    label="Min"
                    type="number"
                    value={draftMin}
                    onChange={(e) => setDraftMin(e.target.value)}
                    fullWidth
                    inputProps={{ step: 'any' }}
                  />
                  <TextField
                    label="Max"
                    type="number"
                    value={draftMax}
                    onChange={(e) => setDraftMax(e.target.value)}
                    fullWidth
                    inputProps={{ step: 'any' }}
                  />
                </Stack>
                <Stack direction="row" spacing={1}>
                  <TextField
                    label="Low-End"
                    type="number"
                    value={draftLowEnd}
                    onChange={(e) => setDraftLowEnd(e.target.value)}
                    fullWidth
                    inputProps={{ step: 'any' }}
                    helperText="rot / gelb"
                  />
                  <TextField
                    label="Medium-End"
                    type="number"
                    value={draftMediumEnd}
                    onChange={(e) => setDraftMediumEnd(e.target.value)}
                    fullWidth
                    inputProps={{ step: 'any' }}
                    helperText="gelb / grün"
                  />
                </Stack>
                <TextField
                  label="Range-Label (optional)"
                  value={draftRangeLabel}
                  onChange={(e) => setDraftRangeLabel(e.target.value)}
                  fullWidth
                  placeholder="z. B. 70% to 100%"
                />
              </>
            )}
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

function parseOrDefault(raw: string, fallback: number): number {
  const n = Number.parseFloat(raw);
  return Number.isFinite(n) ? n : fallback;
}

function NumberView({ config }: { config: NumberConfig }): JSX.Element {
  const trendPositive = config.trend != null && config.trend >= 0;
  return (
    <>
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
    </>
  );
}

function GaugeView({ config }: { config: GaugeConfig }): JSX.Element {
  const theme = useTheme();
  const cx = 100;
  const cy = 100;
  const radius = 80;
  const strokeWidth = 14;

  const clampedValue = Math.max(config.min, Math.min(config.max, config.value));
  const span = Math.max(1, config.max - config.min);
  const valueRatio = (clampedValue - config.min) / span;
  const needleAngle = valueRatio * 180;

  // Zonen-Grenzwinkel in Grad
  const lowRatio = Math.max(0, Math.min(1, (config.lowEnd - config.min) / span));
  const mediumRatio = Math.max(0, Math.min(1, (config.mediumEnd - config.min) / span));
  const lowAngle = lowRatio * 180;
  const mediumAngle = mediumRatio * 180;

  const needle = polar(cx, cy, radius - strokeWidth - 4, needleAngle);

  const percentDisplay = `${Math.round(valueRatio * 100)}%`;

  return (
    <Box
      sx={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column' }}
      aria-label="KPI-Gauge"
    >
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'center',
          minHeight: 0,
        }}
      >
        <svg viewBox="0 0 200 120" width="100%" height="100%" role="img" aria-label="Gauge">
          <path
            d={arcPath(cx, cy, radius, 0, lowAngle)}
            stroke={theme.palette.error.main}
            strokeWidth={strokeWidth}
            fill="none"
            strokeLinecap="butt"
          />
          <path
            d={arcPath(cx, cy, radius, lowAngle, mediumAngle)}
            stroke={theme.palette.warning.main}
            strokeWidth={strokeWidth}
            fill="none"
            strokeLinecap="butt"
          />
          <path
            d={arcPath(cx, cy, radius, mediumAngle, 180)}
            stroke={theme.palette.success.main}
            strokeWidth={strokeWidth}
            fill="none"
            strokeLinecap="butt"
          />
          <line
            x1={cx}
            y1={cy}
            x2={needle.x}
            y2={needle.y}
            stroke={theme.palette.text.primary}
            strokeWidth={2.5}
            strokeLinecap="round"
          />
          <circle cx={cx} cy={cy} r={4} fill={theme.palette.text.primary} />
          <text
            x={cx}
            y={cy - 10}
            textAnchor="middle"
            fontSize="22"
            fontWeight="700"
            fill={theme.palette.text.primary}
          >
            {percentDisplay}
          </text>
          {config.rangeLabel && (
            <text
              x={cx}
              y={cy + 14}
              textAnchor="middle"
              fontSize="10"
              fill={theme.palette.text.secondary}
            >
              {config.rangeLabel}
            </text>
          )}
        </svg>
      </Box>
      {config.label && (
        <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center', mb: 0.5 }}>
          {config.label}
        </Typography>
      )}
    </Box>
  );
}
