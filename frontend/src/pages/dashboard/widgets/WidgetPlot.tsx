import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  Divider,
  Drawer,
  FormControlLabel,
  FormGroup,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Switch,
  Tab,
  Tabs,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import type { WidgetDto } from '../../../api/dashboard';
import {
  aggregateTimeSeries,
  listEntries,
  listTimeSeries,
  type Granularity,
  type TimeSeriesSummary,
} from '../../../api/timeseries';
import { ApiError } from '../../../api/client';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

export type PlotOverlay = 'mean' | 'median' | 'min' | 'max';

const GRANULARITIES: ReadonlyArray<{ value: Granularity; label: string }> = [
  { value: 'DAILY', label: 'Täglich' },
  { value: 'WEEKLY', label: 'Wöchentlich' },
  { value: 'MONTHLY', label: 'Monatlich' },
  { value: 'YEARLY', label: 'Jährlich' },
];

const OVERLAY_META: Record<PlotOverlay, { label: string; prefix: string; color: string }> = {
  mean: { label: 'Mittelwert', prefix: 'Ø', color: '#d32f2f' },
  median: { label: 'Median', prefix: 'Md', color: '#7b1fa2' },
  min: { label: 'Minimum', prefix: 'Min', color: '#2e7d32' },
  max: { label: 'Maximum', prefix: 'Max', color: '#ed6c02' },
};

const OVERLAY_ORDER: readonly PlotOverlay[] = ['mean', 'median', 'min', 'max'];

interface PlotConfig {
  timeSeriesId: number | null;
  /** `null` = Rohwerte-Modus (neuer Default), sonst aggregierter Modus mit Tabs. */
  defaultGranularity: Granularity | null;
  overlays: PlotOverlay[];
  /** Lineare Trend-/Regressionslinie mit Zukunfts-Extrapolation (nur aggregierter Modus). */
  regression: boolean;
  showBorder: boolean;
  backgroundColor?: string;
}

interface ChartPoint {
  label: string;
  value: number;
  /** Bucket-Start (ISO) im aggregierten Modus — Basis für Zukunfts-Labels der Regression. */
  iso?: string;
}

/** Render-Datum: Zukunftspunkte haben `value: null`, alle Punkte ggf. einen Regressionswert. */
interface PlotDatum {
  label: string;
  value: number | null;
  regression?: number;
}

function isGranularity(v: unknown): v is Granularity {
  return v === 'DAILY' || v === 'WEEKLY' || v === 'MONTHLY' || v === 'YEARLY';
}

function parseGranularity(value: unknown): Granularity | null {
  if (isGranularity(value)) return value;
  // Feld fehlt komplett oder explizit null → Rohwerte-Modus.
  if (value === undefined || value === null) return null;
  // Feld gesetzt, aber kein gültiger Wert → alter Default als Fallback.
  return 'DAILY';
}

function parseOverlays(value: unknown): PlotOverlay[] {
  if (!Array.isArray(value)) return [];
  return OVERLAY_ORDER.filter((o) => value.includes(o));
}

function parseConfig(raw: string): PlotConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      timeSeriesId: typeof parsed.timeSeriesId === 'number' ? parsed.timeSeriesId : null,
      defaultGranularity: parseGranularity(parsed.defaultGranularity),
      overlays: parseOverlays(parsed.overlays),
      regression: typeof parsed.regression === 'boolean' ? parsed.regression : false,
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return {
      timeSeriesId: null,
      defaultGranularity: null,
      overlays: [],
      regression: false,
      showBorder: false,
    };
  }
}

function formatBucketLabel(iso: string, granularity: Granularity): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  switch (granularity) {
    case 'DAILY':
      return d.toLocaleDateString();
    case 'WEEKLY':
      return `KW ${weekNumber(d)}`;
    case 'MONTHLY':
      return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short' });
    case 'YEARLY':
      return String(d.getFullYear());
  }
}

function formatEntryLabel(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString();
}

function weekNumber(d: Date): number {
  const target = new Date(d.valueOf());
  const dayNr = (d.getUTCDay() + 6) % 7;
  target.setUTCDate(target.getUTCDate() - dayNr + 3);
  const firstThursday = target.valueOf();
  target.setUTCMonth(0, 1);
  if (target.getUTCDay() !== 4) {
    target.setUTCMonth(0, 1 + ((4 - target.getUTCDay() + 7) % 7));
  }
  return 1 + Math.ceil((firstThursday - target.valueOf()) / 604_800_000);
}

function median(values: number[]): number {
  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 === 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid];
}

export function overlayValue(overlay: PlotOverlay, values: number[]): number {
  switch (overlay) {
    case 'mean':
      return values.reduce((sum, v) => sum + v, 0) / values.length;
    case 'median':
      return median(values);
    case 'min':
      return Math.min(...values);
    case 'max':
      return Math.max(...values);
  }
}

function formatOverlayNumber(value: number): string {
  return value.toLocaleString('de-DE', { maximumFractionDigits: 1 });
}

export interface LinearFit {
  slope: number;
  intercept: number;
}

/** Least-Squares-Trendgerade über Index x = 0..n-1 gegen y = values. `n < 2` → `null`. */
export function linearRegression(values: number[]): LinearFit | null {
  const n = values.length;
  if (n < 2) return null;
  let sumX = 0;
  let sumY = 0;
  let sumXy = 0;
  let sumXx = 0;
  for (let i = 0; i < n; i++) {
    sumX += i;
    sumY += values[i];
    sumXy += i * values[i];
    sumXx += i * i;
  }
  // Nenner ist für n >= 2 mit ganzzahligen, paarweise verschiedenen x niemals 0.
  const denom = n * sumXx - sumX * sumX;
  const slope = (n * sumXy - sumX * sumY) / denom;
  const intercept = (sumY - slope * sumX) / n;
  return { slope, intercept };
}

/** Zukunfts-Horizont: ~30 % der Punktanzahl, mindestens 1 Bucket. */
export function forecastHorizon(n: number): number {
  return Math.max(1, Math.round(n * 0.3));
}

/** ISO-Datum `n` Perioden nach `iso`, je Granularität (UTC-basiert wie weekNumber). */
export function addPeriod(iso: string, granularity: Granularity, n: number): string {
  const d = new Date(iso);
  switch (granularity) {
    case 'DAILY':
      d.setUTCDate(d.getUTCDate() + n);
      break;
    case 'WEEKLY':
      d.setUTCDate(d.getUTCDate() + n * 7);
      break;
    case 'MONTHLY':
      d.setUTCMonth(d.getUTCMonth() + n);
      break;
    case 'YEARLY':
      d.setUTCFullYear(d.getUTCFullYear() + n);
      break;
  }
  return d.toISOString();
}

interface Props {
  widget: WidgetDto;
  onChange: (next: WidgetDto) => void;
  onDelete: () => void;
  readOnly?: boolean;
}

export default function WidgetPlot({
  widget,
  onChange,
  onDelete,
  readOnly = false,
}: Props): JSX.Element {
  const config = parseConfig(widget.config);
  const surface = widgetSurface(readOnly, config);
  const isRawMode = config.defaultGranularity === null;

  const [granularity, setGranularity] = useState<Granularity>(
    config.defaultGranularity ?? 'DAILY',
  );
  const [points, setPoints] = useState<ChartPoint[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [open, setOpen] = useState(false);
  const [seriesList, setSeriesList] = useState<TimeSeriesSummary[] | null>(null);
  const [draftSeriesId, setDraftSeriesId] = useState<string>(
    config.timeSeriesId == null ? '' : String(config.timeSeriesId),
  );
  const [draftGranularity, setDraftGranularity] = useState<string>(
    config.defaultGranularity ?? '',
  );
  const [draftOverlays, setDraftOverlays] = useState<PlotOverlay[]>(config.overlays);
  const [draftRegression, setDraftRegression] = useState(config.regression);
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

  useEffect(() => {
    if (config.defaultGranularity != null) setGranularity(config.defaultGranularity);
  }, [config.defaultGranularity]);

  useEffect(() => {
    if (config.timeSeriesId == null) {
      setPoints([]);
      return;
    }
    const id = config.timeSeriesId;
    let cancelled = false;
    setLoadError(null);
    setPoints(null);
    const request = isRawMode
      ? listEntries(id).then((entries) =>
          entries.map((e) => ({ label: formatEntryLabel(e.timestamp), value: e.value })),
        )
      : aggregateTimeSeries(id, granularity).then((buckets) =>
          buckets.map((b) => ({
            label: formatBucketLabel(b.bucketStart, granularity),
            value: b.avg,
            iso: b.bucketStart,
          })),
        );
    request
      .then((data) => {
        if (!cancelled) setPoints(data);
      })
      .catch((e) => {
        if (cancelled) return;
        setLoadError(e instanceof ApiError ? e.message : 'Laden fehlgeschlagen');
        setPoints([]);
      });
    return () => {
      cancelled = true;
    };
  }, [config.timeSeriesId, isRawMode, granularity]);

  useEffect(() => {
    if (!open) return;
    setDraftSeriesId(config.timeSeriesId == null ? '' : String(config.timeSeriesId));
    setDraftGranularity(config.defaultGranularity ?? '');
    setDraftOverlays(config.overlays);
    setDraftRegression(config.regression);
    setDraftShowBorder(config.showBorder);
    setDraftBackgroundColor(config.backgroundColor ?? '');
    if (seriesList === null) {
      listTimeSeries()
        .then(setSeriesList)
        .catch(() => setSeriesList([]));
    }
    // `widget.config` ist die stabile String-Quelle aller config.*-Felder; die
    // geparsten Werte (u. a. das overlays-Array) sind pro Render neue Referenzen.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, widget.config, seriesList]);

  function toggleOverlay(overlay: PlotOverlay): void {
    setDraftOverlays((prev) =>
      prev.includes(overlay) ? prev.filter((o) => o !== overlay) : [...prev, overlay],
    );
  }

  function handleApply(): void {
    const nextGranularity: Granularity | null =
      draftGranularity === '' ? null : (draftGranularity as Granularity);
    const next: PlotConfig = {
      timeSeriesId: draftSeriesId === '' ? null : Number.parseInt(draftSeriesId, 10),
      defaultGranularity: nextGranularity,
      overlays: nextGranularity === null ? [] : draftOverlays,
      regression: nextGranularity === null ? false : draftRegression,
      showBorder: draftShowBorder,
      ...(draftBackgroundColor.trim() !== ''
        ? { backgroundColor: draftBackgroundColor.trim() }
        : {}),
    };
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  const chartData = useMemo<PlotDatum[]>(() => {
    const base = points ?? [];
    const plain = base.map((p) => ({ label: p.label, value: p.value }));
    if (isRawMode || !config.regression) return plain;
    const fit = linearRegression(base.map((p) => p.value));
    if (fit === null) return plain;
    const hist: PlotDatum[] = base.map((p, i) => ({
      label: p.label,
      value: p.value,
      regression: fit.slope * i + fit.intercept,
    }));
    const lastIso = base[base.length - 1]?.iso;
    const horizon = forecastHorizon(base.length);
    const future: PlotDatum[] = [];
    for (let k = 1; k <= horizon; k++) {
      const idx = base.length - 1 + k;
      const label = lastIso
        ? formatBucketLabel(addPeriod(lastIso, granularity, k), granularity)
        : `+${k}`;
      future.push({ label, value: null, regression: fit.slope * idx + fit.intercept });
    }
    return [...hist, ...future];
  }, [points, isRawMode, config.regression, granularity]);

  const showRegressionLine = chartData.some((d) => d.regression !== undefined);

  const overlayLines = useMemo(() => {
    const data = points ?? [];
    if (isRawMode || data.length === 0 || config.overlays.length === 0) return [];
    const values = data.map((p) => p.value);
    return config.overlays.map((overlay) => ({
      overlay,
      y: overlayValue(overlay, values),
      ...OVERLAY_META[overlay],
    }));
  }, [isRawMode, points, config.overlays]);

  const noConfiguredSeries = config.timeSeriesId == null;

  return (
    <Paper
      variant={surface.variant}
      elevation={surface.elevation}
      sx={{ p: 1.5, height: '100%', display: 'flex', flexDirection: 'column', ...surface.sx }}
    >
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="subtitle2" noWrap>
          Plot
        </Typography>
        {!readOnly && (
          <Stack direction="row" spacing={0.5}>
            <IconButton size="small" onClick={() => setOpen(true)} aria-label="Plot bearbeiten">
              <EditIcon fontSize="small" />
            </IconButton>
            <IconButton size="small" onClick={onDelete} aria-label="Plot löschen">
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Stack>
        )}
      </Stack>

      {!isRawMode && (
        <Tabs
          value={granularity}
          onChange={(_, v) => setGranularity(v as Granularity)}
          variant="scrollable"
          scrollButtons={false}
          sx={{ minHeight: 32, '& .MuiTab-root': { minHeight: 32, py: 0.5 } }}
        >
          {GRANULARITIES.map((g) => (
            <Tab key={g.value} value={g.value} label={g.label} />
          ))}
        </Tabs>
      )}

      <Box sx={{ flex: 1, minHeight: 120, mt: 1 }} aria-label="Plot-Bereich">
        {noConfiguredSeries ? (
          <Alert severity="info" sx={{ height: '100%' }}>
            Bitte eine Zeitreihe wählen (über das Stift-Icon).
          </Alert>
        ) : loadError ? (
          <Alert severity="error">{loadError}</Alert>
        ) : points === null ? (
          <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }}>
            <CircularProgress size={24} />
          </Stack>
        ) : points.length === 0 ? (
          <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }}>
            <Typography variant="body2" color="text.secondary">
              Keine Daten im Zeitfenster.
            </Typography>
          </Stack>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 8, right: 12, bottom: 8, left: 4 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              {overlayLines.map((ol) => (
                <ReferenceLine
                  key={ol.overlay}
                  y={ol.y}
                  stroke={ol.color}
                  strokeDasharray="4 4"
                  label={{
                    value: `${ol.prefix} ${formatOverlayNumber(ol.y)}`,
                    position: 'right',
                    fontSize: 10,
                    fill: ol.color,
                  }}
                />
              ))}
              <Line
                type="monotone"
                dataKey="value"
                stroke="#1976d2"
                dot={{ r: 3 }}
                isAnimationActive={false}
              />
              {showRegressionLine && (
                <Line
                  type="linear"
                  dataKey="regression"
                  stroke="#9c27b0"
                  strokeDasharray="5 5"
                  dot={false}
                  connectNulls
                  isAnimationActive={false}
                />
              )}
            </LineChart>
          </ResponsiveContainer>
        )}
      </Box>

      <Drawer anchor="right" open={open} onClose={() => setOpen(false)}>
        <Box sx={{ width: 340, display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Toolbar>
            <Typography variant="h6">Plot bearbeiten</Typography>
          </Toolbar>
          <Divider />
          <Stack spacing={2} sx={{ p: 2, flex: 1, overflow: 'auto' }}>
            <TextField
              label="Zeitreihe"
              select
              value={draftSeriesId}
              onChange={(e) => setDraftSeriesId(e.target.value)}
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
              label="Granularität"
              select
              value={draftGranularity}
              onChange={(e) => setDraftGranularity(e.target.value)}
              helperText="Ohne Granularität werden alle Rohwerte als Linie gezeigt."
            >
              <MenuItem value="">
                <em>(keine — Rohwerte)</em>
              </MenuItem>
              {GRANULARITIES.map((g) => (
                <MenuItem key={g.value} value={g.value}>
                  {g.label}
                </MenuItem>
              ))}
            </TextField>
            {draftGranularity !== '' && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Overlay-Linien
                </Typography>
                <FormGroup>
                  {OVERLAY_ORDER.map((overlay) => (
                    <FormControlLabel
                      key={overlay}
                      control={
                        <Checkbox
                          checked={draftOverlays.includes(overlay)}
                          onChange={() => toggleOverlay(overlay)}
                        />
                      }
                      label={OVERLAY_META[overlay].label}
                    />
                  ))}
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={draftRegression}
                        onChange={(e) => setDraftRegression(e.target.checked)}
                      />
                    }
                    label="Trend / Regression"
                  />
                </FormGroup>
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
              placeholder="z. B. #1e1e1e oder rgba(255,255,255,0.05)"
            />
          </Stack>
          <Divider />
          <Stack direction="row" spacing={1} sx={{ p: 2, justifyContent: 'flex-end' }}>
            <Button onClick={() => setOpen(false)}>Abbrechen</Button>
            <Button variant="contained" onClick={handleApply}>
              Übernehmen
            </Button>
          </Stack>
        </Box>
      </Drawer>
    </Paper>
  );
}
