import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import InteractiveCropFrame from './InteractiveCropFrame';

function setImageNaturalSize(img: HTMLImageElement, width: number, height: number): void {
  Object.defineProperty(img, 'naturalWidth', { configurable: true, get: () => width });
  Object.defineProperty(img, 'naturalHeight', { configurable: true, get: () => height });
}

/**
 * jsdom + RTL fireEvent.pointer* drop clientX/clientY off the event.
 * Dispatch a real MouseEvent under the pointer-event type instead — React
 * forwards it through the onPointer handler, and clientX/Y survive.
 */
function firePointer(
  target: Element,
  type: 'pointerdown' | 'pointermove' | 'pointerup' | 'pointercancel',
  init: { clientX: number; clientY: number; pointerId?: number },
): void {
  const event = new MouseEvent(type, {
    bubbles: true,
    cancelable: true,
    clientX: init.clientX,
    clientY: init.clientY,
  });
  Object.defineProperty(event, 'pointerId', { configurable: true, value: init.pointerId ?? 1 });
  fireEvent(target, event);
}

describe('InteractiveCropFrame', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders only the loading-placeholder image until natural dimensions are known', () => {
    render(
      <InteractiveCropFrame
        imageUrl="blob:mock"
        targetWidth={1200}
        targetHeight={630}
        xOffset={0.5}
        yOffset={0.5}
        onChange={() => {}}
      />,
    );

    expect(screen.getByAltText(/Original-Bild lädt/i)).toBeInTheDocument();
    expect(screen.queryByTestId('crop-frame')).not.toBeInTheDocument();
  });

  it('renders the drag frame once the image reports its natural size', () => {
    render(
      <InteractiveCropFrame
        imageUrl="blob:mock"
        targetWidth={1200}
        targetHeight={630}
        xOffset={0.5}
        yOffset={0.5}
        onChange={() => {}}
      />,
    );

    const img = screen.getByAltText(/Original-Bild lädt/i) as HTMLImageElement;
    setImageNaturalSize(img, 1200, 1500);
    fireEvent.load(img);

    const frame = screen.getByTestId('crop-frame');
    expect(frame).toBeInTheDocument();
    expect(frame).toHaveAttribute('aria-orientation', 'vertical');
  });

  it('emits a higher yOffset when the frame is dragged downwards on a tall source', () => {
    const onChange = vi.fn();
    render(
      <InteractiveCropFrame
        imageUrl="blob:mock"
        targetWidth={1200}
        targetHeight={630}
        xOffset={0.5}
        yOffset={0.5}
        onChange={onChange}
      />,
    );

    const img = screen.getByAltText(/Original-Bild lädt/i) as HTMLImageElement;
    setImageNaturalSize(img, 1200, 2400);
    fireEvent.load(img);

    const frame = screen.getByTestId('crop-frame');
    firePointer(frame, 'pointerdown', { clientX: 100, clientY: 100 });
    firePointer(frame, 'pointermove', { clientX: 100, clientY: 300 });
    firePointer(frame, 'pointerup', { clientX: 100, clientY: 300 });

    expect(onChange).toHaveBeenCalled();
    const last = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    expect(last.yOffset).toBeGreaterThan(0.5);
    expect(last.xOffset).toBe(0.5);
  });

  it('emits a higher xOffset when the frame is dragged right on a wide source', () => {
    const onChange = vi.fn();
    render(
      <InteractiveCropFrame
        imageUrl="blob:mock"
        targetWidth={1200}
        targetHeight={630}
        xOffset={0.5}
        yOffset={0.5}
        onChange={onChange}
      />,
    );

    const img = screen.getByAltText(/Original-Bild lädt/i) as HTMLImageElement;
    setImageNaturalSize(img, 4000, 1000);
    fireEvent.load(img);

    const frame = screen.getByTestId('crop-frame');
    expect(frame).toHaveAttribute('aria-orientation', 'horizontal');
    firePointer(frame, 'pointerdown', { clientX: 100, clientY: 100 });
    firePointer(frame, 'pointermove', { clientX: 250, clientY: 100 });
    firePointer(frame, 'pointerup', { clientX: 250, clientY: 100 });

    expect(onChange).toHaveBeenCalled();
    const last = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    expect(last.xOffset).toBeGreaterThan(0.5);
    expect(last.yOffset).toBe(0.5);
  });

  it('clamps offsets at the 0..1 boundary', () => {
    const onChange = vi.fn();
    render(
      <InteractiveCropFrame
        imageUrl="blob:mock"
        targetWidth={1200}
        targetHeight={630}
        xOffset={0.5}
        yOffset={0.5}
        onChange={onChange}
      />,
    );

    const img = screen.getByAltText(/Original-Bild lädt/i) as HTMLImageElement;
    setImageNaturalSize(img, 1200, 2400);
    fireEvent.load(img);

    const frame = screen.getByTestId('crop-frame');
    firePointer(frame, 'pointerdown', { clientX: 100, clientY: 100 });
    firePointer(frame, 'pointermove', { clientX: 100, clientY: 99999 });
    firePointer(frame, 'pointerup', { clientX: 100, clientY: 99999 });

    const last = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    expect(last.yOffset).toBe(1);
  });

  it('does not emit changes without a preceding pointer down', () => {
    const onChange = vi.fn();
    render(
      <InteractiveCropFrame
        imageUrl="blob:mock"
        targetWidth={1200}
        targetHeight={630}
        xOffset={0.5}
        yOffset={0.5}
        onChange={onChange}
      />,
    );

    const img = screen.getByAltText(/Original-Bild lädt/i) as HTMLImageElement;
    setImageNaturalSize(img, 1200, 2400);
    fireEvent.load(img);

    const frame = screen.getByTestId('crop-frame');
    firePointer(frame, 'pointermove', { clientX: 100, clientY: 300 });

    expect(onChange).not.toHaveBeenCalled();
  });
});
