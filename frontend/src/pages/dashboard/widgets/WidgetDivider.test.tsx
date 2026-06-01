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
        widget={widget(JSON.stringify({ color: '#ccc', thickness: 2 }))}
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
        widget={widget(JSON.stringify({ color: '', thickness: 4 }))}
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
        widget={widget(JSON.stringify({ color: '#ccc', thickness: 2 }))}
        onChange={vi.fn()}
        onDelete={onDelete}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Trennlinie löschen' }));
    expect(onDelete).toHaveBeenCalledTimes(1);
  });

  it('bietet keinen Orientierungs-Selektor mehr an', async () => {
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ color: '', thickness: 2 }))}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Trennlinie bearbeiten' }));
    expect(screen.queryByRole('combobox', { name: 'Orientierung' })).not.toBeInTheDocument();
  });

  it('rendert eine horizontale Linie (borderTop) mit Farbe und Breite', () => {
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ color: '#abc', thickness: 3 }))}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    const line = screen.getByLabelText('Trennlinie');
    expect(line).toHaveStyle('border-top: 3px solid #abc');
  });

  it('rendert persistierte vertical-Config gracefully als horizontale Linie', () => {
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ orientation: 'vertical', color: '#abc', thickness: 3 }))}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    const line = screen.getByLabelText('Trennlinie');
    expect(line).toHaveStyle('border-top: 3px solid #abc');
  });

  it('speichert Farbe, Breite und Darstellung aus dem Drawer mit horizontaler Grid-Größe', async () => {
    const onChange = vi.fn();
    render(
      <WidgetDivider
        widget={widget(JSON.stringify({ color: '', thickness: 2 }))}
        onChange={onChange}
        onDelete={vi.fn()}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Trennlinie bearbeiten' }));

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
      color: '#ff0000',
      thickness: 4,
      showBorder: true,
      backgroundColor: '#1e1e1e',
    });
    expect(saved).not.toHaveProperty('orientation');
    expect(savedWidget.width).toBe(6);
    expect(savedWidget.height).toBe(1);
  });
});
