import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import TimeSeriesDetailPage from './TimeSeriesDetailPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

vi.mock('../../api/timeseries', () => ({
  getTimeSeries: vi.fn(),
  updateTimeSeries: vi.fn(),
  listEntries: vi.fn(),
  addEntry: vi.fn(),
}));

import {
  addEntry,
  getTimeSeries,
  listEntries,
  updateTimeSeries,
  type TimeSeriesSummary,
  type TimeSeriesEntry,
} from '../../api/timeseries';

const get = getTimeSeries as ReturnType<typeof vi.fn>;
const upd = updateTimeSeries as ReturnType<typeof vi.fn>;
const listE = listEntries as ReturnType<typeof vi.fn>;
const addE = addEntry as ReturnType<typeof vi.fn>;

function summary(over: Partial<TimeSeriesSummary> = {}): TimeSeriesSummary {
  return {
    id: 42,
    name: 'Weight',
    description: 'Body weight',
    unit: 'kg',
    dataType: 'DECIMAL',
    entryCount: 2,
    createdAt: '2026-05-26T10:00:00Z',
    updatedAt: '2026-05-26T10:00:00Z',
    ...over,
  };
}

function entry(id: number, value: number, ts: string): TimeSeriesEntry {
  return { id, value, timestamp: ts };
}

describe('TimeSeriesDetailPage', () => {
  beforeEach(() => {
    get.mockReset();
    upd.mockReset();
    listE.mockReset();
    addE.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  function render_() {
    return render(
      <MemoryRouter initialEntries={['/timeseries/42']}>
        <NotifyProvider>
          <Routes>
            <Route path="/timeseries/:id" element={<TimeSeriesDetailPage />} />
          </Routes>
        </NotifyProvider>
      </MemoryRouter>,
    );
  }

  it('renders summary header, unit and dataType chips', async () => {
    get.mockResolvedValueOnce(summary());
    listE.mockResolvedValueOnce([]);

    render_();

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Weight' })).toBeInTheDocument(),
    );
    expect(screen.getByText('kg')).toBeInTheDocument();
    expect(screen.getByText('Dezimal')).toBeInTheDocument();
    expect(screen.getByText('Body weight')).toBeInTheDocument();
  });

  it('renders entries table with formatted values', async () => {
    get.mockResolvedValueOnce(summary({ entryCount: 2 }));
    listE.mockResolvedValueOnce([
      entry(2, 78.5, '2026-05-27T12:00:00Z'),
      entry(1, 78.2, '2026-05-26T08:30:00Z'),
    ]);

    render_();

    await waitFor(() => expect(screen.getByText('78.5')).toBeInTheDocument());
    expect(screen.getByText('78.2')).toBeInTheDocument();
    expect(screen.getByText('Einträge (2)')).toBeInTheDocument();
  });

  it('shows empty state when no entries', async () => {
    get.mockResolvedValueOnce(summary({ entryCount: 0 }));
    listE.mockResolvedValueOnce([]);

    render_();

    await waitFor(() =>
      expect(screen.getByText(/Noch keine Einträge/)).toBeInTheDocument(),
    );
  });

  it('inline-edits the name and persists via updateTimeSeries', async () => {
    get
      .mockResolvedValueOnce(summary({ name: 'Old' }))
      .mockResolvedValueOnce(summary({ name: 'New' }));
    listE.mockResolvedValue([]);
    upd.mockResolvedValueOnce(summary({ name: 'New' }));

    render_();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Old' })).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Namen bearbeiten' }));

    const field = screen.getByLabelText('Zeitreihen-Name');
    await user.clear(field);
    await user.type(field, 'New');

    await user.click(screen.getByRole('button', { name: 'Namen speichern' }));

    await waitFor(() =>
      expect(upd).toHaveBeenCalledWith(42, {
        name: 'New',
        description: 'Body weight',
        unit: 'kg',
        dataType: 'DECIMAL',
      }),
    );
  });

  it('cancels inline name edit without API call', async () => {
    get.mockResolvedValueOnce(summary());
    listE.mockResolvedValue([]);

    render_();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Weight' })).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Namen bearbeiten' }));
    await user.click(screen.getByRole('button', { name: 'Bearbeitung abbrechen' }));

    expect(upd).not.toHaveBeenCalled();
    // After cancel original name is back as heading
    expect(screen.getByRole('heading', { name: 'Weight' })).toBeInTheDocument();
  });

  it('adds a decimal entry through the form', async () => {
    get.mockResolvedValue(summary());
    listE.mockResolvedValue([]);
    addE.mockResolvedValueOnce(entry(99, 78.5, '2026-05-27T12:00:00Z'));

    render_();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Weight' })).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Wert'), '78.5');
    await user.click(screen.getByRole('button', { name: 'Eintragen' }));

    await waitFor(() => expect(addE).toHaveBeenCalled());
    expect(addE.mock.calls[0][0]).toBe(42);
    expect(addE.mock.calls[0][2]).toBe(78.5);
  });

  it('rejects decimals on INTEGER series', async () => {
    get.mockResolvedValueOnce(summary({ dataType: 'INTEGER', unit: 'Anzahl' }));
    listE.mockResolvedValueOnce([]);

    render_();
    await waitFor(() => expect(screen.getByText('Ganzzahl')).toBeInTheDocument());

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Wert'), '78.5');
    await user.click(screen.getByRole('button', { name: 'Eintragen' }));

    await waitFor(() =>
      expect(
        screen.getByText(/keine Nachkommastellen erlaubt/i),
      ).toBeInTheDocument(),
    );
    expect(addE).not.toHaveBeenCalled();
  });

  it('rejects non-numeric input', async () => {
    get.mockResolvedValueOnce(summary());
    listE.mockResolvedValueOnce([]);

    render_();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Weight' })).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Wert'), 'foo');
    await user.click(screen.getByRole('button', { name: 'Eintragen' }));

    await waitFor(() =>
      expect(screen.getByText(/Keine gültige Zahl/i)).toBeInTheDocument(),
    );
    expect(addE).not.toHaveBeenCalled();
  });

  it('shows error alert on load failure', async () => {
    get.mockRejectedValueOnce(new Error('boom'));
    listE.mockRejectedValueOnce(new Error('boom'));

    render_();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Unbekannter Fehler/),
    );
  });
});
