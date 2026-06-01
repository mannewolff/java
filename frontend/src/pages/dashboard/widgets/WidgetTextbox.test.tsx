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
        config: JSON.stringify({
          markdown: 'Neuer Inhalt',
          paddingTop: 2,
          paddingLeft: 2,
          paddingRight: 2,
          paddingBottom: 2,
          showBorder: false,
        }),
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

  it('meldet im Read-Modus die scrollHeight des Containers an onContentHeight (Auto-Resize)', () => {
    // jsdom hat keinen ResizeObserver — wir stellen einen Stub, der nach `observe`
    // synchron feuert. Damit testen wir, dass WidgetTextbox die scrollHeight an
    // den Callback durchreicht.
    let captured: ResizeObserverCallback | null = null;
    type RO = {
      observe: (t: Element) => void;
      disconnect: () => void;
      unobserve: (t: Element) => void;
    };
    class StubResizeObserver implements RO {
      constructor(cb: ResizeObserverCallback) {
        captured = cb;
      }
      observe = vi.fn();
      disconnect = vi.fn();
      unobserve = vi.fn();
    }
    const original = (globalThis as { ResizeObserver?: typeof ResizeObserver }).ResizeObserver;
    (globalThis as unknown as { ResizeObserver: typeof StubResizeObserver }).ResizeObserver =
      StubResizeObserver;

    const onContentHeight = vi.fn();
    render(
      <WidgetTextbox
        widget={makeWidget('# Langer Text\n\nNoch mehr Inhalt der scrollt.')}
        onChange={vi.fn()}
        onDelete={vi.fn()}
        readOnly
        onContentHeight={onContentHeight}
      />,
    );

    // ResizeObserver ist instanziiert. scrollHeight ist in jsdom 0, der Aufruf erfolgt aber.
    expect(captured).not.toBeNull();
    // Simuliere ein Resize-Event — der Stub-Callback ruft intern Element.scrollHeight ab.
    captured!([], {} as ResizeObserver);
    expect(onContentHeight).toHaveBeenCalled();
    expect(typeof onContentHeight.mock.calls[0][0]).toBe('number');

    if (original) {
      (globalThis as unknown as { ResizeObserver: typeof ResizeObserver }).ResizeObserver =
        original;
    } else {
      delete (globalThis as { ResizeObserver?: unknown }).ResizeObserver;
    }
  });

  it('startet keinen ResizeObserver, wenn nicht readOnly ist', () => {
    const observeSpy = vi.fn();
    class StubResizeObserver {
      observe = observeSpy;
      disconnect = vi.fn();
      unobserve = vi.fn();
    }
    const original = (globalThis as { ResizeObserver?: typeof ResizeObserver }).ResizeObserver;
    (globalThis as unknown as { ResizeObserver: typeof StubResizeObserver }).ResizeObserver =
      StubResizeObserver;

    render(
      <WidgetTextbox
        widget={makeWidget('Text')}
        onChange={vi.fn()}
        onDelete={vi.fn()}
        // editMode → kein readOnly, kein Auto-Resize
        onContentHeight={vi.fn()}
      />,
    );

    expect(observeSpy).not.toHaveBeenCalled();

    if (original) {
      (globalThis as unknown as { ResizeObserver: typeof ResizeObserver }).ResizeObserver =
        original;
    } else {
      delete (globalThis as { ResizeObserver?: unknown }).ResizeObserver;
    }
  });

  // ----- Darstellung / Lese-Modus (#122) ---------------------------------

  it('Lese-Modus ohne showBorder: Paper ist nicht outlined', () => {
    const { container } = render(
      <WidgetTextbox widget={makeWidget('Text')} onChange={vi.fn()} onDelete={vi.fn()} readOnly />,
    );

    const paper = container.querySelector('.MuiPaper-root');
    expect(paper).not.toBeNull();
    expect(paper).not.toHaveClass('MuiPaper-outlined');
  });

  it('Lese-Modus mit showBorder: Paper ist outlined', () => {
    const { container } = render(
      <WidgetTextbox
        widget={makeWidget('Text', {
          config: JSON.stringify({ markdown: 'Text', showBorder: true }),
        })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
        readOnly
      />,
    );

    expect(container.querySelector('.MuiPaper-root')).toHaveClass('MuiPaper-outlined');
  });

  it('Lese-Modus mit backgroundColor: setzt bgcolor des Papers', () => {
    const { container } = render(
      <WidgetTextbox
        widget={makeWidget('Text', {
          config: JSON.stringify({ markdown: 'Text', backgroundColor: 'rgb(20, 30, 40)' }),
        })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
        readOnly
      />,
    );

    expect(container.querySelector('.MuiPaper-root')).toHaveStyle({
      backgroundColor: 'rgb(20, 30, 40)',
    });
  });

  it('Edit-Modus: Paper bleibt outlined', () => {
    const { container } = render(
      <WidgetTextbox widget={makeWidget('Text')} onChange={vi.fn()} onDelete={vi.fn()} />,
    );

    expect(container.querySelector('.MuiPaper-root')).toHaveClass('MuiPaper-outlined');
  });

  it('Drawer: speichert showBorder und backgroundColor in der Config', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetTextbox widget={makeWidget('Text')} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Textbox bearbeiten' }));
    await user.click(screen.getByLabelText('Rahmen anzeigen'));
    await user.type(screen.getByLabelText('Hintergrundfarbe (leer = transparent)'), '#123456');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const next = onChange.mock.calls[0][0] as WidgetDto;
    const parsed = JSON.parse(next.config) as { showBorder: boolean; backgroundColor: string };
    expect(parsed.showBorder).toBe(true);
    expect(parsed.backgroundColor).toBe('#123456');
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

describe('WidgetTextbox Abstände (#171)', () => {
  afterEach(() => cleanup());

  it('Drawer-Felder zeigen Default-Padding 2 bei Config ohne Padding', async () => {
    const user = userEvent.setup();
    render(
      <WidgetTextbox widget={makeWidget('x')} onChange={vi.fn()} onDelete={vi.fn()} />,
    );
    await user.click(screen.getByRole('button', { name: 'Textbox bearbeiten' }));
    expect(screen.getByLabelText('Abstand oben')).toHaveValue(2);
    expect(screen.getByLabelText('Abstand links')).toHaveValue(2);
    expect(screen.getByLabelText('Abstand rechts')).toHaveValue(2);
    expect(screen.getByLabelText('Abstand unten')).toHaveValue(2);
  });

  it('speichert die vier Abstände aus dem Drawer', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetTextbox widget={makeWidget('x')} onChange={onChange} onDelete={vi.fn()} />,
    );
    await user.click(screen.getByRole('button', { name: 'Textbox bearbeiten' }));

    const top = screen.getByLabelText('Abstand oben');
    await user.clear(top);
    await user.type(top, '0');
    const right = screen.getByLabelText('Abstand rechts');
    await user.clear(right);
    await user.type(right, '4');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as {
      paddingTop: number;
      paddingRight: number;
      paddingLeft: number;
      paddingBottom: number;
    };
    expect(parsed.paddingTop).toBe(0);
    expect(parsed.paddingRight).toBe(4);
    expect(parsed.paddingLeft).toBe(2);
    expect(parsed.paddingBottom).toBe(2);
  });

  it('klemmt zu große Werte auf das Maximum (8)', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <WidgetTextbox widget={makeWidget('x')} onChange={onChange} onDelete={vi.fn()} />,
    );
    await user.click(screen.getByRole('button', { name: 'Textbox bearbeiten' }));
    const top = screen.getByLabelText('Abstand oben');
    await user.clear(top);
    await user.type(top, '99');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as {
      paddingTop: number;
    };
    expect(parsed.paddingTop).toBe(8);
  });
});
