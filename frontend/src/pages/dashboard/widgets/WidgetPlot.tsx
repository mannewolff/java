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
  Area,
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  Line,
  Pie,
  PieChart,
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

/** Unterstützte Diagrammtypen (#180). `line` ist der Default (Rückwärtskompatibilität). */
export type ChartType = 'line' | 'area' | 'bar' | 'pie';

const CHART_TYPES: ReadonlyArray<{ value: ChartType; label: string }> = [
  { value: 'line', label: 'Linie' },
  { value: 'area', label: 'Fläche' },
  { value: 'bar', label: 'Balken' },
  { value: 'pie', label: 'Kuchen' },
];

/*
 * Bewusste Ausnahme von "Farben nur über das Theme" (CLAUDE-react.md), analog zu
 * statusColors.ts: Alle Hex-Werte in dieser Datei (PIE_COLORS, DEFAULT_SERIES_COLORS,
 * SWATCH_COLORS, OVERLAY_META, Trend-Linie) sind Diagramm-Paletten. Sie brauchen eine feste,
 * untereinander gut unterscheidbare Serienfolge, die unabhängig vom MUI-Theme stabil bleibt —
 * kein allgemeines UI-Token, sondern semantische Serien-/Overlay-Farben.
 */

/** Farbpalette für Pie-Slices, wenn die einzelnen Punkte keine eigene Farbe haben. */
const PIE_COLORS: readonly string[] = [
  '#1976d2',
  '#d32f2f',
  '#2e7d32',
  '#ed6c02',
  '#9c27b0',
  '#0288d1',
  '#c2185b',
];

/** Liest den Diagrammtyp defensiv; fehlend/ungültig → `'line'` (alte Plots bleiben Linien). */
export function parseChartType(value: unknown): ChartType {
  return value === 'area' || value === 'bar' || value === 'pie' ? value : 'line';
}

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

/** Maximale Anzahl gleichzeitig darstellbarer Zeitreihen. */
export const MAX_SERIES = 3;

/** Hartes Maximum fuer das Datenpunkt-Limit — deckt sich mit dem Backend (#197). */
export const MAX_LIMIT = 10_000;

/**
 * Liest das optionale Datenpunkt-Limit defensiv aus einer Roh-Config. Nicht-positive oder
 * ungueltige Werte ergeben `null` (= unbegrenzt); gueltige werden auf {@link MAX_LIMIT} geklemmt.
 */
export function parseLimit(raw: unknown): number | null {
  if (typeof raw !== 'number' || !Number.isFinite(raw)) return null;
  const rounded = Math.floor(raw);
  if (rounded < 1) return null;
  return Math.min(rounded, MAX_LIMIT);
}
/** Default-Farben für neue Serien (Blau, Rot, Grün — analog matplotlib-Stil). */
export const DEFAULT_SERIES_COLORS: readonly string[] = ['#1976d2', '#d32f2f', '#2e7d32'];
/** Auswählbare Serien-Farben im Drawer (5 gängige, gut unterscheidbare Farben). */
export const SWATCH_COLORS: ReadonlyArray<{ name: string; value: string }> = [
  { name: 'Blau', value: '#1976d2' },
  { name: 'Rot', value: '#d32f2f' },
  { name: 'Grün', value: '#2e7d32' },
  { name: 'Orange', value: '#ed6c02' },
  { name: 'Lila', value: '#9c27b0' },
];

/** Y-Achsen-Seite einer Serie. */
type AxisSide = 'left' | 'right';

/** Eine konfigurierte Zeitreihe im Plot. */
interface PlotSeries {
  timeSeriesId: number;
  color: string;
  /** Welche Y-Achse die Serie nutzt. Default `'left'`. */
  yAxis: AxisSide;
}

interface PlotConfig {
  /** Diagrammtyp (#180). Default `'line'`. */
  chartType: ChartType;
  /** Bis zu {@link MAX_SERIES} Zeitreihen. Leer = nichts konfiguriert. */
  series: PlotSeries[];
  /** `null` = Rohwerte-Modus (neuer Default), sonst aggregierter Modus mit Tabs. */
  defaultGranularity: Granularity | null;
  overlays: PlotOverlay[];
  /** Lineare Trend-/Regressionslinie — nur bei genau 1 Serie + aggregiertem Modus. */
  regression: boolean;
  /** Feste Y-Achsen-Grenzen. `null` = automatisch (recharts-Default). */
  yMin: number | null;
  yMax: number | null;
  /**
   * Begrenzt die Anzahl angezeigter Datenpunkte auf die juengsten N (#197).
   * `null` = unbegrenzt (altes Verhalten).
   */
  limit: number | null;
  /** Legende ein-/ausblenden. */
  showLegend: boolean;
  showBorder: boolean;
  backgroundColor?: string;
}

interface ChartPoint {
  label: string;
  value: number;
  /** Bucket-Start bzw. Timestamp (ISO) — Basis für chronologische Sortierung + Regressions-Labels. */
  iso?: string;
}

/**
 * Render-Zeile für recharts: ein Eintrag pro X-Label, mit je einem Wert pro Serie (`s0`, `s1`, …),
 * optionalem Regressionswert und ISO für die Sortierung. Zukunftspunkte haben keinen Serien-Wert.
 */
interface ChartRow {
  label: string;
  iso?: string;
  regression?: number;
  [seriesKey: string]: number | string | undefined;
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

/**
 * Serien-Liste aus der Roh-Config. Migriert Legacy-Single-Serie (`timeSeriesId`) zu einem
 * einelementigen `series`-Array. Begrenzt auf {@link MAX_SERIES}.
 */
export function parseSeries(parsed: Record<string, unknown>): PlotSeries[] {
  if (Array.isArray(parsed.series)) {
    return parsed.series
      .filter((s): s is Record<string, unknown> => typeof s === 'object' && s !== null)
      .map((s, i) => ({
        timeSeriesId: typeof s.timeSeriesId === 'number' ? s.timeSeriesId : -1,
        color:
          typeof s.color === 'string' && s.color.trim() !== ''
            ? s.color
            : DEFAULT_SERIES_COLORS[i % DEFAULT_SERIES_COLORS.length],
        yAxis: s.yAxis === 'right' ? ('right' as AxisSide) : ('left' as AxisSide),
      }))
      .filter((s) => s.timeSeriesId >= 0)
      .slice(0, MAX_SERIES);
  }
  if (typeof parsed.timeSeriesId === 'number') {
    return [{ timeSeriesId: parsed.timeSeriesId, color: DEFAULT_SERIES_COLORS[0], yAxis: 'left' }];
  }
  return [];
}

/** Wird mindestens eine Serie an der rechten Y-Achse dargestellt? */
export function usesRightAxis(series: ReadonlyArray<{ yAxis: AxisSide }>): boolean {
  return series.some((s) => s.yAxis === 'right');
}

function parseConfig(raw: string): PlotConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const series = parseSeries(parsed);
    return {
      chartType: parseChartType(parsed.chartType),
      series,
      defaultGranularity: parseGranularity(parsed.defaultGranularity),
      overlays: parseOverlays(parsed.overlays),
      regression: typeof parsed.regression === 'boolean' ? parsed.regression : false,
      yMin: typeof parsed.yMin === 'number' ? parsed.yMin : null,
      yMax: typeof parsed.yMax === 'number' ? parsed.yMax : null,
      limit: parseLimit(parsed.limit),
      showLegend:
        typeof parsed.showLegend === 'boolean' ? parsed.showLegend : series.length > 1,
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return {
      chartType: 'line',
      series: [],
      defaultGranularity: null,
      overlays: [],
      regression: false,
      yMin: null,
      yMax: null,
      limit: null,
      showLegend: false,
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

/**
 * Führt die Punktlisten mehrerer Serien per X-Label zu Chart-Zeilen zusammen (Outer-Join).
 * Serie `i` landet unter dem Schlüssel `s${i}`; fehlende Werte bleiben undefined (Lücke).
 * Zeilen werden chronologisch nach `iso` sortiert, sofern vorhanden.
 */
export function mergeSeries(perSeries: ChartPoint[][]): ChartRow[] {
  const byLabel = new Map<string, ChartRow>();
  perSeries.forEach((points, si) => {
    points.forEach((p) => {
      let row = byLabel.get(p.label);
      if (row === undefined) {
        row = { label: p.label, iso: p.iso };
        byLabel.set(p.label, row);
      } else if (row.iso === undefined && p.iso !== undefined) {
        row.iso = p.iso;
      }
      row[`s${si}`] = p.value;
    });
  });
  const rows = [...byLabel.values()];
  rows.sort((a, b) => {
    if (a.iso != null && b.iso != null) {
      return a.iso < b.iso ? -1 : a.iso > b.iso ? 1 : 0;
    }
    return 0;
  });
  return rows;
}

/**
 * recharts-Y-Achsen-Domain aus optionalen Grenzen. Beide `null` → `undefined`
 * (kein domain-Prop, Default-Verhalten). Einzelne Grenze → die andere Seite `'auto'`.
 */
export function computeYDomain(
  yMin: number | null,
  yMax: number | null,
): [number | 'auto', number | 'auto'] | undefined {
  if (yMin === null && yMax === null) return undefined;
  return [yMin ?? 'auto', yMax ?? 'auto'];
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
  const [perSeriesData, setPerSeriesData] = useState<ChartPoint[][] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [open, setOpen] = useState(false);
  const [seriesList, setSeriesList] = useState<TimeSeriesSummary[] | null>(null);
  const [draftChartType, setDraftChartType] = useState<ChartType>(config.chartType);
  const [draftSeries, setDraftSeries] = useState<PlotSeries[]>(config.series);
  const [draftGranularity, setDraftGranularity] = useState<string>(
    config.defaultGranularity ?? '',
  );
  const [draftOverlays, setDraftOverlays] = useState<PlotOverlay[]>(config.overlays);
  const [draftRegression, setDraftRegression] = useState(config.regression);
  const [draftYMin, setDraftYMin] = useState(config.yMin == null ? '' : String(config.yMin));
  const [draftYMax, setDraftYMax] = useState(config.yMax == null ? '' : String(config.yMax));
  const [draftLimitEnabled, setDraftLimitEnabled] = useState(config.limit != null);
  const [draftLimit, setDraftLimit] = useState(config.limit == null ? '' : String(config.limit));
  const [draftShowLegend, setDraftShowLegend] = useState(config.showLegend);
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

  const seriesIds = config.series.map((s) => s.timeSeriesId);
  const seriesIdsKey = seriesIds.join(',');

  /** Anzeigename einer Serie für Legende/Tooltip. */
  function seriesName(id: number): string {
    return seriesList?.find((s) => s.id === id)?.name ?? `Serie ${id}`;
  }

  useEffect(() => {
    if (config.defaultGranularity != null) setGranularity(config.defaultGranularity);
  }, [config.defaultGranularity]);

  useEffect(() => {
    if (seriesIds.length === 0) {
      setPerSeriesData([]);
      return;
    }
    let cancelled = false;
    setLoadError(null);
    setPerSeriesData(null);
    const limit = config.limit ?? undefined;
    const fetchOne = (id: number): Promise<ChartPoint[]> =>
      isRawMode
        ? listEntries(id, { limit }).then((entries) =>
            entries.map((e) => ({
              label: formatEntryLabel(e.timestamp),
              value: e.value,
              iso: e.timestamp,
            })),
          )
        : aggregateTimeSeries(id, granularity, undefined, undefined, limit).then((buckets) =>
            buckets.map((b) => ({
              label: formatBucketLabel(b.bucketStart, granularity),
              value: b.avg,
              iso: b.bucketStart,
            })),
          );
    Promise.all(seriesIds.map(fetchOne))
      .then((data) => {
        if (!cancelled) setPerSeriesData(data);
      })
      .catch((e) => {
        if (cancelled) return;
        setLoadError(e instanceof ApiError ? e.message : 'Laden fehlgeschlagen');
        setPerSeriesData([]);
      });
    return () => {
      cancelled = true;
    };
    // seriesIdsKey ist die stabile String-Quelle der Serien-IDs (Array ist pro Render neu).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seriesIdsKey, isRawMode, granularity, config.limit]);

  // Serien-Namen für Legende/Tooltip auch im Lese-Modus laden (nicht nur beim Drawer-Öffnen).
  useEffect(() => {
    if (seriesList === null && seriesIds.length > 0) {
      listTimeSeries()
        .then(setSeriesList)
        .catch(() => setSeriesList([]));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seriesIdsKey]);

  useEffect(() => {
    if (!open) return;
    setDraftChartType(config.chartType);
    setDraftSeries(config.series);
    setDraftGranularity(config.defaultGranularity ?? '');
    setDraftOverlays(config.overlays);
    setDraftRegression(config.regression);
    setDraftYMin(config.yMin == null ? '' : String(config.yMin));
    setDraftYMax(config.yMax == null ? '' : String(config.yMax));
    setDraftLimitEnabled(config.limit != null);
    setDraftLimit(config.limit == null ? '' : String(config.limit));
    setDraftShowLegend(config.showLegend);
    setDraftShowBorder(config.showBorder);
    setDraftBackgroundColor(config.backgroundColor ?? '');
    if (seriesList === null) {
      listTimeSeries()
        .then(setSeriesList)
        .catch(() => setSeriesList([]));
    }
    // `widget.config` ist die stabile String-Quelle aller config.*-Felder; die
    // geparsten Werte (u. a. das series-Array) sind pro Render neue Referenzen.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, widget.config, seriesList]);

  function toggleOverlay(overlay: PlotOverlay): void {
    setDraftOverlays((prev) =>
      prev.includes(overlay) ? prev.filter((o) => o !== overlay) : [...prev, overlay],
    );
  }

  function updateSeries(index: number, patch: Partial<PlotSeries>): void {
    setDraftSeries((prev) => prev.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  }

  function addSeries(): void {
    setDraftSeries((prev) => {
      if (prev.length >= MAX_SERIES) return prev;
      const firstId = seriesList?.[0]?.id ?? -1;
      return [
        ...prev,
        {
          timeSeriesId: firstId,
          color: DEFAULT_SERIES_COLORS[prev.length % DEFAULT_SERIES_COLORS.length],
          yAxis: 'left',
        },
      ];
    });
  }

  function removeSeries(index: number): void {
    setDraftSeries((prev) => prev.filter((_, i) => i !== index));
  }

  function handleApply(): void {
    const nextGranularity: Granularity | null =
      draftGranularity === '' ? null : (draftGranularity as Granularity);
    const parsedYMin = Number.parseFloat(draftYMin);
    const parsedYMax = Number.parseFloat(draftYMax);
    const cleanSeries = draftSeries.filter((s) => s.timeSeriesId >= 0).slice(0, MAX_SERIES);
    // Overlays/Regression nur bei genau 1 Serie im aggregierten Modus sinnvoll.
    const singleAggregated = cleanSeries.length === 1 && nextGranularity !== null;
    const next: PlotConfig = {
      chartType: draftChartType,
      series: cleanSeries,
      defaultGranularity: nextGranularity,
      overlays: singleAggregated ? draftOverlays : [],
      regression: singleAggregated ? draftRegression : false,
      yMin: draftYMin.trim() === '' || !Number.isFinite(parsedYMin) ? null : parsedYMin,
      yMax: draftYMax.trim() === '' || !Number.isFinite(parsedYMax) ? null : parsedYMax,
      limit: draftLimitEnabled ? parseLimit(Number.parseInt(draftLimit, 10)) : null,
      showLegend: draftShowLegend,
      showBorder: draftShowBorder,
      ...(draftBackgroundColor.trim() !== ''
        ? { backgroundColor: draftBackgroundColor.trim() }
        : {}),
    };
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  // Overlays + Regression sind nur bei genau 1 Serie im aggregierten Modus aktiv.
  const isSingleSeries = config.series.length === 1;

  const chartData = useMemo<ChartRow[]>(() => {
    const per = perSeriesData ?? [];
    const rows = mergeSeries(per);
    if (!isSingleSeries || isRawMode || !config.regression) return rows;
    const base = per[0] ?? [];
    const fit = linearRegression(base.map((p) => p.value));
    if (fit === null) return rows;
    rows.forEach((row, i) => {
      row.regression = fit.slope * i + fit.intercept;
    });
    const lastIso = base[base.length - 1]?.iso;
    const horizon = forecastHorizon(base.length);
    for (let k = 1; k <= horizon; k++) {
      const idx = base.length - 1 + k;
      const label = lastIso
        ? formatBucketLabel(addPeriod(lastIso, granularity, k), granularity)
        : `+${k}`;
      rows.push({ label, regression: fit.slope * idx + fit.intercept });
    }
    return rows;
  }, [perSeriesData, isSingleSeries, isRawMode, config.regression, granularity]);

  const showRegressionLine = chartData.some((d) => d.regression !== undefined);

  const overlayLines = useMemo(() => {
    const base = perSeriesData?.[0] ?? [];
    if (!isSingleSeries || isRawMode || base.length === 0 || config.overlays.length === 0) {
      return [];
    }
    const values = base.map((p) => p.value);
    return config.overlays.map((overlay) => ({
      overlay,
      y: overlayValue(overlay, values),
      ...OVERLAY_META[overlay],
    }));
  }, [isSingleSeries, isRawMode, perSeriesData, config.overlays]);

  // Pie nutzt die erste Serie: ein Slice je Datenpunkt (Label → Wert).
  const pieData = useMemo(
    () => (perSeriesData?.[0] ?? []).map((p) => ({ name: p.label, value: p.value })),
    [perSeriesData],
  );

  /** Rendert die kartesische Darstellung einer Serie passend zum Diagrammtyp. */
  function renderSeriesElement(s: PlotSeries, i: number): JSX.Element {
    const name = seriesName(s.timeSeriesId);
    if (config.chartType === 'bar') {
      return <Bar key={i} yAxisId={s.yAxis} dataKey={`s${i}`} name={name} fill={s.color} isAnimationActive={false} />;
    }
    if (config.chartType === 'area') {
      return (
        <Area
          key={i}
          yAxisId={s.yAxis}
          type="monotone"
          dataKey={`s${i}`}
          name={name}
          stroke={s.color}
          fill={s.color}
          fillOpacity={0.3}
          connectNulls={false}
          isAnimationActive={false}
        />
      );
    }
    return (
      <Line
        key={i}
        yAxisId={s.yAxis}
        type="monotone"
        dataKey={`s${i}`}
        name={name}
        stroke={s.color}
        dot={{ r: 3 }}
        connectNulls={false}
        isAnimationActive={false}
      />
    );
  }

  const noConfiguredSeries = config.series.length === 0;
  const noData =
    perSeriesData !== null && perSeriesData.every((d) => d.length === 0);

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
        ) : perSeriesData === null ? (
          <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }}>
            <CircularProgress size={24} />
          </Stack>
        ) : noData ? (
          <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }}>
            <Typography variant="body2" color="text.secondary">
              Keine Daten im Zeitfenster.
            </Typography>
          </Stack>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            {config.chartType === 'pie' ? (
              <PieChart>
                <Tooltip />
                {config.showLegend && <Legend />}
                <Pie
                  data={pieData}
                  dataKey="value"
                  nameKey="name"
                  outerRadius="80%"
                  label
                  isAnimationActive={false}
                >
                  {pieData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
              </PieChart>
            ) : (
              <ComposedChart data={chartData} margin={{ top: 8, right: 12, bottom: 8, left: 4 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis
                  yAxisId="left"
                  domain={computeYDomain(config.yMin, config.yMax)}
                  tick={{ fontSize: 11 }}
                />
                {usesRightAxis(config.series) && (
                  <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 11 }} />
                )}
                <Tooltip />
                {config.showLegend && <Legend />}
                {overlayLines.map((ol) => (
                  <ReferenceLine
                    key={ol.overlay}
                    yAxisId="left"
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
                {config.series.map((s, i) => renderSeriesElement(s, i))}
                {showRegressionLine && (
                  <Line
                    yAxisId="left"
                    type="linear"
                    dataKey="regression"
                    name="Trend"
                    stroke="#9c27b0"
                    strokeDasharray="5 5"
                    dot={false}
                    connectNulls
                    isAnimationActive={false}
                  />
                )}
              </ComposedChart>
            )}
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
              label="Diagrammtyp"
              select
              value={draftChartType}
              onChange={(e) => setDraftChartType(e.target.value as ChartType)}
            >
              {CHART_TYPES.map((c) => (
                <MenuItem key={c.value} value={c.value}>
                  {c.label}
                </MenuItem>
              ))}
            </TextField>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Zeitreihen (max {MAX_SERIES})
              </Typography>
              <Stack spacing={1.5} sx={{ mt: 0.5 }}>
                {draftSeries.map((s, i) => (
                  <Stack key={i} spacing={1.25}>
                    <TextField
                      select
                      size="small"
                      label={`Serie ${i + 1}`}
                      value={String(s.timeSeriesId)}
                      onChange={(e) =>
                        updateSeries(i, { timeSeriesId: Number.parseInt(e.target.value, 10) })
                      }
                      fullWidth
                    >
                      {(seriesList ?? []).map((ts) => (
                        <MenuItem key={ts.id} value={String(ts.id)}>
                          {ts.name} ({ts.unit})
                        </MenuItem>
                      ))}
                    </TextField>
                    <Stack direction="row" spacing={1.5} alignItems="flex-end">
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Farbe
                        </Typography>
                        <Stack direction="row" spacing={0.5} sx={{ mt: 0.25 }}>
                          {SWATCH_COLORS.map((c) => {
                            const selected = s.color === c.value;
                            return (
                              <Box
                                key={c.value}
                                component="button"
                                type="button"
                                aria-label={`Farbe ${c.name}`}
                                aria-pressed={selected}
                                onClick={() => updateSeries(i, { color: c.value })}
                                sx={{
                                  width: 22,
                                  height: 22,
                                  p: 0,
                                  borderRadius: '50%',
                                  cursor: 'pointer',
                                  bgcolor: c.value,
                                  border: (theme) =>
                                    selected
                                      ? `2px solid ${theme.palette.text.primary}`
                                      : `1px solid ${theme.palette.divider}`,
                                  boxShadow: (theme) =>
                                    selected ? `0 0 0 2px ${theme.palette.background.paper}` : 'none',
                                }}
                              />
                            );
                          })}
                        </Stack>
                      </Box>
                      <TextField
                        select
                        size="small"
                        label="Achse"
                        value={s.yAxis}
                        onChange={(e) => updateSeries(i, { yAxis: e.target.value as AxisSide })}
                        sx={{ minWidth: 88 }}
                      >
                        <MenuItem value="left">Links</MenuItem>
                        <MenuItem value="right">Rechts</MenuItem>
                      </TextField>
                      <IconButton
                        size="small"
                        aria-label={`Serie ${i + 1} entfernen`}
                        onClick={() => removeSeries(i)}
                        sx={{ ml: 'auto' }}
                      >
                        <DeleteOutlineIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  </Stack>
                ))}
              </Stack>
              {draftSeries.length < MAX_SERIES && (
                <Button size="small" onClick={addSeries} sx={{ mt: 1 }} disabled={!seriesList?.length}>
                  + Zeitreihe
                </Button>
              )}
            </Box>
            <FormControlLabel
              control={
                <Switch
                  checked={draftShowLegend}
                  onChange={(e) => setDraftShowLegend(e.target.checked)}
                />
              }
              label="Legende anzeigen"
            />
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
            {draftGranularity !== '' && draftSeries.length === 1 && (
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
            <Stack direction="row" spacing={1}>
              <TextField
                label="Y-Achse Min"
                type="number"
                value={draftYMin}
                onChange={(e) => setDraftYMin(e.target.value)}
                fullWidth
                inputProps={{ step: 'any' }}
                helperText="leer = automatisch"
              />
              <TextField
                label="Y-Achse Max"
                type="number"
                value={draftYMax}
                onChange={(e) => setDraftYMax(e.target.value)}
                fullWidth
                inputProps={{ step: 'any' }}
                helperText="leer = automatisch"
              />
            </Stack>
            <Box>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={draftLimitEnabled}
                    onChange={(e) => setDraftLimitEnabled(e.target.checked)}
                  />
                }
                label="Anzahl der Werte limitieren"
              />
              <TextField
                label="Maximale Werte"
                type="number"
                value={draftLimit}
                onChange={(e) => setDraftLimit(e.target.value)}
                disabled={!draftLimitEnabled}
                fullWidth
                inputProps={{ min: 1, max: MAX_LIMIT, step: 1 }}
                helperText={`zeigt nur die juengsten N Werte (1–${MAX_LIMIT})`}
              />
            </Box>
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
