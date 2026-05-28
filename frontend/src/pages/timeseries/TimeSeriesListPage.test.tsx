import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import TimeSeriesListPage from './TimeSeriesListPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

vi.mock('../../api/timeseries', () => ({
  listTimeSeries: vi.fn(),
  createTimeSeries: vi.fn(),
  deleteTimeSeries: vi.fn(),
}));

import {
  createTimeSeries,
  deleteTimeSeries,
  listTimeSeries,
  type TimeSeriesSummary,
} from '../../api/timeseries';

const list = listTimeSeries as ReturnType<typeof vi.fn>;
const create = createTimeSeries as ReturnType<typeof vi.fn>;
const del = deleteTimeSeries as ReturnType<typeof vi.fn>;

function ts(over: Partial<TimeSeriesSummary> = {}): TimeSeriesSummary {
  return {
    id: 1,
    name: 'Weight',
    description: 'Body weight',
    unit: 'kg',
    dataType: 'DECIMAL',
    entryCount: 0,
    createdAt: '2026-05-26T10:00:00Z',
    updatedAt: '2026-05-26T10:00:00Z',
    ...over,
  };
}

describe('TimeSeriesListPage', () => {
  beforeEach(() => {
    list.mockReset();
    create.mockReset();
    del.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  function render_() {
    return render(
      <MemoryRouter>
        <NotifyProvider>
          <TimeSeriesListPage />
        </NotifyProvider>
      </MemoryRouter>,
    );
  }

  it('shows loading skeletons initially', () => {
    list.mockReturnValueOnce(new Promise(() => undefined));

    render_();

    expect(screen.getByLabelText('Zeitreihen werden geladen')).toBeInTheDocument();
  });

  it('shows empty-state CTA when list is empty', async () => {
    list.mockResolvedValueOnce([]);

    render_();

    await waitFor(() =>
      expect(screen.getByText(/Noch keine Zeitreihen/)).toBeInTheDocument(),
    );
    expect(
      screen.getByRole('button', { name: 'Erste Zeitreihe anlegen' }),
    ).toBeInTheDocument();
  });

  it('renders entries with unit and dataType chips', async () => {
    list.mockResolvedValueOnce([
      ts({ id: 1, name: 'Weight', unit: 'kg', dataType: 'DECIMAL', entryCount: 12 }),
      ts({ id: 2, name: 'Schritte', unit: 'Anzahl', dataType: 'INTEGER', entryCount: 1 }),
    ]);

    render_();

    await waitFor(() => expect(screen.getByText('Weight')).toBeInTheDocument());
    expect(screen.getByText('Schritte')).toBeInTheDocument();
    expect(screen.getByText('kg')).toBeInTheDocument();
    expect(screen.getByText('Anzahl')).toBeInTheDocument();
    expect(screen.getAllByText('Dezimal').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Ganzzahl').length).toBeGreaterThan(0);
    expect(screen.getByText(/12 Einträge/)).toBeInTheDocument();
    expect(screen.getByText(/1 Eintrag/)).toBeInTheDocument();
  });

  it('opens create modal with required fields and submits', async () => {
    list.mockResolvedValueOnce([]);
    create.mockResolvedValueOnce(ts({ id: 99, name: 'Neu', unit: 'kg' }));

    render_();
    await waitFor(() =>
      expect(screen.getByText(/Noch keine Zeitreihen/)).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Neue Zeitreihe anlegen' }));

    await user.type(screen.getByLabelText(/^Name/), 'Gewicht');
    await user.type(screen.getByLabelText(/^Einheit/), 'kg');

    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        name: 'Gewicht',
        description: undefined,
        unit: 'kg',
        dataType: 'DECIMAL',
      }),
    );
  });

  it('disables submit when required fields are empty', async () => {
    list.mockResolvedValueOnce([]);

    render_();
    await waitFor(() =>
      expect(screen.getByText(/Noch keine Zeitreihen/)).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Neue Zeitreihe anlegen' }));

    const submit = screen.getByRole('button', { name: 'Anlegen' });
    expect(submit).toBeDisabled();
  });

  it('asks for confirmation before deleting and calls API on confirm', async () => {
    list
      .mockResolvedValueOnce([ts({ id: 7, name: 'Bye' })])
      .mockResolvedValueOnce([]);
    del.mockResolvedValueOnce(undefined);

    render_();
    await waitFor(() => expect(screen.getByText('Bye')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Zeitreihe Bye löschen' }));
    expect(screen.getByText(/Zeitreihe löschen\?/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Löschen' }));
    await waitFor(() => expect(del).toHaveBeenCalledWith(7));
  });

  it('cancels delete without calling API', async () => {
    list.mockResolvedValueOnce([ts({ id: 7, name: 'Bye' })]);

    render_();
    await waitFor(() => expect(screen.getByText('Bye')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Zeitreihe Bye löschen' }));
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(del).not.toHaveBeenCalled();
  });

  it('shows error alert on list failure', async () => {
    list.mockRejectedValueOnce(new Error('boom'));

    render_();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Unbekannter Fehler/),
    );
  });
});
