import { beforeEach, describe, expect, it } from 'vitest';

import {
  clearReloginGuard,
  RELOGIN_WINDOW_MS,
  shouldAttemptRelogin,
} from './reloginGuard';

describe('reloginGuard (#233)', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
  });

  it('erlaubt den ersten Re-Login-Versuch', () => {
    expect(shouldAttemptRelogin(1_000)).toBe(true);
  });

  it('unterbricht einen zweiten Versuch innerhalb des Zeitfensters (Loop-Breaker)', () => {
    expect(shouldAttemptRelogin(1_000)).toBe(true);
    expect(shouldAttemptRelogin(1_000 + RELOGIN_WINDOW_MS - 1)).toBe(false);
  });

  it('erlaubt einen erneuten Versuch nach Ablauf des Zeitfensters', () => {
    expect(shouldAttemptRelogin(1_000)).toBe(true);
    expect(shouldAttemptRelogin(1_000 + RELOGIN_WINDOW_MS)).toBe(true);
  });

  it('erlaubt nach clearReloginGuard sofort wieder einen Versuch', () => {
    expect(shouldAttemptRelogin(1_000)).toBe(true);
    clearReloginGuard();
    expect(shouldAttemptRelogin(1_500)).toBe(true);
  });
});
