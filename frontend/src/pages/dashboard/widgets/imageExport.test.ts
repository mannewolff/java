import { describe, expect, it } from 'vitest';
import { cropSourceRect, exportFilename, exportSize } from './imageExport';

describe('exportSize (#192)', () => {
  it('crop: Ausschnitt = Viewport, begrenzt auf Bildgröße', () => {
    expect(exportSize('crop', { width: 1000, height: 1000 }, { width: 500, height: 500 })).toEqual({
      width: 500,
      height: 500,
    });
  });

  it('crop: Viewport größer als Bild → auf Bildgröße begrenzt', () => {
    expect(exportSize('crop', { width: 300, height: 200 }, { width: 500, height: 500 })).toEqual({
      width: 300,
      height: 200,
    });
  });

  it('crop: rundet den Viewport und erzwingt mindestens 1px', () => {
    expect(exportSize('crop', { width: 1000, height: 1000 }, { width: 499.6, height: 0 })).toEqual({
      width: 500,
      height: 1,
    });
  });

  it('resize: ganzes Bild in Naturgröße (Viewport egal)', () => {
    expect(exportSize('resize', { width: 1000, height: 800 }, { width: 200, height: 150 })).toEqual({
      width: 1000,
      height: 800,
    });
  });
});

describe('cropSourceRect (#192)', () => {
  it('Offset 0 → oben/links', () => {
    expect(
      cropSourceRect({ width: 1000, height: 1000 }, { width: 500, height: 500 }, 0, 0),
    ).toEqual({ x: 0, y: 0, width: 500, height: 500 });
  });

  it('Offset 1 → unten/rechts (geklemmt auf natural - out)', () => {
    expect(
      cropSourceRect({ width: 1000, height: 1000 }, { width: 500, height: 400 }, 1, 1),
    ).toEqual({ x: 500, y: 600, width: 500, height: 400 });
  });

  it('Offset 0.5 → mittig', () => {
    expect(
      cropSourceRect({ width: 1000, height: 1000 }, { width: 400, height: 400 }, 0.5, 0.5),
    ).toEqual({ x: 300, y: 300, width: 400, height: 400 });
  });

  it('Out größer als Bild → x/y = 0', () => {
    expect(
      cropSourceRect({ width: 300, height: 300 }, { width: 300, height: 300 }, 0.7, 0.7),
    ).toEqual({ x: 0, y: 0, width: 300, height: 300 });
  });
});

describe('exportFilename (#192)', () => {
  it('png-Endung', () => {
    expect(exportFilename('png', 1717245000000)).toBe('widget-image-1717245000000.png');
  });
  it('jpeg → .jpg', () => {
    expect(exportFilename('jpeg', 42)).toBe('widget-image-42.jpg');
  });
});
