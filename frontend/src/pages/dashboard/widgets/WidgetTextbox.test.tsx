import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetTextbox from './WidgetTextbox';
import type { WidgetDto } from '../../../api/dashboard';

function makeWidget(markdown = '# Test', overrides: Partial<WidgetDto> = {}): WidgetDto {
  return {
    id: 1,
    type: 'TEXTBOX',
    posX: 0,
    posY: 0,
    width: 4,
    height: 3,
    config: JSON.stringify({ markdown }),
    ...overrides,
  };
}

describe('WidgetTextbox', () => {
  afterEach(() => cleanup());

  it('rendert Markdown-Inhalt mit Heading und Fließtext', () => {
    render(
      <WidgetTextbox
        widget={makeWidget('# Hallo\n\nDies ist ein Test.')}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(screen.getByRole('heading', { level: 1, name: 'Hallo' })).toBeInTheDocument();
    expect(screen.getByText('Dies ist ein Test.')).toBeInTheDocument();
  });

  it('öffnet den Edit-Drawer und zeigt den aktuellen Markdown-Text in der Textarea', async () => {
    const user = userEvent.setup();
    render(
      <WidgetTextbox
        widget={makeWidget('Original-Text')}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Textbox bearbeiten' }));

    const textarea = screen.getByLabelText('Markdown-Quelltext') as HTMLTextAreaElement;
    expect(textarea.value).toBe('Original-Text');
  });

  it('ruft onChange mit aktualisierter Config beim Übernehmen', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetTextbox
        widget={makeWidget('Original')}
        onChange={onChange}
        onDelete={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Textbox bearbeiten' }));
    const textarea = screen.getByLabelText('Markdown-Quelltext');
    await user.clear(textarea);
    await user.type(textarea, 'Neuer Inhalt');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({
        config: JSON.stringify({ markdown: 'Neuer Inhalt' }),
      }),
    );
  });

  it('ruft onChange NICHT beim Abbrechen', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetTextbox
        widget={makeWidget('Original')}
        onChange={onChange}
        onDelete={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Textbox bearbeiten' }));
    const textarea = screen.getByLabelText('Markdown-Quelltext');
    await user.clear(textarea);
    await user.type(textarea, 'verworfen');
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onChange).not.toHaveBeenCalled();
  });

  it('ruft onDelete beim Klick auf das Lösch-Icon', async () => {
    const onDelete = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetTextbox widget={makeWidget()} onChange={vi.fn()} onDelete={onDelete} />,
    );

    await user.click(screen.getByRole('button', { name: 'Textbox löschen' }));

    expect(onDelete).toHaveBeenCalledTimes(1);
  });

  it('fängt invalide Config-JSON ab und rendert ohne Crash', () => {
    const widget: WidgetDto = {
      type: 'TEXTBOX',
      posX: 0,
      posY: 0,
      width: 4,
      height: 3,
      config: 'kein-valides-json',
    };

    render(<WidgetTextbox widget={widget} onChange={vi.fn()} onDelete={vi.fn()} />);

    // Edit-Button bleibt erreichbar — Komponente ist nicht gecrasht.
    expect(screen.getByRole('button', { name: 'Textbox bearbeiten' })).toBeInTheDocument();
  });
});
