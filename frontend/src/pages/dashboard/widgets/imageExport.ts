import type { ImageMode } from './WidgetImage';

export interface Size {
  width: number;
  height: number;
}

export interface SourceRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

function clamp01(v: number): number {
  return Math.min(1, Math.max(0, v));
}

/**
 * Tatsächliche Export-Größe (#192):
 * - Crop: Ausschnitt in Quell-Pixeln = Viewport-Größe, begrenzt auf die Bildgröße (1:1, objectFit none).
 * - Resize: das ganze Bild in Naturgröße (objectFit ist reine Anzeige, schneidet nichts weg).
 */
export function exportSize(mode: ImageMode, natural: Size, viewport: Size): Size {
  if (mode === 'crop') {
    return {
      width: Math.max(1, Math.min(Math.round(viewport.width), natural.width)),
      height: Math.max(1, Math.min(Math.round(viewport.height), natural.height)),
    };
  }
  return { width: natural.width, height: natural.height };
}

/**
 * Quell-Rechteck des Crop-Ausschnitts. Der Offset (0..1) verschiebt das Fenster über das Bild;
 * 0 = oben/links, 1 = unten/rechts. Wird auf gültige Bildgrenzen geklemmt.
 */
export function cropSourceRect(
  natural: Size,
  out: Size,
  offsetX: number,
  offsetY: number,
): SourceRect {
  const maxX = Math.max(0, natural.width - out.width);
  const maxY = Math.max(0, natural.height - out.height);
  return {
    x: Math.round(clamp01(offsetX) * maxX),
    y: Math.round(clamp01(offsetY) * maxY),
    width: out.width,
    height: out.height,
  };
}

/** Dateiname für den Download, z. B. widget-image-1717245000000.png. */
export function exportFilename(format: 'png' | 'jpeg', timestampMs: number): string {
  const ext = format === 'jpeg' ? 'jpg' : 'png';
  return `widget-image-${timestampMs}.${ext}`;
}
