import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetKpi from './WidgetKpi';
import type { WidgetDto } from '../../../api/dashboard';

vi.mock('../../../api/timeseries', () => ({
  getLatestEntry: vi.fn(),
  getTimeSeries: vi.fn(),
  listTimeSeries: vi.fn(),
}));

import { getLatestEntry, getTimeSeries, listTimeSeries } from '../../../api/timeseries';
import { ApiError } from '../../../api/client';

const latest = getLatestEntry as ReturnType<typeof vi.fn>;
const getTs = getTimeSeries as ReturnType<typeof vi.fn>;
const listTs = listTimeSeries as ReturnType<typeof vi.fn>;

interface KpiConfigInput {
  value?: number;
  label?: string;
  trend?: number | null;
  color?: 'neutral' | 'success' | 'warning' | 'error';
}

function makeWidget(config: KpiConfigInput = {}): WidgetDto {
  return {
    id: 1,
    type: 'KPI',
    posX: 0,
    posY: 0,
    width: 2,
    height: 2,
    config: JSON.stringify({
      value: config.value ?? 42,
      label: config.label ?? 'Active Users',
      trend: config.trend === undefined ? null : config.trend,
      color: config.color ?? 'neutral',
    }),
  };
}

describe('WidgetKpi', () => {
  beforeEach(() => {
    // Drawer ruft listTimeSeries beim Oeffnen — Default-Mock damit Style-Wechsel
    // funktioniert ohne explizit gemockt zu haben.
    listTs.mockResolvedValue([]);
  });
  afterEach(() => cleanup());

  it('rendert Wert und Label', () => {
    render(
      <WidgetKpi widget={makeWidget({ value: 1337, label: 'Visits' })} onChange={vi.fn()} onDelete={vi.fn()} />,
    );

    expect(screen.getByLabelText('KPI-Wert')).toHaveTextContent('1337');
    expect(screen.getByText('Visits')).toBeInTheDocument();
  });

  it('zeigt keinen Trend wenn trend null ist', () => {
    render(<WidgetKpi widget={makeWidget({ trend: null })} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(screen.queryByLabelText('KPI-Trend')).not.toBeInTheDocument();
  });

  it('zeigt einen Aufwärtspfeil bei positivem Trend', () => {
    render(<WidgetKpi widget={makeWidget({ trend: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    const trendBlock = screen.getByLabelText('KPI-Trend');
    expect(trendBlock).toHaveTextContent('5%');
    expect(trendBlock.querySelector('[data-testid="ArrowUpwardIcon"]')).toBeInTheDocument();
  });

  it('zeigt einen Abwärtspfeil bei negativem Trend', () => {
    render(<WidgetKpi widget={makeWidget({ trend: -3 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    const trendBlock = screen.getByLabelText('KPI-Trend');
    expect(trendBlock).toHaveTextContent('3%');
    expect(trendBlock.querySelector('[data-testid="ArrowDownwardIcon"]')).toBeInTheDocument();
  });

  it('öffnet den Drawer und speichert Änderungen mit Übernehmen', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetKpi widget={makeWidget({ value: 10, label: 'Old' })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'KPI bearbeiten' }));

    const labelInput = screen.getByLabelText('Label');
    await user.clear(labelInput);
    await user.type(labelInput, 'Neues Label');

    const valueInput = screen.getByLabelText('Wert');
    await user.clear(valueInput);
    await user.type(valueInput, '99.5');

    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    expect(onChange).toHaveBeenCalledTimes(1);
    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { value: number; label: string };
    expect(parsed.value).toBe(99.5);
    expect(parsed.label).toBe('Neues Label');
  });

  it('verwirft Änderungen beim Abbrechen', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetKpi widget={makeWidget()} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'KPI bearbeiten' }));
    const labelInput = screen.getByLabelText('Label');
    await user.clear(labelInput);
    await user.type(labelInput, 'verworfen');
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onChange).not.toHaveBeenCalled();
  });

  it('schreibt trend=null wenn das Trend-Feld geleert wird', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetKpi widget={makeWidget({ trend: 5 })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'KPI bearbeiten' }));
    const trendInput = screen.getByLabelText('Trend in % (leer = kein Trend)');
    await user.clear(trendInput);
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { trend: number | null };
    expect(parsed.trend).toBeNull();
  });

  it('ruft onDelete beim Klick auf das Lösch-Icon', async () => {
    const onDelete = vi.fn();
    const user = userEvent.setup();
    render(<WidgetKpi widget={makeWidget()} onChange={vi.fn()} onDelete={onDelete} />);

    await user.click(screen.getByRole('button', { name: 'KPI löschen' }));

    expect(onDelete).toHaveBeenCalledTimes(1);
  });

  it('fällt bei invalider Config auf neutrale Defaults zurück', () => {
    const widget: WidgetDto = {
      type: 'KPI',
      posX: 0,
      posY: 0,
      width: 2,
      height: 2,
      config: 'nicht-json',
    };

    render(<WidgetKpi widget={widget} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(screen.getByLabelText('KPI-Wert')).toHaveTextContent('0');
    expect(screen.queryByLabelText('KPI-Trend')).not.toBeInTheDocument();
  });

  // ----- Gauge-Sub-Type (#82) --------------------------------------------

  function gaugeWidget(over: Partial<{
    value: number;
    label: string;
    min: number;
    max: number;
    lowEnd: number;
    mediumEnd: number;
    rangeLabel: string;
  }> = {}): WidgetDto {
    return {
      id: 1,
      type: 'KPI',
      posX: 0,
      posY: 0,
      width: 2,
      height: 2,
      config: JSON.stringify({
        style: 'gauge',
        value: over.value ?? 50,
        label: over.label ?? '',
        min: over.min ?? 0,
        max: over.max ?? 100,
        lowEnd: over.lowEnd ?? 33,
        mediumEnd: over.mediumEnd ?? 66,
        rangeLabel: over.rangeLabel ?? '',
      }),
    };
  }

  it('Gauge: rendert drei Zonen und zentrale Prozentanzeige', () => {
    render(
      <WidgetKpi
        widget={gaugeWidget({ value: 50, min: 0, max: 100, lowEnd: 33, mediumEnd: 66 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('KPI-Gauge')).toBeInTheDocument();
    // Drei farbige Arc-Pfade — wir pruefen ueber die SVG-Path-Anzahl
    const svg = screen.getByRole('img', { name: 'Gauge' });
    expect(svg.querySelectorAll('path').length).toBe(3);
    expect(screen.getByText('50%')).toBeInTheDocument();
  });

  it('Gauge: hoher Wert ergibt 100% Anzeige', () => {
    render(
      <WidgetKpi
        widget={gaugeWidget({ value: 95, min: 0, max: 100, lowEnd: 33, mediumEnd: 66 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(screen.getByText('95%')).toBeInTheDocument();
  });

  it('Gauge: klammert Wert ueber Max auf 100%', () => {
    render(
      <WidgetKpi
        widget={gaugeWidget({ value: 9999, min: 0, max: 100, lowEnd: 33, mediumEnd: 66 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(screen.getByText('100%')).toBeInTheDocument();
  });

  it('Gauge: zeigt rangeLabel wenn gesetzt', () => {
    render(
      <WidgetKpi
        widget={gaugeWidget({ rangeLabel: '70% to 100%' })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(screen.getByText('70% to 100%')).toBeInTheDocument();
  });

  it('Gauge: Legacy ohne style-Feld rendert als Number (Regression)', () => {
    // KEY: kein style-Feld → defensiv "number"
    const legacyWidget: WidgetDto = {
      type: 'KPI',
      posX: 0,
      posY: 0,
      width: 2,
      height: 2,
      config: JSON.stringify({ value: 42, label: 'Legacy', color: 'success' }),
    };

    render(<WidgetKpi widget={legacyWidget} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(screen.getByLabelText('KPI-Wert')).toHaveTextContent('42');
    expect(screen.queryByLabelText('KPI-Gauge')).not.toBeInTheDocument();
  });

  it('Drawer: Style-Wechsel macht Gauge-Felder sichtbar', async () => {
    const user = userEvent.setup();
    render(<WidgetKpi widget={makeWidget()} onChange={vi.fn()} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'KPI bearbeiten' }));
    // Initial: number style — Farb-Akzent ist sichtbar
    expect(screen.getByLabelText('Farb-Akzent')).toBeInTheDocument();

    // Switch zu gauge
    await user.click(screen.getByLabelText('Darstellung'));
    await user.click(screen.getByRole('option', { name: 'Gauge (Tacho)' }));

    expect(screen.getByLabelText('Min')).toBeInTheDocument();
    expect(screen.getByLabelText('Max')).toBeInTheDocument();
    expect(screen.getByLabelText('Low-End')).toBeInTheDocument();
    expect(screen.getByLabelText('Medium-End')).toBeInTheDocument();
    expect(screen.queryByLabelText('Farb-Akzent')).not.toBeInTheDocument();
  });

  it('Drawer: Gauge-Style speichert Werte korrekt', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetKpi widget={gaugeWidget({ value: 50 })} onChange={onChange} onDelete={vi.fn()} />,
    );

    await user.click(screen.getByRole('button', { name: 'KPI bearbeiten' }));

    const valueField = screen.getByLabelText('Wert');
    await user.clear(valueField);
    await user.type(valueField, '75');

    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    expect(onChange).toHaveBeenCalled();
    const passed = JSON.parse(onChange.mock.calls[0][0].config);
    expect(passed.style).toBe('gauge');
    expect(passed.value).toBe(75);
  });

  // ----- TimeSeries-Sub-Type (#94) ---------------------------------------

  function tsWidget(over: Partial<{
    timeSeriesId: number | null;
    refreshSeconds: number;
    label: string;
  }> = {}): WidgetDto {
    return {
      id: 1,
      type: 'KPI',
      posX: 0,
      posY: 0,
      width: 2,
      height: 2,
      config: JSON.stringify({
        style: 'timeseries',
        timeSeriesId: over.timeSeriesId === undefined ? 42 : over.timeSeriesId,
        refreshSeconds: over.refreshSeconds ?? 30,
        label: over.label ?? '',
      }),
    };
  }

  describe('timeseries style', () => {
    beforeEach(() => {
      latest.mockReset();
      getTs.mockReset();
      listTs.mockReset();
      listTs.mockResolvedValue([]);
    });

    it('zeigt "Bitte Zeitreihe wählen" wenn keine konfiguriert', () => {
      render(
        <WidgetKpi
          widget={tsWidget({ timeSeriesId: null })}
          onChange={vi.fn()}
          onDelete={vi.fn()}
        />,
      );

      expect(screen.getByText(/Bitte Zeitreihe wählen/)).toBeInTheDocument();
      expect(latest).not.toHaveBeenCalled();
    });

    it('zeigt Lade-State initial', () => {
      latest.mockReturnValueOnce(new Promise(() => undefined));
      getTs.mockReturnValueOnce(new Promise(() => undefined));

      render(<WidgetKpi widget={tsWidget()} onChange={vi.fn()} onDelete={vi.fn()} />);

      expect(screen.getByLabelText('KPI-Loading')).toBeInTheDocument();
    });

    it('rendert Wert + Einheit + Series-Name nach erfolgreichem Fetch', async () => {
      latest.mockResolvedValueOnce({ id: 99, timestamp: 'x', value: 78.5 });
      getTs.mockResolvedValueOnce({
        id: 42,
        name: 'Gewicht',
        unit: 'kg',
        dataType: 'DECIMAL',
        entryCount: 5,
        createdAt: 'x',
        updatedAt: 'x',
      });

      render(<WidgetKpi widget={tsWidget()} onChange={vi.fn()} onDelete={vi.fn()} />);

      await waitFor(() =>
        expect(screen.getByLabelText('KPI-Wert')).toHaveTextContent('78.5'),
      );
      expect(screen.getByLabelText('KPI-Wert')).toHaveTextContent('kg');
      expect(screen.getByText('Gewicht')).toBeInTheDocument();
    });

    it('zeigt "Keine Daten" bei 404 ohne Crash', async () => {
      latest.mockRejectedValueOnce(new ApiError(404, 'not found', null));
      getTs.mockRejectedValueOnce(new ApiError(404, 'not found', null));

      render(<WidgetKpi widget={tsWidget()} onChange={vi.fn()} onDelete={vi.fn()} />);

      await waitFor(() => expect(screen.getByLabelText('KPI-No-Data')).toBeInTheDocument());
    });

    it('zeigt Error-Message bei 500', async () => {
      latest.mockRejectedValueOnce(new ApiError(500, 'boom', null));
      getTs.mockRejectedValueOnce(new ApiError(500, 'boom', null));

      render(<WidgetKpi widget={tsWidget()} onChange={vi.fn()} onDelete={vi.fn()} />);

      await waitFor(() => expect(screen.getByLabelText('KPI-Error')).toBeInTheDocument());
    });

    it('Drawer Style-Wechsel zu timeseries macht Refresh-Intervall sichtbar', async () => {
      listTs.mockResolvedValue([
        {
          id: 1,
          name: 'Weight',
          unit: 'kg',
          dataType: 'DECIMAL',
          entryCount: 0,
          createdAt: 'x',
          updatedAt: 'x',
        },
      ]);
      const user = userEvent.setup();
      render(<WidgetKpi widget={makeWidget()} onChange={vi.fn()} onDelete={vi.fn()} />);

      await user.click(screen.getByRole('button', { name: 'KPI bearbeiten' }));
      await user.click(screen.getByLabelText('Darstellung'));
      await user.click(screen.getByRole('option', { name: 'Zeitreihe' }));

      await waitFor(() =>
        expect(screen.getByLabelText(/Refresh-Intervall/)).toBeInTheDocument(),
      );
    });
  });

  // ----- Gauge + Zeitreihe (#123) ----------------------------------------

  describe('Gauge mit dynamischem Zeitreihenwert', () => {
    function gaugeWithTs(timeSeriesId: number, refreshSeconds = 60): WidgetDto {
      return {
        id: 1,
        type: 'KPI',
        posX: 0,
        posY: 0,
        width: 2,
        height: 2,
        config: JSON.stringify({
          style: 'gauge',
          value: 50,
          label: '',
          min: 0,
          max: 100,
          lowEnd: 33,
          mediumEnd: 66,
          rangeLabel: '',
          timeSeriesId,
          refreshSeconds,
        }),
      };
    }

    it('zeigt Ladeindikator beim ersten Fetch', () => {
      // getLatestEntry haengt unbegrenzt
      latest.mockReturnValue(new Promise(() => undefined));
      render(<WidgetKpi widget={gaugeWithTs(1)} onChange={vi.fn()} onDelete={vi.fn()} />);
      expect(screen.getByLabelText('KPI-Gauge-Loading')).toBeInTheDocument();
    });

    it('zeigt den geladenen Wert als Gauge-Prozentanzeige', async () => {
      latest.mockResolvedValue({ id: 1, timestamp: '2026-01-01T00:00:00Z', value: 75 });
      render(<WidgetKpi widget={gaugeWithTs(1)} onChange={vi.fn()} onDelete={vi.fn()} />);
      await waitFor(() => expect(screen.getByText('75%')).toBeInTheDocument());
      expect(screen.getByLabelText('KPI-Gauge')).toBeInTheDocument();
    });

    it('zeigt Fehlermeldung bei API-Fehler', async () => {
      latest.mockRejectedValue(new ApiError(500, 'Server-Fehler', null));
      render(<WidgetKpi widget={gaugeWithTs(1)} onChange={vi.fn()} onDelete={vi.fn()} />);
      await waitFor(() =>
        expect(screen.getByLabelText('KPI-Gauge-Error')).toBeInTheDocument(),
      );
    });

    it('Drawer: Zeitreihe-Dropdown erscheint im Gauge-Modus', async () => {
      listTs.mockResolvedValue([{ id: 7, name: 'Temperatur', unit: '°C' }]);
      const user = userEvent.setup();
      render(
        <WidgetKpi
          widget={gaugeWithTs(7)}
          onChange={vi.fn()}
          onDelete={vi.fn()}
        />,
      );
      // Drawer ist nicht in readOnly — aber gaugeWithTs ohne readOnly=false ist default
      // Wir öffnen ihn über den Edit-Button
      await user.click(screen.getByRole('button', { name: 'KPI bearbeiten' }));
      await waitFor(() =>
        expect(
          screen.getByLabelText(/Zeitreihe \(optional/),
        ).toBeInTheDocument(),
      );
    });
  });
});
