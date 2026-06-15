import { useEffect, useState } from 'react';
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
import {
  Box,
  Button,
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
  useTheme,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';

import type { WidgetDto } from '../../../api/dashboard';
import {
  getLatestEntry,
  getTimeSeries,
  listTimeSeries,
  type TimeSeriesEntry as TsEntry,
  type TimeSeriesSummary,
} from '../../../api/timeseries';
import { ApiError } from '../../../api/client';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

type KpiColor = 'neutral' | 'success' | 'warning' | 'error';
type KpiStyle = 'number' | 'gauge' | 'timeseries' | 'progress';
/** Was die Gauge mittig anzeigt: Prozent-Verhältnis oder den tatsächlichen Wert + Einheit. */
type GaugeDisplay = 'percent' | 'value';

const COLORS: ReadonlyArray<KpiColor> = ['neutral', 'success', 'warning', 'error'];
const STYLES: ReadonlyArray<{ value: KpiStyle; label: string }> = [
  { value: 'gauge', label: 'Gauge (Tacho)' },
  { value: 'progress', label: 'Gauge (Fortschritt)' },
  { value: 'number', label: 'Zahl (Number)' },
  { value: 'timeseries', label: 'Zeitreihe' },
];

const MIN_REFRESH_SECONDS = 5;
const MAX_REFRESH_SECONDS = 3600;
const DEFAULT_REFRESH_SECONDS = 30;

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
  /** #220: Farbzonen umkehren (Grün niedrig → Rot hoch) für "niedrig ist gut"-Metriken wie Gewicht. */
  invert: boolean;
  rangeLabel: string;
  /** Mittelanzeige: Prozent (Default) oder tatsächlicher Wert + Einheit. */
  display: GaugeDisplay;
  /** Einheit für den statischen Wert-Modus. Im Zeitreihen-Modus gewinnt die Serien-Einheit. */
  unit: string;
  /** Optional: aktuellen Wert aus dieser Zeitreihe laden. null/undefined = statischer Modus. */
  timeSeriesId?: number | null;
  /** Refresh-Intervall in Sekunden wenn timeSeriesId gesetzt. Default: 60. */
  refreshSeconds?: number;
}

interface ProgressConfig {
  style: 'progress';
  value: number;
  label: string;
  min: number;
  max: number;
  /** Farbe des Rings als Hex-String, z. B. '#4caf50'. */
  color: string;
  display: GaugeDisplay;
  unit: string;
  timeSeriesId?: number | null;
  refreshSeconds?: number;
}

interface TimeSeriesConfig {
  style: 'timeseries';
  timeSeriesId: number | null;
  refreshSeconds: number;
  label: string;
}

/** Lese-Modus-Darstellung — gilt fuer alle KPI-Sub-Typen. */
interface DisplayConfig {
  showBorder: boolean;
  backgroundColor?: string;
}

type KpiConfig = (NumberConfig | GaugeConfig | ProgressConfig | TimeSeriesConfig) & DisplayConfig;

function isKpiColor(v: unknown): v is KpiColor {
  return typeof v === 'string' && (COLORS as readonly string[]).includes(v);
}

function isKpiStyle(v: unknown): v is KpiStyle {
  return v === 'gauge' || v === 'number' || v === 'timeseries' || v === 'progress';
}

const NUMBER_DEFAULTS = {
  value: 0,
  label: '',
  trend: null as number | null,
  color: 'neutral' as KpiColor,
};

const PROGRESS_DEFAULTS = {
  value: 75,
  label: '',
  min: 0,
  max: 100,
  color: '#4caf50',
  display: 'percent' as GaugeDisplay,
  unit: '',
};

const GAUGE_DEFAULTS = {
  value: 50,
  label: '',
  min: 0,
  max: 100,
  lowEnd: 33,
  mediumEnd: 66,
  invert: false,
  rangeLabel: '',
  display: 'percent' as GaugeDisplay,
  unit: '',
};

function parseConfig(raw: string): KpiConfig {
  let parsed: Record<string, unknown> = {};
  try {
    parsed = JSON.parse(raw) as Record<string, unknown>;
  } catch {
    // fallback to number defaults
  }
  const display = parseSurfaceConfig(parsed);
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
      // Fehlendes invert-Feld → false (rückwärtskompatibel zu bestehenden Gauges).
      invert: typeof parsed.invert === 'boolean' ? parsed.invert : GAUGE_DEFAULTS.invert,
      rangeLabel:
        typeof parsed.rangeLabel === 'string' ? parsed.rangeLabel : GAUGE_DEFAULTS.rangeLabel,
      // Fehlendes display-Feld → 'percent' (rückwärtskompatibel zu bestehenden Gauges).
      display: parsed.display === 'value' ? 'value' : 'percent',
      unit: typeof parsed.unit === 'string' ? parsed.unit : GAUGE_DEFAULTS.unit,
      timeSeriesId:
        typeof parsed.timeSeriesId === 'number' ? parsed.timeSeriesId : null,
      refreshSeconds:
        typeof parsed.refreshSeconds === 'number'
          ? clampRefresh(parsed.refreshSeconds)
          : 60,
      ...display,
    };
  }
  if (style === 'progress') {
    return {
      style: 'progress',
      value: typeof parsed.value === 'number' ? parsed.value : PROGRESS_DEFAULTS.value,
      label: typeof parsed.label === 'string' ? parsed.label : PROGRESS_DEFAULTS.label,
      min: typeof parsed.min === 'number' ? parsed.min : PROGRESS_DEFAULTS.min,
      max: typeof parsed.max === 'number' ? parsed.max : PROGRESS_DEFAULTS.max,
      color: typeof parsed.color === 'string' && parsed.color !== '' ? parsed.color : PROGRESS_DEFAULTS.color,
      display: parsed.display === 'value' ? 'value' : 'percent',
      unit: typeof parsed.unit === 'string' ? parsed.unit : PROGRESS_DEFAULTS.unit,
      timeSeriesId: typeof parsed.timeSeriesId === 'number' ? parsed.timeSeriesId : null,
      refreshSeconds:
        typeof parsed.refreshSeconds === 'number' ? clampRefresh(parsed.refreshSeconds) : 60,
      ...display,
    };
  }
  if (style === 'timeseries') {
    return {
      style: 'timeseries',
      timeSeriesId: typeof parsed.timeSeriesId === 'number' ? parsed.timeSeriesId : null,
      refreshSeconds:
        typeof parsed.refreshSeconds === 'number'
          ? clampRefresh(parsed.refreshSeconds)
          : DEFAULT_REFRESH_SECONDS,
      label: typeof parsed.label === 'string' ? parsed.label : '',
      ...display,
    };
  }
  return {
    style: 'number',
    value: typeof parsed.value === 'number' ? parsed.value : NUMBER_DEFAULTS.value,
    label: typeof parsed.label === 'string' ? parsed.label : NUMBER_DEFAULTS.label,
    trend: typeof parsed.trend === 'number' ? parsed.trend : NUMBER_DEFAULTS.trend,
    color: isKpiColor(parsed.color) ? parsed.color : NUMBER_DEFAULTS.color,
    ...display,
  };
}

function clampRefresh(n: number): number {
  if (!Number.isFinite(n)) return DEFAULT_REFRESH_SECONDS;
  return Math.max(MIN_REFRESH_SECONDS, Math.min(MAX_REFRESH_SECONDS, Math.round(n)));
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
  const surface = widgetSurface(readOnly, config);
  const [open, setOpen] = useState(false);

  // Drawer-Drafts pro Sub-Type. Beim Style-Wechsel im Drawer bleiben die alten
  // Werte des jeweils anderen Style erhalten.
  const [draftStyle, setDraftStyle] = useState<KpiStyle>(config.style);
  const [draftLabel, setDraftLabel] = useState(config.label);
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

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
  const [draftInvert, setDraftInvert] = useState(gaugeConfig.invert);
  const [draftRangeLabel, setDraftRangeLabel] = useState(gaugeConfig.rangeLabel);
  const [draftGaugeSeriesId, setDraftGaugeSeriesId] = useState<string>(
    gaugeConfig.timeSeriesId == null ? '' : String(gaugeConfig.timeSeriesId),
  );
  const [draftGaugeRefresh, setDraftGaugeRefresh] = useState<string>(
    String(gaugeConfig.refreshSeconds ?? 60),
  );
  const [draftGaugeDisplay, setDraftGaugeDisplay] = useState<GaugeDisplay>(gaugeConfig.display);
  const [draftGaugeUnit, setDraftGaugeUnit] = useState<string>(gaugeConfig.unit);

  // Progress-Drafts
  const progressConfig: ProgressConfig =
    config.style === 'progress'
      ? config
      : { style: 'progress', ...PROGRESS_DEFAULTS };
  const [draftProgressValue, setDraftProgressValue] = useState(String(progressConfig.value));
  const [draftProgressMin, setDraftProgressMin] = useState(String(progressConfig.min));
  const [draftProgressMax, setDraftProgressMax] = useState(String(progressConfig.max));
  const [draftProgressColor, setDraftProgressColor] = useState(progressConfig.color);
  const [draftProgressDisplay, setDraftProgressDisplay] = useState<GaugeDisplay>(progressConfig.display);
  const [draftProgressUnit, setDraftProgressUnit] = useState(progressConfig.unit);
  const [draftProgressSeriesId, setDraftProgressSeriesId] = useState<string>(
    progressConfig.timeSeriesId == null ? '' : String(progressConfig.timeSeriesId),
  );
  const [draftProgressRefresh, setDraftProgressRefresh] = useState<string>(
    String(progressConfig.refreshSeconds ?? 60),
  );

  // TimeSeries-Drafts
  const tsConfig: TimeSeriesConfig =
    config.style === 'timeseries'
      ? config
      : {
          style: 'timeseries',
          timeSeriesId: null,
          refreshSeconds: DEFAULT_REFRESH_SECONDS,
          label: '',
        };
  const [draftSeriesId, setDraftSeriesId] = useState<string>(
    tsConfig.timeSeriesId == null ? '' : String(tsConfig.timeSeriesId),
  );
  const [draftRefresh, setDraftRefresh] = useState<string>(String(tsConfig.refreshSeconds));
  const [seriesList, setSeriesList] = useState<TimeSeriesSummary[] | null>(null);

  useEffect(() => {
    if (!open) return;
    setDraftStyle(config.style);
    setDraftLabel(config.label);
    setDraftShowBorder(config.showBorder);
    setDraftBackgroundColor(config.backgroundColor ?? '');
    if (config.style === 'number') {
      setDraftValue(String(config.value));
      setDraftTrend(config.trend == null ? '' : String(config.trend));
      setDraftColor(config.color);
    } else if (config.style === 'gauge') {
      setDraftGaugeValue(String(config.value));
      setDraftMin(String(config.min));
      setDraftMax(String(config.max));
      setDraftLowEnd(String(config.lowEnd));
      setDraftMediumEnd(String(config.mediumEnd));
      setDraftInvert(config.invert);
      setDraftRangeLabel(config.rangeLabel);
      setDraftGaugeSeriesId(config.timeSeriesId == null ? '' : String(config.timeSeriesId));
      setDraftGaugeRefresh(String(config.refreshSeconds ?? 60));
      setDraftGaugeDisplay(config.display);
      setDraftGaugeUnit(config.unit);
    } else if (config.style === 'progress') {
      setDraftProgressValue(String(config.value));
      setDraftProgressMin(String(config.min));
      setDraftProgressMax(String(config.max));
      setDraftProgressColor(config.color);
      setDraftProgressDisplay(config.display);
      setDraftProgressUnit(config.unit);
      setDraftProgressSeriesId(config.timeSeriesId == null ? '' : String(config.timeSeriesId));
      setDraftProgressRefresh(String(config.refreshSeconds ?? 60));
    } else {
      setDraftSeriesId(config.timeSeriesId == null ? '' : String(config.timeSeriesId));
      setDraftRefresh(String(config.refreshSeconds));
    }
    if (seriesList === null) {
      listTimeSeries()
        .then(setSeriesList)
        .catch(() => setSeriesList([]));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function handleApply(): void {
    const display: DisplayConfig = {
      showBorder: draftShowBorder,
      ...(draftBackgroundColor.trim() !== ''
        ? { backgroundColor: draftBackgroundColor.trim() }
        : {}),
    };
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
        ...display,
      };
    } else if (draftStyle === 'gauge') {
      next = {
        style: 'gauge',
        value: parseOrDefault(draftGaugeValue, GAUGE_DEFAULTS.value),
        label: draftLabel,
        min: parseOrDefault(draftMin, GAUGE_DEFAULTS.min),
        max: parseOrDefault(draftMax, GAUGE_DEFAULTS.max),
        lowEnd: parseOrDefault(draftLowEnd, GAUGE_DEFAULTS.lowEnd),
        mediumEnd: parseOrDefault(draftMediumEnd, GAUGE_DEFAULTS.mediumEnd),
        invert: draftInvert,
        rangeLabel: draftRangeLabel,
        display: draftGaugeDisplay,
        unit: draftGaugeUnit.trim(),
        timeSeriesId: draftGaugeSeriesId === '' ? null : Number.parseInt(draftGaugeSeriesId, 10),
        refreshSeconds: clampRefresh(parseOrDefault(draftGaugeRefresh, 60)),
        ...display,
      };
    } else if (draftStyle === 'progress') {
      next = {
        style: 'progress',
        value: parseOrDefault(draftProgressValue, PROGRESS_DEFAULTS.value),
        label: draftLabel,
        min: parseOrDefault(draftProgressMin, PROGRESS_DEFAULTS.min),
        max: parseOrDefault(draftProgressMax, PROGRESS_DEFAULTS.max),
        color: draftProgressColor.trim() !== '' ? draftProgressColor.trim() : PROGRESS_DEFAULTS.color,
        display: draftProgressDisplay,
        unit: draftProgressUnit.trim(),
        timeSeriesId: draftProgressSeriesId === '' ? null : Number.parseInt(draftProgressSeriesId, 10),
        refreshSeconds: clampRefresh(parseOrDefault(draftProgressRefresh, 60)),
        ...display,
      };
    } else {
      next = {
        style: 'timeseries',
        timeSeriesId: draftSeriesId === '' ? null : Number.parseInt(draftSeriesId, 10),
        refreshSeconds: clampRefresh(parseOrDefault(draftRefresh, DEFAULT_REFRESH_SECONDS)),
        label: draftLabel,
        ...display,
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
      variant={surface.variant}
      elevation={surface.elevation}
      sx={{
        height: '100%',
        position: 'relative',
        overflow: 'hidden',
        ...surface.sx,
        // Farb-Akzent-Linksrand nur im Edit-Modus oder wenn im Lese-Modus ein Rahmen aktiv ist.
        ...(config.style === 'number' &&
          (!readOnly || config.showBorder) && {
            borderLeftStyle: 'solid',
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

      {config.style === 'number' ? (
        <NumberView config={config} />
      ) : config.style === 'gauge' ? (
        <GaugeView config={config} />
      ) : config.style === 'progress' ? (
        <ProgressView config={config} />
      ) : (
        <TimeSeriesView config={config} />
      )}

      <Drawer
        anchor="right"
        open={open}
        onClose={handleCancel}
        PaperProps={{ sx: { width: CONFIG_DRAWER_WIDTH } }}
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
                <TextField
                  select
                  label="Mittelanzeige"
                  value={draftGaugeDisplay}
                  onChange={(e) => setDraftGaugeDisplay(e.target.value as GaugeDisplay)}
                  fullWidth
                >
                  <MenuItem value="percent">Prozent</MenuItem>
                  <MenuItem value="value">Wert</MenuItem>
                </TextField>
                {draftGaugeDisplay === 'value' && (
                  <TextField
                    label="Einheit"
                    value={draftGaugeUnit}
                    onChange={(e) => setDraftGaugeUnit(e.target.value)}
                    fullWidth
                    placeholder="z. B. kg"
                    helperText="Im Zeitreihen-Modus wird die Einheit der Serie verwendet."
                  />
                )}
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
                <FormControlLabel
                  control={
                    <Switch
                      checked={draftInvert}
                      onChange={(e) => setDraftInvert(e.target.checked)}
                    />
                  }
                  label="Niedrig ist gut (Farben umkehren)"
                />
                <TextField
                  label="Range-Label (optional)"
                  value={draftRangeLabel}
                  onChange={(e) => setDraftRangeLabel(e.target.value)}
                  fullWidth
                  placeholder="z. B. 70% to 100%"
                />
                <TextField
                  select
                  label="Zeitreihe (optional — überschreibt statischen Wert)"
                  value={draftGaugeSeriesId}
                  onChange={(e) => setDraftGaugeSeriesId(e.target.value)}
                  fullWidth
                >
                  <MenuItem value="">
                    <em>— statischer Wert —</em>
                  </MenuItem>
                  {(seriesList ?? []).map((ts) => (
                    <MenuItem key={ts.id} value={String(ts.id)}>
                      {ts.name} ({ts.unit})
                    </MenuItem>
                  ))}
                </TextField>
                {draftGaugeSeriesId !== '' && (
                  <TextField
                    label="Refresh-Intervall (Sekunden)"
                    type="number"
                    value={draftGaugeRefresh}
                    onChange={(e) => setDraftGaugeRefresh(e.target.value)}
                    fullWidth
                    inputProps={{ min: MIN_REFRESH_SECONDS, max: MAX_REFRESH_SECONDS, step: 1 }}
                    helperText={`${MIN_REFRESH_SECONDS} bis ${MAX_REFRESH_SECONDS} Sekunden`}
                  />
                )}
              </>
            )}
            {draftStyle === 'progress' && (
              <>
                <TextField
                  label="Wert"
                  type="number"
                  value={draftProgressValue}
                  onChange={(e) => setDraftProgressValue(e.target.value)}
                  fullWidth
                  inputProps={{ step: 'any' }}
                />
                <TextField
                  select
                  label="Mittelanzeige"
                  value={draftProgressDisplay}
                  onChange={(e) => setDraftProgressDisplay(e.target.value as GaugeDisplay)}
                  fullWidth
                >
                  <MenuItem value="percent">Prozent</MenuItem>
                  <MenuItem value="value">Wert</MenuItem>
                </TextField>
                {draftProgressDisplay === 'value' && (
                  <TextField
                    label="Einheit"
                    value={draftProgressUnit}
                    onChange={(e) => setDraftProgressUnit(e.target.value)}
                    fullWidth
                    placeholder="z. B. kg"
                    helperText="Im Zeitreihen-Modus wird die Einheit der Serie verwendet."
                  />
                )}
                <Stack direction="row" spacing={1}>
                  <TextField
                    label="Min"
                    type="number"
                    value={draftProgressMin}
                    onChange={(e) => setDraftProgressMin(e.target.value)}
                    fullWidth
                    inputProps={{ step: 'any' }}
                  />
                  <TextField
                    label="Max"
                    type="number"
                    value={draftProgressMax}
                    onChange={(e) => setDraftProgressMax(e.target.value)}
                    fullWidth
                    inputProps={{ step: 'any' }}
                  />
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Typography variant="body2" sx={{ flexShrink: 0 }}>
                    Farbe
                  </Typography>
                  <Box
                    component="input"
                    type="color"
                    value={draftProgressColor}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      setDraftProgressColor(e.target.value)
                    }
                    sx={{ width: 48, height: 36, border: 'none', cursor: 'pointer', p: 0 }}
                    aria-label="Ring-Farbe wählen"
                  />
                  <Typography variant="body2" color="text.secondary">
                    {draftProgressColor}
                  </Typography>
                </Stack>
                <TextField
                  select
                  label="Zeitreihe (optional — überschreibt statischen Wert)"
                  value={draftProgressSeriesId}
                  onChange={(e) => setDraftProgressSeriesId(e.target.value)}
                  fullWidth
                >
                  <MenuItem value="">
                    <em>— statischer Wert —</em>
                  </MenuItem>
                  {(seriesList ?? []).map((ts) => (
                    <MenuItem key={ts.id} value={String(ts.id)}>
                      {ts.name} ({ts.unit})
                    </MenuItem>
                  ))}
                </TextField>
                {draftProgressSeriesId !== '' && (
                  <TextField
                    label="Refresh-Intervall (Sekunden)"
                    type="number"
                    value={draftProgressRefresh}
                    onChange={(e) => setDraftProgressRefresh(e.target.value)}
                    fullWidth
                    inputProps={{ min: MIN_REFRESH_SECONDS, max: MAX_REFRESH_SECONDS, step: 1 }}
                    helperText={`${MIN_REFRESH_SECONDS} bis ${MAX_REFRESH_SECONDS} Sekunden`}
                  />
                )}
              </>
            )}
            {draftStyle === 'timeseries' && (
              <>
                <TextField
                  select
                  label="Zeitreihe"
                  value={draftSeriesId}
                  onChange={(e) => setDraftSeriesId(e.target.value)}
                  fullWidth
                >
                  <MenuItem value="">
                    <em>— bitte wählen —</em>
                  </MenuItem>
                  {(seriesList ?? []).map((ts) => (
                    <MenuItem key={ts.id} value={String(ts.id)}>
                      {ts.name} ({ts.unit})
                    </MenuItem>
                  ))}
                </TextField>
                <TextField
                  label="Refresh-Intervall (Sekunden)"
                  type="number"
                  value={draftRefresh}
                  onChange={(e) => setDraftRefresh(e.target.value)}
                  fullWidth
                  inputProps={{ min: MIN_REFRESH_SECONDS, max: MAX_REFRESH_SECONDS, step: 1 }}
                  helperText={`${MIN_REFRESH_SECONDS} bis ${MAX_REFRESH_SECONDS} Sekunden`}
                />
              </>
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

function formatGaugeValue(value: number, unit: string): string {
  const num = value.toLocaleString('de-DE', { maximumFractionDigits: 2 });
  return unit.trim() === '' ? num : `${num} ${unit.trim()}`;
}

function GaugeView({ config }: { config: GaugeConfig }): JSX.Element {
  // Dynamischer Modus: Wert aus Zeitreihe laden.
  const [dynamicValue, setDynamicValue] = useState<number | null>(null);
  const [tsLoading, setTsLoading] = useState(false);
  const [tsError, setTsError] = useState<string | null>(null);
  // Serien-Einheit für den Wert-Modus (z. B. "kg") — einmalig geladen.
  const [seriesUnit, setSeriesUnit] = useState<string | null>(null);

  useEffect(() => {
    if (config.timeSeriesId == null || config.display !== 'value') {
      setSeriesUnit(null);
      return;
    }
    const id = config.timeSeriesId;
    let cancelled = false;
    getTimeSeries(id)
      .then((s) => {
        if (!cancelled) setSeriesUnit(s.unit);
      })
      .catch(() => {
        if (!cancelled) setSeriesUnit(null);
      });
    return () => {
      cancelled = true;
    };
  }, [config.timeSeriesId, config.display]);

  useEffect(() => {
    if (config.timeSeriesId == null) {
      setDynamicValue(null);
      setTsError(null);
      return;
    }
    const id = config.timeSeriesId;
    const refreshMs = (config.refreshSeconds ?? 60) * 1000;
    let cancelled = false;

    async function tick(): Promise<void> {
      setTsLoading(true);
      try {
        const entry = await getLatestEntry(id);
        if (!cancelled) {
          setDynamicValue(entry.value);
          setTsError(null);
        }
      } catch (e) {
        if (!cancelled) {
          setTsError(e instanceof ApiError ? e.message : 'Fehler beim Laden');
        }
      } finally {
        if (!cancelled) setTsLoading(false);
      }
    }

    void tick();
    const handle = window.setInterval(() => void tick(), refreshMs);
    return () => {
      cancelled = true;
      window.clearInterval(handle);
    };
  }, [config.timeSeriesId, config.refreshSeconds]);

  const theme = useTheme();
  const cx = 100;
  const cy = 100;
  const radius = 80;
  const strokeWidth = 14;

  // Effektiver Wert: dynamisch wenn Zeitreihe konfiguriert, sonst statisch.
  const effectiveValue = config.timeSeriesId != null && dynamicValue != null
    ? dynamicValue
    : config.value;

  const clampedValue = Math.max(config.min, Math.min(config.max, effectiveValue));
  const span = Math.max(1, config.max - config.min);
  const valueRatio = (clampedValue - config.min) / span;
  const needleAngle = valueRatio * 180;

  // Zonen-Grenzwinkel in Grad.
  const lowRatio = Math.max(0, Math.min(1, (config.lowEnd - config.min) / span));
  const mediumRatio = Math.max(0, Math.min(1, (config.mediumEnd - config.min) / span));
  const lowAngle = lowRatio * 180;
  const mediumAngle = mediumRatio * 180;
  // #220: Bei invert sind die Farbzonen umgekehrt — Grün niedrig (links), Rot hoch
  // (rechts), z. B. für Gewicht ("niedrig ist gut"). Default: Rot niedrig, Grün hoch.
  const lowZoneColor = config.invert ? theme.palette.success.main : theme.palette.error.main;
  const highZoneColor = config.invert ? theme.palette.error.main : theme.palette.success.main;

  const needle = polar(cx, cy, radius - strokeWidth - 4, needleAngle);

  // Einheit im Wert-Modus: Zeitreihe → Serien-Einheit, sonst die konfigurierte Einheit.
  const effectiveUnit = config.timeSeriesId != null ? (seriesUnit ?? '') : config.unit;
  const centerDisplay =
    config.display === 'value'
      ? formatGaugeValue(effectiveValue, effectiveUnit)
      : `${Math.round(valueRatio * 100)}%`;
  // Wert+Einheit kann länger sein als "60%" → etwas kleinere Schrift, damit es mittig passt.
  const centerFontSize = config.display === 'value' ? 16 : 22;

  if (config.timeSeriesId != null && tsLoading && dynamicValue === null) {
    return (
      <Typography variant="caption" color="text.secondary" aria-label="KPI-Gauge-Loading">
        Lade…
      </Typography>
    );
  }

  if (tsError != null) {
    return (
      <Typography variant="caption" color="error" aria-label="KPI-Gauge-Error">
        {tsError}
      </Typography>
    );
  }

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
            stroke={lowZoneColor}
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
            stroke={highZoneColor}
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
            fontSize={centerFontSize}
            fontWeight="700"
            fill={theme.palette.text.primary}
          >
            {centerDisplay}
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

const PROGRESS_CIRCUMFERENCE = 2 * Math.PI * 50;

function ProgressView({ config }: { config: ProgressConfig }): JSX.Element {
  const [dynamicValue, setDynamicValue] = useState<number | null>(null);
  const [tsLoading, setTsLoading] = useState(false);
  const [tsError, setTsError] = useState<string | null>(null);
  const [seriesUnit, setSeriesUnit] = useState<string | null>(null);
  const theme = useTheme();

  useEffect(() => {
    if (config.timeSeriesId == null || config.display !== 'value') {
      setSeriesUnit(null);
      return;
    }
    const id = config.timeSeriesId;
    let cancelled = false;
    getTimeSeries(id)
      .then((s) => { if (!cancelled) setSeriesUnit(s.unit); })
      .catch(() => { if (!cancelled) setSeriesUnit(null); });
    return () => { cancelled = true; };
  }, [config.timeSeriesId, config.display]);

  useEffect(() => {
    if (config.timeSeriesId == null) {
      setDynamicValue(null);
      setTsError(null);
      return;
    }
    const id = config.timeSeriesId;
    const refreshMs = (config.refreshSeconds ?? 60) * 1000;
    let cancelled = false;

    async function tick(): Promise<void> {
      setTsLoading(true);
      try {
        const entry = await getLatestEntry(id);
        if (!cancelled) { setDynamicValue(entry.value); setTsError(null); }
      } catch (e) {
        if (!cancelled) setTsError(e instanceof ApiError ? e.message : 'Fehler beim Laden');
      } finally {
        if (!cancelled) setTsLoading(false);
      }
    }

    void tick();
    const handle = window.setInterval(() => void tick(), refreshMs);
    return () => { cancelled = true; window.clearInterval(handle); };
  }, [config.timeSeriesId, config.refreshSeconds]);

  if (config.timeSeriesId != null && tsLoading && dynamicValue === null) {
    return (
      <Typography variant="caption" color="text.secondary" aria-label="KPI-Progress-Loading">
        Lade…
      </Typography>
    );
  }
  if (tsError != null) {
    return (
      <Typography variant="caption" color="error" aria-label="KPI-Progress-Error">
        {tsError}
      </Typography>
    );
  }

  const effectiveValue =
    config.timeSeriesId != null && dynamicValue != null ? dynamicValue : config.value;
  const span = Math.max(1, config.max - config.min);
  const clamped = Math.max(config.min, Math.min(config.max, effectiveValue));
  const ratio = (clamped - config.min) / span;
  const dashOffset = PROGRESS_CIRCUMFERENCE * (1 - ratio);

  const effectiveUnit = config.timeSeriesId != null ? (seriesUnit ?? '') : config.unit;
  const centerText =
    config.display === 'value'
      ? formatGaugeValue(effectiveValue, effectiveUnit)
      : `${Math.round(ratio * 100)}%`;
  const centerFontSize = config.display === 'value' ? 14 : 18;

  return (
    <Box
      sx={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column' }}
      aria-label="KPI-Progress-Gauge"
    >
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 0 }}>
        <svg viewBox="0 0 120 120" width="100%" height="100%" role="img" aria-label="Fortschrittsanzeige">
          <circle
            cx={60}
            cy={60}
            r={50}
            fill="none"
            stroke={theme.palette.action.disabledBackground}
            strokeWidth={10}
          />
          <circle
            cx={60}
            cy={60}
            r={50}
            fill="none"
            stroke={config.color}
            strokeWidth={10}
            strokeLinecap="round"
            strokeDasharray={PROGRESS_CIRCUMFERENCE}
            strokeDashoffset={dashOffset}
            transform="rotate(-90 60 60)"
          />
          <text
            x={60}
            y={60}
            textAnchor="middle"
            dominantBaseline="central"
            fontSize={centerFontSize}
            fontWeight="700"
            fill={theme.palette.text.primary}
          >
            {centerText}
          </text>
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

type TsState =
  | { kind: 'loading' }
  | { kind: 'no-config' }
  | { kind: 'no-data' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; entry: TsEntry; summary: TimeSeriesSummary };

function TimeSeriesView({ config }: { config: TimeSeriesConfig }): JSX.Element {
  const [state, setState] = useState<TsState>(
    config.timeSeriesId == null ? { kind: 'no-config' } : { kind: 'loading' },
  );

  useEffect(() => {
    if (config.timeSeriesId == null) {
      setState({ kind: 'no-config' });
      return;
    }
    const id = config.timeSeriesId;
    let cancelled = false;

    async function tick(): Promise<void> {
      try {
        const [entry, summary] = await Promise.all([getLatestEntry(id), getTimeSeries(id)]);
        if (!cancelled) setState({ kind: 'ready', entry, summary });
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 404) {
          setState({ kind: 'no-data' });
        } else {
          setState({
            kind: 'error',
            message: e instanceof ApiError ? e.message : 'Fehler beim Laden',
          });
        }
      }
    }

    void tick();
    const handle = window.setInterval(() => void tick(), config.refreshSeconds * 1000);
    return () => {
      cancelled = true;
      window.clearInterval(handle);
    };
  }, [config.timeSeriesId, config.refreshSeconds]);

  if (state.kind === 'no-config') {
    return (
      <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center' }}>
        Bitte Zeitreihe wählen.
      </Typography>
    );
  }
  if (state.kind === 'loading') {
    return (
      <Typography variant="caption" color="text.secondary" aria-label="KPI-Loading">
        Lade…
      </Typography>
    );
  }
  if (state.kind === 'no-data') {
    return (
      <Typography variant="caption" color="text.secondary" aria-label="KPI-No-Data">
        Keine Daten
      </Typography>
    );
  }
  if (state.kind === 'error') {
    return (
      <Typography variant="caption" color="error" aria-label="KPI-Error">
        {state.message}
      </Typography>
    );
  }

  const label = config.label || state.summary.name;
  return (
    <>
      <Typography
        variant="h3"
        component="div"
        sx={{ fontWeight: 700, lineHeight: 1.1, textAlign: 'center' }}
        aria-label="KPI-Wert"
      >
        {state.entry.value}
        <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 0.5 }}>
          {state.summary.unit}
        </Typography>
      </Typography>
      {label && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, textAlign: 'center' }}>
          {label}
        </Typography>
      )}
    </>
  );
}
