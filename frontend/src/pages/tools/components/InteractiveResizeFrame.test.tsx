import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';

import InteractiveResizeFrame from './InteractiveResizeFrame';

function renderFrame(over: Partial<React.ComponentProps<typeof InteractiveResizeFrame>> = {}) {
  const onChange = vi.fn();
  render(
    <InteractiveResizeFrame
      imageUrl="blob:x"
      naturalWidth={1280}
      naturalHeight={640}
      width={1280}
      height={640}
      aspectLocked={false}
      onChange={onChange}
      {...over}
    />,
  );
  return onChange;
}

/**
 * jsdom + RTL fireEvent.pointer* verlieren clientX/clientY. Ein echtes MouseEvent unter dem
 * Pointer-Event-Typ behält die Koordinaten (gleiches Muster wie InteractiveCropFrame.test).
 */
function firePointer(
  target: Element | Window,
  type: 'pointerdown' | 'pointermove' | 'pointerup',
  init: { clientX: number; clientY: number },
): void {
  const event = new MouseEvent(type, {
    bubbles: true,
    cancelable: true,
    clientX: init.clientX,
    clientY: init.clientY,
  });
  Object.defineProperty(event, 'pointerId', { configurable: true, value: 1 });
  fireEvent(target as Element, event);
}

// scale = min(640, naturalWidth)/naturalWidth = 640/1280 = 0.5.
// pointerdown auf dem Griff, move/up auf `window` — so verhält sich der echte Browser, sobald der
// Zeiger den 12px-Griff verlässt (#201). Mit den alten Griff-Handlern wäre onChange nie gefeuert.
function drag(testId: string, dx: number, dy: number) {
  const handle = screen.getByTestId(testId);
  firePointer(handle, 'pointerdown', { clientX: 0, clientY: 0 });
  firePointer(window, 'pointermove', { clientX: dx, clientY: dy });
  firePointer(window, 'pointerup', { clientX: dx, clientY: dy });
}

describe('InteractiveResizeFrame (#198)', () => {
  afterEach(() => cleanup());

  it('rendert acht Greifpunkte und die Zielgröße', () => {
    renderFrame();
    ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w'].forEach((h) =>
      expect(screen.getByTestId(`resize-handle-${h}`)).toBeInTheDocument(),
    );
    expect(screen.getByText(/1280×640/)).toBeInTheDocument();
  });

  it('verkleinert die Breite über den rechten Kanten-Griff (1D, ohne Aspekt-Lock)', () => {
    const onChange = renderFrame({ width: 1280, height: 640 });
    // dx = -100 Anzeige-Px → -200 Quell-Px → Breite 1080, Höhe unverändert.
    drag('resize-handle-e', -100, 0);
    expect(onChange).toHaveBeenLastCalledWith({ width: 1080, height: 640 });
  });

  it('verkleinert die Höhe über den unteren Kanten-Griff (1D)', () => {
    const onChange = renderFrame({ width: 1280, height: 640 });
    drag('resize-handle-s', 0, -100);
    expect(onChange).toHaveBeenLastCalledWith({ width: 1280, height: 440 });
  });

  it('Eck-Griff ändert beide Achsen unabhängig (ohne Aspekt-Lock)', () => {
    const onChange = renderFrame({ width: 1280, height: 640 });
    drag('resize-handle-se', -100, -50);
    expect(onChange).toHaveBeenLastCalledWith({ width: 1080, height: 540 });
  });

  it('koppelt die Höhe an die Breite bei aktivem Aspekt-Lock', () => {
    const onChange = renderFrame({ width: 1280, height: 640, aspectLocked: true });
    // aspect = 2. Breite −200 → 1080, Höhe = round(1080/2) = 540.
    drag('resize-handle-e', -100, 0);
    expect(onChange).toHaveBeenLastCalledWith({ width: 1080, height: 540 });
  });

  it('klemmt auf mindestens 1 px', () => {
    const onChange = renderFrame({ width: 1280, height: 640 });
    // Extrem großes negatives dx → Breite würde negativ, wird auf 1 geklemmt.
    drag('resize-handle-e', -100000, 0);
    expect(onChange).toHaveBeenLastCalledWith({ width: 1, height: 640 });
  });
});
