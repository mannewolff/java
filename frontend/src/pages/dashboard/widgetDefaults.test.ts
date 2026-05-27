import { describe, expect, it } from 'vitest';
import { GRID_MARGIN, GRID_ROW_HEIGHT, pxToRows } from './widgetDefaults';

describe('pxToRows', () => {
  it('mappt 0 px auf mindestens 1 Row', () => {
    expect(pxToRows(0)).toBe(1);
  });

  it('passt genau in 1 Row, wenn die Höhe einer Row entspricht', () => {
    expect(pxToRows(GRID_ROW_HEIGHT)).toBe(1);
  });

  it('braucht 2 Rows, sobald die Inhalts-Höhe 1 Row + 1 Margin überschreitet', () => {
    // 1-Row-Höhe = 40, 2-Row-Höhe = 90 (40+10+40). Alles dazwischen → 2 Rows.
    expect(pxToRows(GRID_ROW_HEIGHT + GRID_MARGIN + 1)).toBe(2);
  });

  it('rundet beliebige Höhen auf die nächste passende Row-Anzahl auf', () => {
    // 200 px → benötigt N mit 50N - 10 >= 200 → N >= 4.2 → 5.
    expect(pxToRows(200)).toBe(5);
  });
});
