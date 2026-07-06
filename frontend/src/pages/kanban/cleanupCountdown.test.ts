import { describe, expect, it } from 'vitest';
import { cleanupCountdownLabel, cleanupDaysRemaining } from './cleanupCountdown';

describe('cleanupDaysRemaining', () => {
  it('liefert die volle Retention, wenn das Item gerade eben nach DONE wandert', () => {
    const now = Date.parse('2026-01-10T12:00:00Z');
    const movedToDoneAt = '2026-01-10T12:00:00Z';
    expect(cleanupDaysRemaining(movedToDoneAt, 5, now)).toBe(5);
  });

  it('zaehlt sich pro angefangenem Tag nach unten', () => {
    const now = Date.parse('2026-01-12T12:00:00Z'); // 2 Tage drin
    const movedToDoneAt = '2026-01-10T12:00:00Z';
    expect(cleanupDaysRemaining(movedToDoneAt, 5, now)).toBe(3);
  });

  it('kappt bei 0, wenn die Retention abgelaufen ist', () => {
    const now = Date.parse('2026-01-20T12:00:00Z');
    const movedToDoneAt = '2026-01-10T12:00:00Z';
    expect(cleanupDaysRemaining(movedToDoneAt, 5, now)).toBe(0);
  });

  it('verwendet Math.ceil — Bruchteile zählen als Tag', () => {
    const movedToDoneAt = '2026-01-10T00:00:00Z';
    const now = Date.parse('2026-01-10T01:00:00Z'); // 1 Stunde drin, Retention 1
    // Verbleibend = 1 * 86_400_000 - 3_600_000 = 82_800_000 → /86_400_000 ≈ 0.958 → ceil = 1
    expect(cleanupDaysRemaining(movedToDoneAt, 1, now)).toBe(1);
  });
});

describe('cleanupCountdownLabel', () => {
  it('formatiert 0 als "heute"', () => {
    expect(cleanupCountdownLabel(0)).toBe('wird heute archiviert');
  });

  it('formatiert 1 als "morgen"', () => {
    expect(cleanupCountdownLabel(1)).toBe('wird morgen archiviert');
  });

  it('formatiert mehrere Tage in Tagen', () => {
    expect(cleanupCountdownLabel(5)).toBe('wird in 5 Tagen archiviert');
  });
});
