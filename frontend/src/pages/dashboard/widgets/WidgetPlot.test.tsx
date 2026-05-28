import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetPlot from './WidgetPlot';
import type { WidgetDto } from '../../../api/dashboard';

vi.mock('../../../api/timeseries', () => ({
  aggregateTimeSeries: vi.fn(),
  listTimeSeries: vi.fn(),
}));

import { aggregateTimeSeries, listTimeSeries } from '../../../api/timeseries';

const aggregate = aggregateTimeSeries as ReturnType<typeof vi.fn>;
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
});
