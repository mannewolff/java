import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetDivider from './WidgetDivider';
import type { WidgetDto } from '../../../api/dashboard';

function widget(config: string): WidgetDto {
  return { id: 1, type: 'DIVIDER', posX: 0, posY: 0, width: 6, height: 1, config };
}

describe('WidgetDivider', () => {
  afterEach(() => {
    cleanup();
  });

  it('rendert die Linie und im Edit-Modus die Aktions-Icons', () => {
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ orientation: 'horizontal', color: '#ccc', thickness: 2 }))}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    expect(screen.getByLabelText('Trennlinie')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Trennlinie bearbeiten' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Trennlinie löschen' })).toBeInTheDocument();
  });

  it('zeigt im Read-Modus nur die Linie, keine Steuerelemente', () => {
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ orientation: 'vertical', color: '', thickness: 4 }))}
        onChange={vi.fn()}
        onDelete={vi.fn()}
        readOnly
      />,
    );
    expect(screen.getByLabelText('Trennlinie')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Trennlinie bearbeiten' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Trennlinie löschen' })).not.toBeInTheDocument();
  });

  it('fällt bei ungültiger Config auf Defaults zurück, ohne zu crashen', () => {
    render(
      <WidgetDivider widget={widget('not json')} onChange={vi.fn()} onDelete={vi.fn()} />,
    );
    expect(screen.getByLabelText('Trennlinie')).toBeInTheDocument();
  });

  it('ruft onDelete beim Klick auf das Lösch-Icon', async () => {
    const onDelete = vi.fn();
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ orientation: 'horizontal', color: '#ccc', thickness: 2 }))}
        onChange={vi.fn()}
        onDelete={onDelete}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Trennlinie löschen' }));
    expect(onDelete).toHaveBeenCalledTimes(1);
  });

  it('speichert Orientierung, Farbe, Breite und Darstellung aus dem Drawer', async () => {
    const onChange = vi.fn();
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ orientation: 'horizontal', color: '', thickness: 2 }))}
        onChange={onChange}
        onDelete={vi.fn()}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Trennlinie bearbeiten' }));

    // Orientierung auf Vertikal umstellen (MUI Select).
    await user.click(screen.getByRole('combobox', { name: 'Orientierung' }));
    await user.click(screen.getByRole('option', { name: 'Vertikal' }));

    const colorField = screen.getByLabelText('Linienfarbe (leer = Theme-Standard)');
    await user.clear(colorField);
    await user.type(colorField, '#ff0000');

    const thicknessField = screen.getByLabelText('Linienbreite (px)');
    await user.clear(thicknessField);
    await user.type(thicknessField, '4');

    await user.click(screen.getByRole('checkbox', { name: 'Rahmen anzeigen' }));

    const bgField = screen.getByLabelText('Hintergrundfarbe (leer = transparent)');
    await user.type(bgField, '#1e1e1e');

    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    expect(onChange).toHaveBeenCalledTimes(1);
    const savedWidget = onChange.mock.calls[0][0] as WidgetDto;
    const saved = JSON.parse(savedWidget.config) as Record<string, unknown>;
    expect(saved).toMatchObject({
      orientation: 'vertical',
      color: '#ff0000',
      thickness: 4,
      showBorder: true,
      backgroundColor: '#1e1e1e',
    });
    // Vertikal → Grid-Breite 1 (passt zwischen 6-Spalten-Widgets), Höhe 3.
    expect(savedWidget.width).toBe(1);
    expect(savedWidget.height).toBe(3);
  });

  it('Vertikal-Divider rendert eine seitliche Linie (borderLeft)', () => {
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ orientation: 'vertical', color: '#abc', thickness: 3 }))}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    const line = screen.getByLabelText('Trennlinie');
    expect(line).toHaveStyle('border-left: 3px solid #abc');
  });

  it('Umschalten auf Horizontal setzt die Grid-Größe auf 6×1 zurück', async () => {
    const onChange = vi.fn();
    render(
      <WidgetDivider
        widget={{
          id: 1,
          type: 'DIVIDER',
          posX: 0,
          posY: 0,
          width: 1,
          height: 3,
          config: JSON.stringify({ orientation: 'vertical', color: '', thickness: 2 }),
        }}
        onChange={onChange}
        onDelete={vi.fn()}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Trennlinie bearbeiten' }));
    await user.click(screen.getByRole('combobox', { name: 'Orientierung' }));
    await user.click(screen.getByRole('option', { name: 'Horizontal' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const savedWidget = onChange.mock.calls[0][0] as WidgetDto;
    expect(savedWidget.width).toBe(6);
    expect(savedWidget.height).toBe(1);
    expect(JSON.parse(savedWidget.config).orientation).toBe('horizontal');
  });
});
