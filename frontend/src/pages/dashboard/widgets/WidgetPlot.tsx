import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  Drawer,
  IconButton,
  MenuItem,
  Paper,
  Stack,
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
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import type { WidgetDto } from '../../../api/dashboard';
import {
  aggregateTimeSeries,
  listTimeSeries,
  type AggregateBucket,
  type Granularity,
  type TimeSeriesSummary,
} from '../../../api/timeseries';
import { ApiError } from '../../../api/client';

type Metric = 'avg' | 'min' | 'max' | 'last';

const METRICS: ReadonlyArray<{ value: Metric; label: string }> = [
  { value: 'avg', label: 'Mittelwert' },
  { value: 'min', label: 'Minimum' },
  { value: 'max', label: 'Maximum' },
  { value: 'last', label: 'Letzter Wert' },
];

const GRANULARITIES: ReadonlyArray<{ value: Granularity; label: string }> = [
  { value: 'DAILY', label: 'Täglich' },
  { value: 'WEEKLY', label: 'Wöchentlich' },
  { value: 'MONTHLY', label: 'Monatlich' },
  { value: 'YEARLY', label: 'Jährlich' },
];

interface PlotConfig {
  timeSeriesId: number | null;
  metric: Metric;
  defaultGranularity: Granularity;
}

function isMetric(v: unknown): v is Metric {
  return v === 'avg' || v === 'min' || v === 'max' || v === 'last';
}

function isGranularity(v: unknown): v is Granularity {
  return v === 'DAILY' || v === 'WEEKLY' || v === 'MONTHLY' || v === 'YEARLY';
}

function parseConfig(raw: string): PlotConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      timeSeriesId: typeof parsed.timeSeriesId === 'number' ? parsed.timeSeriesId : null,
      metric: isMetric(parsed.metric) ? parsed.metric : 'avg',
      defaultGranularity: isGranularity(parsed.defaultGranularity)
        ? parsed.defaultGranularity
        : 'DAILY',
    };
  } catch {
    return { timeSeriesId: null, metric: 'avg', defaultGranularity: 'DAILY' };
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
  const [granularity, setGranularity] = useState<Granularity>(config.defaultGranularity);
  const [buckets, setBuckets] = useState<AggregateBucket[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [open, setOpen] = useState(false);
  const [seriesList, setSeriesList] = useState<TimeSeriesSummary[] | null>(null);
  const [draftSeriesId, setDraftSeriesId] = useState<string>(
    config.timeSeriesId == null ? '' : String(config.timeSeriesId),
  );
  const [draftMetric, setDraftMetric] = useState<Metric>(config.metric);
  const [draftGranularity, setDraftGranularity] = useState<Granularity>(
    config.defaultGranularity,
  );

  useEffect(() => {
    setGranularity(config.defaultGranularity);
  }, [config.defaultGranularity]);

  useEffect(() => {
    if (config.timeSeriesId == null) {
      setBuckets([]);
      return;
    }
    let cancelled = false;
    setLoadError(null);
    aggregateTimeSeries(config.timeSeriesId, granularity)
      .then((data) => {
        if (!cancelled) setBuckets(data);
      })
      .catch((e) => {
        if (cancelled) return;
        setLoadError(e instanceof ApiError ? e.message : 'Laden fehlgeschlagen');
        setBuckets([]);
      });
    return () => {
      cancelled = true;
    };
  }, [config.timeSeriesId, granularity]);

  useEffect(() => {
    if (!open) return;
    setDraftSeriesId(config.timeSeriesId == null ? '' : String(config.timeSeriesId));
    setDraftMetric(config.metric);
    setDraftGranularity(config.defaultGranularity);
    if (seriesList === null) {
      listTimeSeries()
        .then(setSeriesList)
        .catch(() => setSeriesList([]));
    }
  }, [open, config.timeSeriesId, config.metric, config.defaultGranularity, seriesList]);

  function handleApply(): void {
    const next: PlotConfig = {
      timeSeriesId: draftSeriesId === '' ? null : Number.parseInt(draftSeriesId, 10),
      metric: draftMetric,
      defaultGranularity: draftGranularity,
    };
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  const chartData = useMemo(() => {
    if (!buckets) return [];
    return buckets.map((b) => ({
      label: formatBucketLabel(b.bucketStart, granularity),
      value: b[config.metric],
    }));
  }, [buckets, granularity, config.metric]);

  const noConfiguredSeries = config.timeSeriesId == null;

  return (
    <Paper variant="outlined" sx={{ p: 1.5, height: '100%', display: 'flex', flexDirection: 'column' }}>
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

      <Box sx={{ flex: 1, minHeight: 120, mt: 1 }} aria-label="Plot-Bereich">
        {noConfiguredSeries ? (
          <Alert severity="info" sx={{ height: '100%' }}>
            Bitte eine Zeitreihe wählen (über das Stift-Icon).
          </Alert>
        ) : loadError ? (
          <Alert severity="error">{loadError}</Alert>
        ) : buckets === null ? (
          <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }}>
            <CircularProgress size={24} />
          </Stack>
        ) : buckets.length === 0 ? (
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
              <Line
                type="monotone"
                dataKey="value"
                stroke="#1976d2"
                dot={{ r: 3 }}
                isAnimationActive={false}
              />
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
          <Stack spacing={2} sx={{ p: 2, flex: 1 }}>
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
              label="Metrik"
              select
              value={draftMetric}
              onChange={(e) => setDraftMetric(e.target.value as Metric)}
            >
              {METRICS.map((m) => (
                <MenuItem key={m.value} value={m.value}>
                  {m.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Standard-Granularität"
              select
              value={draftGranularity}
              onChange={(e) => setDraftGranularity(e.target.value as Granularity)}
            >
              {GRANULARITIES.map((g) => (
                <MenuItem key={g.value} value={g.value}>
                  {g.label}
                </MenuItem>
              ))}
            </TextField>
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
