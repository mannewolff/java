import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetPlot, {
  addPeriod,
  computeYDomain,
  forecastHorizon,
  linearRegression,
  mergeSeries,
  overlayValue,
  parseChartType,
  usesRightAxis,
} from './WidgetPlot';
import type { WidgetDto } from '../../../api/dashboard';

vi.mock('../../../api/timeseries', () => ({
  aggregateTimeSeries: vi.fn(),
  listEntries: vi.fn(),
  listTimeSeries: vi.fn(),
}));

import { aggregateTimeSeries, listEntries, listTimeSeries } from '../../../api/timeseries';

const aggregate = aggregateTimeSeries as ReturnType<typeof vi.fn>;
const entries = listEntries as ReturnType<typeof vi.fn>;
const list = listTimeSeries as ReturnType<typeof vi.fn>;

// recharts auf jsdom: ResponsiveContainer braucht explizite Groesse, sonst rendert es nichts.
// Wir mocken ResponsiveContainer auf ein einfaches div, damit der LineChart Children rendert.
vi.mock('recharts', async () => {
  const actual = await vi.importActual<typeof import('recharts')>('recharts');
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 400, height: 200 }}>{children}</div>
    ),
  };
});

function widget(config: object): WidgetDto {
  return {
    id: 1,
    type: 'PLOT',
    posX: 0,
    posY: 0,
    width: 6,
    height: 4,
    config: JSON.stringify(config),
  };
}

describe('WidgetPlot', () => {
  beforeEach(() => {
    aggregate.mockReset();
    entries.mockReset();
    list.mockReset();
    list.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
  });

  it('shows "please select" when no timeSeriesId configured', () => {
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: null, metric: 'avg', defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    expect(screen.getByText(/Bitte eine Zeitreihe wählen/)).toBeInTheDocument();
    expect(aggregate).not.toHaveBeenCalled();
  });

  it('loads aggregate when timeSeriesId is set and renders chart', async () => {
    aggregate.mockResolvedValueOnce([
      {
        bucketStart: '2026-05-27T00:00:00Z',
        count: 1,
        min: 10,
        max: 20,
        avg: 15,
        last: 20,
      },
      {
        bucketStart: '2026-05-28T00:00:00Z',
        count: 1,
        min: 30,
        max: 30,
        avg: 30,
        last: 30,
      },
    ]);

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, metric: 'avg', defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(42, 'DAILY'));
  });

  it('shows empty-state when API returns []', async () => {
    aggregate.mockResolvedValueOnce([]);

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, metric: 'avg', defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    await waitFor(() =>
      expect(screen.getByText(/Keine Daten/)).toBeInTheDocument(),
    );
  });

  it('shows error alert when API fails', async () => {
    aggregate.mockRejectedValueOnce(new Error('boom'));

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, metric: 'avg', defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    await waitFor(() =>
      expect(screen.getByText(/Laden fehlgeschlagen/)).toBeInTheDocument(),
    );
  });

  it('switches granularity on tab click and reloads', async () => {
    aggregate.mockResolvedValue([]);

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, metric: 'avg', defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(42, 'DAILY'));

    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'Wöchentlich' }));

    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(42, 'WEEKLY'));
  });

  it('opens drawer in edit mode with series dropdown', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValueOnce([
      {
        id: 1,
        name: 'Weight',
        unit: 'kg',
        dataType: 'DECIMAL',
        entryCount: 5,
        createdAt: 'x',
        updatedAt: 'x',
      },
    ]);

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: null, metric: 'avg', defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));

    await waitFor(() => expect(list).toHaveBeenCalled());
    expect(screen.getByText(/Plot bearbeiten/i)).toBeInTheDocument();
  });

  it('does not render edit/delete icons in read-only mode', () => {
    aggregate.mockResolvedValue([]);

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, metric: 'avg', defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
        readOnly
      />,
    );

    expect(screen.queryByRole('button', { name: 'Plot bearbeiten' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Plot löschen' })).not.toBeInTheDocument();
  });

  // ----- Rohwerte-Modus + Overlays (#124) --------------------------------

  it('Rohwerte-Modus (defaultGranularity null): lädt Einträge statt Aggregat, keine Tabs', async () => {
    entries.mockResolvedValueOnce([
      { id: 1, timestamp: '2026-05-27T08:00:00Z', value: 10 },
      { id: 2, timestamp: '2026-05-27T09:00:00Z', value: 20 },
    ]);

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, defaultGranularity: null, overlays: [] })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    await waitFor(() => expect(entries).toHaveBeenCalledWith(42));
    expect(aggregate).not.toHaveBeenCalled();
    expect(screen.queryByRole('tab', { name: 'Täglich' })).not.toBeInTheDocument();
  });

  it('fehlendes defaultGranularity-Feld → Rohwerte-Modus (Abwärtskompatibilität)', async () => {
    entries.mockResolvedValueOnce([]);

    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 7 })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    await waitFor(() => expect(entries).toHaveBeenCalledWith(7));
    expect(aggregate).not.toHaveBeenCalled();
  });

  it('Drawer: Overlay-Checkboxen nur bei gesetzter Granularität sichtbar', async () => {
    entries.mockResolvedValue([]);
    list.mockResolvedValueOnce([]);

    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, defaultGranularity: null, overlays: [] })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await screen.findByText('Plot bearbeiten');

    expect(screen.queryByRole('checkbox', { name: 'Mittelwert' })).not.toBeInTheDocument();
  });

  it('Drawer: konfigurierte Overlays sind vorausgewählt (aggregierter Modus)', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValueOnce([]);

    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({
          timeSeriesId: 42,
          defaultGranularity: 'WEEKLY',
          overlays: ['median', 'max'],
        })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));

    expect(await screen.findByRole('checkbox', { name: 'Median' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Maximum' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Mittelwert' })).not.toBeChecked();
  });

  it('Drawer: Overlay-Auswahl wird in der Config gespeichert', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValueOnce([]);

    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, defaultGranularity: 'DAILY', overlays: [] })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await user.click(await screen.findByRole('checkbox', { name: 'Mittelwert' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { defaultGranularity: string; overlays: string[] };
    expect(parsed.defaultGranularity).toBe('DAILY');
    expect(parsed.overlays).toEqual(['mean']);
  });
});

describe('overlayValue', () => {
  it('berechnet Mittelwert, Median, Minimum und Maximum', () => {
    const values = [10, 20, 30, 100];
    expect(overlayValue('mean', values)).toBe(40);
    expect(overlayValue('median', values)).toBe(25);
    expect(overlayValue('min', values)).toBe(10);
    expect(overlayValue('max', values)).toBe(100);
  });

  it('Median bei ungerader Anzahl ist der mittlere Wert (sortiert)', () => {
    expect(overlayValue('median', [5, 1, 3])).toBe(3);
  });
});

describe('linearRegression', () => {
  it('flache Daten → slope 0, Vorhersage bleibt konstant', () => {
    const reg = linearRegression([80, 80, 80]);
    expect(reg).not.toBeNull();
    expect(reg?.slope).toBeCloseTo(0);
    expect(reg?.intercept).toBeCloseTo(80);
    // predict(3) = slope*3 + intercept = 80
    expect((reg?.slope ?? 0) * 3 + (reg?.intercept ?? 0)).toBeCloseTo(80);
  });

  it('steigende Daten → positive slope, Extrapolation steigt', () => {
    const reg = linearRegression([10, 20, 30]);
    expect(reg?.slope).toBeCloseTo(10);
    expect(reg?.intercept).toBeCloseTo(10);
    expect((reg?.slope ?? 0) * 3 + (reg?.intercept ?? 0)).toBeCloseTo(40);
  });

  it('weniger als 2 Punkte → null', () => {
    expect(linearRegression([])).toBeNull();
    expect(linearRegression([42])).toBeNull();
  });
});

describe('forecastHorizon', () => {
  it('rund 30% der Punktanzahl, mindestens 1', () => {
    expect(forecastHorizon(10)).toBe(3);
    expect(forecastHorizon(3)).toBe(1);
    expect(forecastHorizon(2)).toBe(1);
    expect(forecastHorizon(20)).toBe(6);
  });
});

describe('mergeSeries', () => {
  it('identische Labels → eine Zeile pro Label mit s0/s1', () => {
    const a = [
      { label: 'A', value: 1, iso: '2026-01-01T00:00:00Z' },
      { label: 'B', value: 2, iso: '2026-01-02T00:00:00Z' },
    ];
    const b = [
      { label: 'A', value: 10, iso: '2026-01-01T00:00:00Z' },
      { label: 'B', value: 20, iso: '2026-01-02T00:00:00Z' },
    ];
    const rows = mergeSeries([a, b]);
    expect(rows).toHaveLength(2);
    expect(rows[0]).toMatchObject({ label: 'A', s0: 1, s1: 10 });
    expect(rows[1]).toMatchObject({ label: 'B', s0: 2, s1: 20 });
  });

  it('disjunkte Labels → Outer-Join, fehlende Werte undefined, chronologisch', () => {
    const a = [{ label: 'A', value: 1, iso: '2026-01-01T00:00:00Z' }];
    const b = [{ label: 'B', value: 20, iso: '2026-01-02T00:00:00Z' }];
    const rows = mergeSeries([a, b]);
    expect(rows.map((r) => r.label)).toEqual(['A', 'B']);
    expect(rows[0].s0).toBe(1);
    expect(rows[0].s1).toBeUndefined();
    expect(rows[1].s0).toBeUndefined();
    expect(rows[1].s1).toBe(20);
  });

  it('sortiert nach iso, auch bei unsortierter Eingabe', () => {
    const a = [
      { label: 'spät', value: 2, iso: '2026-01-02T00:00:00Z' },
      { label: 'früh', value: 1, iso: '2026-01-01T00:00:00Z' },
    ];
    expect(mergeSeries([a]).map((r) => r.label)).toEqual(['früh', 'spät']);
  });
});

describe('usesRightAxis', () => {
  it('true wenn mindestens eine Serie rechts', () => {
    expect(
      usesRightAxis([
        { yAxis: 'left' },
        { yAxis: 'right' },
      ]),
    ).toBe(true);
  });
  it('false wenn alle links', () => {
    expect(usesRightAxis([{ yAxis: 'left' }, { yAxis: 'left' }])).toBe(false);
  });
  it('false bei leerer Liste', () => {
    expect(usesRightAxis([])).toBe(false);
  });
});

describe('computeYDomain', () => {
  it('beide gesetzt → [min, max]', () => {
    expect(computeYDomain(80, 85)).toEqual([80, 85]);
  });
  it('nur Min → [min, auto]', () => {
    expect(computeYDomain(80, null)).toEqual([80, 'auto']);
  });
  it('nur Max → [auto, max]', () => {
    expect(computeYDomain(null, 85)).toEqual(['auto', 85]);
  });
  it('keiner gesetzt → undefined (kein domain-Prop)', () => {
    expect(computeYDomain(null, null)).toBeUndefined();
  });
});

describe('addPeriod', () => {
  it('DAILY addiert Tage', () => {
    expect(addPeriod('2026-05-28T00:00:00Z', 'DAILY', 2)).toBe('2026-05-30T00:00:00.000Z');
  });
  it('WEEKLY addiert Wochen (7 Tage)', () => {
    expect(addPeriod('2026-05-01T00:00:00Z', 'WEEKLY', 1)).toBe('2026-05-08T00:00:00.000Z');
  });
  it('MONTHLY addiert Monate', () => {
    expect(addPeriod('2026-01-15T00:00:00Z', 'MONTHLY', 2)).toBe('2026-03-15T00:00:00.000Z');
  });
  it('YEARLY addiert Jahre', () => {
    expect(addPeriod('2026-06-01T00:00:00Z', 'YEARLY', 3)).toBe('2029-06-01T00:00:00.000Z');
  });
});

describe('WidgetPlot Regression', () => {
  beforeEach(() => {
    aggregate.mockReset();
    entries.mockReset();
    list.mockReset();
    list.mockResolvedValue([]);
  });
  afterEach(() => cleanup());

  it('Drawer: Regression-Checkbox wird in der Config gespeichert', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValueOnce([]);
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, defaultGranularity: 'DAILY', regression: false })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await user.click(await screen.findByRole('checkbox', { name: 'Trend / Regression' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));
    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { regression: boolean };
    expect(parsed.regression).toBe(true);
  });

  it('aktivierte Regression mit nur 1 Bucket rendert ohne Crash', async () => {
    aggregate.mockResolvedValueOnce([
      { bucketStart: '2026-05-27T00:00:00Z', count: 1, min: 80, max: 80, avg: 80, last: 80 },
    ]);
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, defaultGranularity: 'DAILY', regression: true })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(42, 'DAILY'));
    // Kein Throw, Plot-Bereich vorhanden.
    expect(screen.getByLabelText('Plot-Bereich')).toBeInTheDocument();
  });

  it('Drawer: Y-Achse Min/Max werden in der Config gespeichert', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValueOnce([]);
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 42, defaultGranularity: 'DAILY' })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await user.type(await screen.findByLabelText('Y-Achse Min'), '80');
    await user.type(screen.getByLabelText('Y-Achse Max'), '85');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));
    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { yMin: number; yMax: number };
    expect(parsed.yMin).toBe(80);
    expect(parsed.yMax).toBe(85);
  });
});

describe('WidgetPlot Multi-Serie (#156)', () => {
  beforeEach(() => {
    aggregate.mockReset();
    entries.mockReset();
    list.mockReset();
    list.mockResolvedValue([]);
  });
  afterEach(() => cleanup());

  const SUMMARIES = [
    { id: 1, name: 'Gewicht', unit: 'kg', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
    { id: 2, name: 'Temperatur', unit: '°C', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
    { id: 3, name: 'Puls', unit: 'bpm', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
  ];

  it('lädt alle konfigurierten Serien (aggregiert)', async () => {
    aggregate.mockResolvedValue([]);
    render(
      <WidgetPlot
        widget={widget({
          series: [
            { timeSeriesId: 1, color: '#111' },
            { timeSeriesId: 2, color: '#222' },
            { timeSeriesId: 3, color: '#333' },
          ],
          defaultGranularity: 'DAILY',
        })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(1, 'DAILY'));
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(2, 'DAILY'));
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(3, 'DAILY'));
  });

  it('Legacy-Single-Serie (timeSeriesId) wird migriert und geladen', async () => {
    aggregate.mockResolvedValue([
      { bucketStart: '2026-05-27T00:00:00Z', count: 1, min: 1, max: 1, avg: 80, last: 1 },
    ]);
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 9, defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(9, 'DAILY'));
  });

  it('zwei Serien rendern ohne Crash, Plot-Bereich vorhanden (Legende an)', async () => {
    // recharts <Legend> rendert in jsdom keine Item-Texte (kein echtes Layout) — die
    // Namens-/Klick-Darstellung wird manuell am Testserver verifiziert. Hier prüfen wir,
    // dass die Multi-Serie mit aktivierter Legende fehlerfrei rendert.
    aggregate.mockResolvedValue([
      { bucketStart: '2026-05-27T00:00:00Z', count: 1, min: 1, max: 1, avg: 80, last: 1 },
    ]);
    list.mockResolvedValue(SUMMARIES);
    render(
      <WidgetPlot
        widget={widget({
          series: [
            { timeSeriesId: 1, color: '#111' },
            { timeSeriesId: 2, color: '#222' },
          ],
          defaultGranularity: 'DAILY',
          showLegend: true,
        })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(2, 'DAILY'));
    expect(screen.getByLabelText('Plot-Bereich')).toBeInTheDocument();
  });

  it('Drawer: zweite Serie + Legende werden gespeichert', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValue(SUMMARIES);
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({ series: [{ timeSeriesId: 1, color: '#1976d2' }], defaultGranularity: 'DAILY' })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    const addBtn = await screen.findByRole('button', { name: '+ Zeitreihe' });
    await user.click(addBtn);
    await user.click(screen.getByRole('checkbox', { name: 'Legende anzeigen' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));
    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as {
      series: { timeSeriesId: number; color: string }[];
      showLegend: boolean;
    };
    expect(parsed.series).toHaveLength(2);
    expect(parsed.showLegend).toBe(true);
  });

  it('Drawer: Overlays nur bei genau 1 Serie sichtbar', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValue(SUMMARIES);
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({
          series: [
            { timeSeriesId: 1, color: '#111' },
            { timeSeriesId: 2, color: '#222' },
          ],
          defaultGranularity: 'DAILY',
        })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await screen.findByText('Plot bearbeiten');
    // 2 Serien → kein Overlay-Block
    expect(screen.queryByText('Overlay-Linien')).not.toBeInTheDocument();
  });
});

describe('WidgetPlot getrennte Y-Achsen (#157)', () => {
  beforeEach(() => {
    aggregate.mockReset();
    entries.mockReset();
    list.mockReset();
    list.mockResolvedValue([]);
  });
  afterEach(() => cleanup());

  it('rendert zwei Achsen-Serien ohne Crash', async () => {
    aggregate.mockResolvedValue([
      { bucketStart: '2026-05-27T00:00:00Z', count: 1, min: 1, max: 1, avg: 80, last: 1 },
    ]);
    render(
      <WidgetPlot
        widget={widget({
          series: [
            { timeSeriesId: 1, color: '#111', yAxis: 'left' },
            { timeSeriesId: 2, color: '#222', yAxis: 'right' },
          ],
          defaultGranularity: 'DAILY',
        })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(2, 'DAILY'));
    expect(screen.getByLabelText('Plot-Bereich')).toBeInTheDocument();
  });

  it('Drawer: Achsen-Zuweisung wird gespeichert', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValue([
      { id: 1, name: 'Gewicht', unit: 'kg', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
    ]);
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({
          series: [{ timeSeriesId: 1, color: '#111', yAxis: 'left' }],
          defaultGranularity: 'DAILY',
        })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await user.click(await screen.findByLabelText('Achse'));
    await user.click(screen.getByRole('option', { name: 'Rechts' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));
    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { series: { yAxis: string }[] };
    expect(parsed.series[0].yAxis).toBe('right');
  });

  it('Legacy-Serie ohne yAxis-Feld → links (Abwärtskompatibilität)', async () => {
    aggregate.mockResolvedValue([]);
    const onChange = vi.fn();
    const user = userEvent.setup();
    list.mockResolvedValue([
      { id: 5, name: 'X', unit: 'u', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
    ]);
    render(
      <WidgetPlot
        widget={widget({ series: [{ timeSeriesId: 5, color: '#111' }], defaultGranularity: 'DAILY' })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));
    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { series: { yAxis: string }[] };
    expect(parsed.series[0].yAxis).toBe('left');
  });
});

describe('WidgetPlot Farb-Swatches (#165)', () => {
  beforeEach(() => {
    aggregate.mockReset();
    entries.mockReset();
    list.mockReset();
    list.mockResolvedValue([]);
  });
  afterEach(() => cleanup());

  it('Drawer: Klick auf Farb-Swatch setzt die Serienfarbe', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValue([
      { id: 1, name: 'Gewicht', unit: 'kg', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
    ]);
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({
          series: [{ timeSeriesId: 1, color: '#1976d2', yAxis: 'left' }],
          defaultGranularity: 'DAILY',
        })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await user.click(await screen.findByRole('button', { name: 'Farbe Rot' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));
    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { series: { color: string }[] };
    expect(parsed.series[0].color).toBe('#d32f2f');
  });

  it('aktiver Swatch ist als gewählt markiert (aria-pressed)', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValue([
      { id: 1, name: 'Gewicht', unit: 'kg', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
    ]);
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({
          series: [{ timeSeriesId: 1, color: '#2e7d32', yAxis: 'left' }],
          defaultGranularity: 'DAILY',
        })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    const green = await screen.findByRole('button', { name: 'Farbe Grün' });
    expect(green).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Farbe Blau' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });
});

describe('WidgetPlot Chart-Typen (#180)', () => {
  beforeEach(() => {
    aggregate.mockReset();
    entries.mockReset();
    list.mockReset();
    list.mockResolvedValue([]);
  });
  afterEach(() => cleanup());

  const SUMMARIES = [
    { id: 1, name: 'Gewicht', unit: 'kg', dataType: 'DECIMAL', entryCount: 1, createdAt: 'x', updatedAt: 'x' },
  ];
  const ONE_BUCKET = [
    { bucketStart: '2026-05-27T00:00:00Z', count: 1, min: 1, max: 1, avg: 80, last: 1 },
    { bucketStart: '2026-05-28T00:00:00Z', count: 1, min: 1, max: 1, avg: 82, last: 1 },
  ];

  it('parseChartType: fehlend/unbekannt → line, gültige Werte bleiben', () => {
    expect(parseChartType(undefined)).toBe('line');
    expect(parseChartType('quatsch')).toBe('line');
    expect(parseChartType('line')).toBe('line');
    expect(parseChartType('area')).toBe('area');
    expect(parseChartType('bar')).toBe('bar');
    expect(parseChartType('pie')).toBe('pie');
  });

  it('alte Config ohne chartType rendert ohne Crash (Default Linie)', async () => {
    aggregate.mockResolvedValue(ONE_BUCKET);
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 1, defaultGranularity: 'DAILY' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(1, 'DAILY'));
    expect(screen.getByLabelText('Plot-Bereich')).toBeInTheDocument();
  });

  it('Drawer: chartType wird in der Config gespeichert', async () => {
    aggregate.mockResolvedValue([]);
    list.mockResolvedValue(SUMMARIES);
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 1, defaultGranularity: 'DAILY' })}
        onChange={onChange}
        onDelete={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Plot bearbeiten' }));
    await user.click(await screen.findByRole('combobox', { name: 'Diagrammtyp' }));
    await user.click(await screen.findByRole('option', { name: 'Balken' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));
    const next = onChange.mock.calls[0][0] as WidgetDto;
    expect((JSON.parse(next.config) as { chartType: string }).chartType).toBe('bar');
  });

  it('BarChart rendert ohne Crash', async () => {
    aggregate.mockResolvedValue(ONE_BUCKET);
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 1, defaultGranularity: 'DAILY', chartType: 'bar' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(1, 'DAILY'));
    expect(screen.getByLabelText('Plot-Bereich')).toBeInTheDocument();
  });

  it('PieChart rendert ohne Crash', async () => {
    aggregate.mockResolvedValue(ONE_BUCKET);
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 1, defaultGranularity: 'DAILY', chartType: 'pie' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(1, 'DAILY'));
    expect(screen.getByLabelText('Plot-Bereich')).toBeInTheDocument();
  });

  it('AreaChart rendert ohne Crash', async () => {
    aggregate.mockResolvedValue(ONE_BUCKET);
    render(
      <WidgetPlot
        widget={widget({ timeSeriesId: 1, defaultGranularity: 'DAILY', chartType: 'area' })}
        onChange={() => undefined}
        onDelete={() => undefined}
      />,
    );
    await waitFor(() => expect(aggregate).toHaveBeenCalledWith(1, 'DAILY'));
    expect(screen.getByLabelText('Plot-Bereich')).toBeInTheDocument();
  });
});
