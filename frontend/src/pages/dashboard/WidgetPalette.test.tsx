import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';

import WidgetPalette from './WidgetPalette';

describe('WidgetPalette', () => {
  afterEach(() => cleanup());

  it('rendert eine Kachel pro Widget-Typ mit Label', () => {
    render(<WidgetPalette onDragStartWidget={vi.fn()} />);

    expect(screen.getByText('Textbox')).toBeInTheDocument();
    expect(screen.getByText('KPI')).toBeInTheDocument();
  });

  it('ruft onDragStartWidget mit dem korrekten Typ beim Drag-Start einer Kachel', () => {
    const onDragStart = vi.fn();
    render(<WidgetPalette onDragStartWidget={onDragStart} />);

    const textboxTile = screen.getByLabelText('Widget Textbox hinzufügen');
    // dataTransfer-Objekt minimal mocken, sonst wirft jsdom.
    const dataTransfer = {
      setData: vi.fn(),
      effectAllowed: '',
    };
    fireEvent.dragStart(textboxTile, { dataTransfer });

    expect(onDragStart).toHaveBeenCalledWith('TEXTBOX');
    expect(dataTransfer.setData).toHaveBeenCalledWith('text/plain', 'TEXTBOX');
  });

  it('signalisiert die Kachel als draggable', () => {
    render(<WidgetPalette onDragStartWidget={vi.fn()} />);

    const textboxTile = screen.getByLabelText('Widget Textbox hinzufügen');
    expect(textboxTile).toHaveAttribute('draggable', 'true');
  });
});
