import { describe, expect, it } from 'vitest';
import { ARCHIVED_STATUS_COLOR, COLUMN_SURFACE_BG, STATUS_COLORS } from './statusColors';
import { KANBAN_COLUMNS } from '../../api/kanban';

describe('STATUS_COLORS', () => {
  it('enthält für jede der fünf Spalten ein vollständiges Farb-Set', () => {
    for (const column of KANBAN_COLUMNS) {
      const set = STATUS_COLORS[column];
      expect(set.bg).toMatch(/^#[0-9a-f]{6}$/i);
      expect(set.text).toMatch(/^#[0-9a-f]{6}$/i);
      expect(set.dot).toMatch(/^#[0-9a-f]{6}$/i);
    }
  });

  it('vergibt für jede Spalte eine eigene Hintergrundfarbe (keine Duplikate)', () => {
    const backgrounds = KANBAN_COLUMNS.map((c) => STATUS_COLORS[c].bg);
    expect(new Set(backgrounds).size).toBe(KANBAN_COLUMNS.length);
  });
});

describe('ARCHIVED_STATUS_COLOR', () => {
  it('liefert ein vollständiges Farb-Set für archivierte Items', () => {
    expect(ARCHIVED_STATUS_COLOR.bg).toMatch(/^#[0-9a-f]{6}$/i);
    expect(ARCHIVED_STATUS_COLOR.text).toMatch(/^#[0-9a-f]{6}$/i);
    expect(ARCHIVED_STATUS_COLOR.dot).toMatch(/^#[0-9a-f]{6}$/i);
  });
});

describe('COLUMN_SURFACE_BG', () => {
  it('ist eine gültige Hex-Farbe', () => {
    expect(COLUMN_SURFACE_BG).toMatch(/^#[0-9a-f]{6}$/i);
  });
});
