import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetKpi from './WidgetKpi';
import type { WidgetDto } from '../../../api/dashboard';

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
});
