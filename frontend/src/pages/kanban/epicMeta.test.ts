import { describe, expect, it } from 'vitest';

import { EPIC_PALETTE, epicColor, epicShortcode } from './epicMeta';

describe('epicShortcode', () => {
  it('bildet die Initialen der ersten drei Titelwörter (Großbuchstaben)', () => {
    expect(epicShortcode('10-Tage Workshop IT-Bildungshaus')).toBe('1WI');
  });

  it('begrenzt auf drei Initialen', () => {
    expect(epicShortcode('Alpha Beta Gamma Delta')).toBe('ABG');
  });

  it('fällt bei leerem/whitespace-Titel auf EPIC zurück', () => {
    expect(epicShortcode('   ')).toBe('EPIC');
    expect(epicShortcode('')).toBe('EPIC');
  });
});

describe('epicColor', () => {
  it('liefert eine Farbe aus der Palette', () => {
    expect(EPIC_PALETTE).toContain(epicColor(1));
  });

  it('ist deterministisch pro ID', () => {
    expect(epicColor(42)).toBe(epicColor(42));
  });

  it('verteilt unterschiedliche IDs (nicht alle gleich)', () => {
    const colors = new Set([epicColor(1), epicColor(2), epicColor(3), epicColor(4)]);
    expect(colors.size).toBeGreaterThan(1);
  });
});
