import { useEffect, useRef, useState } from 'react';
import type { PointerEvent } from 'react';
import { Box, Typography } from '@mui/material';

const MAX_DISPLAY_WIDTH = 640;

/** Die acht Greifpunkte: vier Ecken + vier Kantenmitten. */
export type ResizeHandle = 'nw' | 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w';

export const RESIZE_HANDLES: ResizeHandle[] = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w'];

export interface InteractiveResizeFrameProps {
  imageUrl: string;
  naturalWidth: number;
  naturalHeight: number;
  /** Aktuelle Zielmaße in Quell-Pixeln (kontrolliert). */
  width: number;
  height: number;
  /** Seitenverhältnis koppeln. */
  aspectLocked: boolean;
  onChange: (next: { width: number; height: number }) => void;
}

interface DragState {
  handle: ResizeHandle;
  startClientX: number;
  startClientY: number;
  startWidth: number;
  startHeight: number;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

/** Welche Achsen ein Griff beeinflusst und in welche Richtung (relativ zur fixen oberen-linken Ecke). */
function axisFactors(handle: ResizeHandle): { fx: number; fy: number } {
  const fx = handle.includes('e') ? 1 : handle.includes('w') ? -1 : 0;
  const fy = handle.includes('s') ? 1 : handle.includes('n') ? -1 : 0;
  return { fx, fy };
}

/**
 * Interaktiver Resize-Rahmen mit acht Greifpunkten (#198). Der Rahmen ist oben links verankert
 * (Position ist beim Verkleinern irrelevant); Ziehen an Ecken ändert beide Achsen, an Kanten eine.
 * Bei gekoppeltem Seitenverhältnis folgt die jeweils andere Achse. Maße in Quell-Pixeln.
 */
export default function InteractiveResizeFrame({
  imageUrl,
  naturalWidth,
  naturalHeight,
  width,
  height,
  aspectLocked,
  onChange,
}: InteractiveResizeFrameProps): JSX.Element {
  const dragRef = useRef<DragState | null>(null);
  const [dragging, setDragging] = useState(false);

  const displayWidth = Math.min(MAX_DISPLAY_WIDTH, naturalWidth);
  const scale = displayWidth / naturalWidth;
  const displayHeight = naturalHeight * scale;
  const aspect = naturalWidth / naturalHeight;

  const frameDisplayW = clamp(width * scale, 0, displayWidth);
  const frameDisplayH = clamp(height * scale, 0, displayHeight);

  // Move-/Up-Listener hängen während des Drags an `window`, nicht am Griff (#201): die 12px-Griffe
  // sind zu klein, als dass `pointermove` zuverlässig auf ihnen landet, sobald der Zeiger sie
  // verlässt — Pointer-Capture allein war hier nicht robust genug. Die exakt registrierten
  // Closures liegen im Ref, damit removeEventListener sie wieder trifft.
  const listenersRef = useRef<{ move: EventListener; up: EventListener } | null>(null);

  function detach(): void {
    const l = listenersRef.current;
    if (l) {
      window.removeEventListener('pointermove', l.move);
      window.removeEventListener('pointerup', l.up);
      window.removeEventListener('pointercancel', l.up);
      listenersRef.current = null;
    }
  }

  function applyMove(clientX: number, clientY: number): void {
    const drag = dragRef.current;
    if (!drag) return;
    const { fx, fy } = axisFactors(drag.handle);
    const dxNatural = (clientX - drag.startClientX) / scale;
    const dyNatural = (clientY - drag.startClientY) / scale;

    let nextWidth = drag.startWidth;
    let nextHeight = drag.startHeight;

    if (aspectLocked) {
      // Eine treibende Achse bestimmen: horizontale Griffe → Breite, sonst Höhe.
      if (fx !== 0) {
        nextWidth = clamp(drag.startWidth + fx * dxNatural, 1, naturalWidth);
        nextHeight = clamp(Math.round(nextWidth / aspect), 1, naturalHeight);
      } else if (fy !== 0) {
        nextHeight = clamp(drag.startHeight + fy * dyNatural, 1, naturalHeight);
        nextWidth = clamp(Math.round(nextHeight * aspect), 1, naturalWidth);
      }
    } else {
      if (fx !== 0) nextWidth = clamp(drag.startWidth + fx * dxNatural, 1, naturalWidth);
      if (fy !== 0) nextHeight = clamp(drag.startHeight + fy * dyNatural, 1, naturalHeight);
    }

    onChange({ width: Math.round(nextWidth), height: Math.round(nextHeight) });
  }

  const handlePointerDown = (handle: ResizeHandle) => (e: PointerEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    dragRef.current = {
      handle,
      startClientX: e.clientX,
      startClientY: e.clientY,
      startWidth: width,
      startHeight: height,
    };
    setDragging(true);
    detach();
    const move: EventListener = (ev) => {
      const pe = ev as globalThis.PointerEvent;
      applyMove(pe.clientX, pe.clientY);
    };
    const up: EventListener = () => {
      dragRef.current = null;
      setDragging(false);
      detach();
    };
    listenersRef.current = { move, up };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
    window.addEventListener('pointercancel', up);
  };

  // Bei Unmount mitten im Drag die globalen Listener sicher entfernen.
  useEffect(() => detach, []);

  return (
    <Box>
      <Box
        sx={{
          position: 'relative',
          width: displayWidth,
          height: displayHeight,
          mx: 'auto',
          userSelect: 'none',
          touchAction: 'none',
          // Gestrichelter Rahmen zeigt die Originalgröße als Referenz, während das Bild schrumpft.
          outline: '1px dashed',
          outlineColor: 'divider',
        }}
      >
        <Box
          data-testid="resize-frame"
          sx={{
            position: 'absolute',
            left: 0,
            top: 0,
            width: frameDisplayW,
            height: frameDisplayH,
            border: '2px solid',
            borderColor: 'primary.main',
            boxSizing: 'border-box',
          }}
        >
          {/* Das Bild selbst wird live auf die Zielmaße skaliert (stretcht bei entkoppeltem
              Seitenverhältnis sichtbar) — es IST der Rahmen, kein Overlay mehr (#201). */}
          <Box
            component="img"
            src={imageUrl}
            alt="Vorschau der Zielgröße"
            draggable={false}
            sx={{ width: '100%', height: '100%', display: 'block' }}
          />
          {RESIZE_HANDLES.map((h) => (
            <Box
              key={h}
              role="slider"
              tabIndex={0}
              aria-label={`Größe ändern (${h})`}
              aria-valuemin={1}
              aria-valuemax={naturalWidth}
              aria-valuenow={Math.round(width)}
              data-testid={`resize-handle-${h}`}
              onPointerDown={handlePointerDown(h)}
              sx={{
                position: 'absolute',
                width: 12,
                height: 12,
                bgcolor: 'primary.main',
                border: '1px solid white',
                borderRadius: '2px',
                cursor: dragging ? 'grabbing' : 'pointer',
                touchAction: 'none',
                ...handlePosition(h),
              }}
            />
          ))}
        </Box>
      </Box>
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{ display: 'block', textAlign: 'center', mt: 1 }}
      >
        Zielgröße: <strong>{Math.round(width)}×{Math.round(height)}</strong> px
      </Typography>
    </Box>
  );
}

/** Positioniert einen Griff am Rand des Rahmens (zentriert auf der jeweiligen Ecke/Kante). */
function handlePosition(handle: ResizeHandle): Record<string, string | number> {
  const style: Record<string, string | number> = {};
  if (handle.includes('n')) style.top = -6;
  if (handle.includes('s')) style.bottom = -6;
  if (handle.includes('e')) style.right = -6;
  if (handle.includes('w')) style.left = -6;
  // Kantenmitten in der freien Achse zentrieren.
  if (handle === 'n' || handle === 's') {
    style.left = 'calc(50% - 6px)';
  }
  if (handle === 'e' || handle === 'w') {
    style.top = 'calc(50% - 6px)';
  }
  return style;
}
