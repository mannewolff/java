import { describe, expect, it } from 'vitest';

import { relativeTime } from './relativeTime';

const NOW = Date.parse('2026-05-29T12:00:00Z');

describe('relativeTime', () => {
  it('formatiert wenige Sekunden in der Vergangenheit', () => {
    expect(relativeTime('2026-05-29T11:59:30Z', NOW)).toBe('vor 30 Sekunden');
  });

  it('formatiert Minuten', () => {
    expect(relativeTime('2026-05-29T11:45:00Z', NOW)).toBe('vor 15 Minuten');
  });

  it('formatiert Stunden', () => {
    expect(relativeTime('2026-05-29T09:00:00Z', NOW)).toBe('vor 3 Stunden');
  });

  it('formatiert Tage', () => {
    expect(relativeTime('2026-05-26T12:00:00Z', NOW)).toBe('vor 3 Tagen');
  });

  it('formatiert Monate', () => {
    expect(relativeTime('2026-03-30T12:00:00Z', NOW)).toBe('vor 2 Monaten');
  });

  it('formatiert Jahre', () => {
    expect(relativeTime('2024-05-29T12:00:00Z', NOW)).toBe('vor 2 Jahren');
  });

  it('formatiert die nahe Zukunft', () => {
    expect(relativeTime('2026-05-29T12:00:20Z', NOW)).toBe('in 20 Sekunden');
  });
});
