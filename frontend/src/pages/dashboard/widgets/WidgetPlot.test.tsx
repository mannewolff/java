import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetPlot, {
  addPeriod,
  forecastHorizon,
  linearRegression,
  overlayValue,
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
});
