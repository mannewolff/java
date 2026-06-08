import { afterEach, describe, expect, it, vi } from 'vitest';

import {
  __resetAuthBridge,
  setTokenRefresher,
  tryRefreshToken,
} from './tokenBridge';

describe('tokenBridge — tryRefreshToken (#237)', () => {
  afterEach(() => {
    __resetAuthBridge();
  });

  it('returns undefined when no refresher is registered', async () => {
    await expect(tryRefreshToken()).resolves.toBeUndefined();
  });

  it('delegates to the registered refresher and returns the fresh token', async () => {
    setTokenRefresher(vi.fn().mockResolvedValue('fresh-token'));
    await expect(tryRefreshToken()).resolves.toBe('fresh-token');
  });

  it('returns undefined when the refresher throws', async () => {
    setTokenRefresher(vi.fn().mockRejectedValue(new Error('network down')));
    await expect(tryRefreshToken()).resolves.toBeUndefined();
  });

  it('shares a single in-flight refresh across concurrent callers', async () => {
    let resolveRefresh: (value: string) => void = () => undefined;
    const refresher = vi.fn().mockImplementation(
      () =>
        new Promise<string>((resolve) => {
          resolveRefresh = resolve;
        }),
    );
    setTokenRefresher(refresher);

    const first = tryRefreshToken();
    const second = tryRefreshToken();
    resolveRefresh('fresh-token');

    await expect(first).resolves.toBe('fresh-token');
    await expect(second).resolves.toBe('fresh-token');
    // Beide Aufrufe teilen sich denselben Versuch — nur ein signinSilent().
    expect(refresher).toHaveBeenCalledTimes(1);
  });

  it('starts a fresh refresh after the previous one settled', async () => {
    const refresher = vi.fn().mockResolvedValue('fresh-token');
    setTokenRefresher(refresher);

    await tryRefreshToken();
    await tryRefreshToken();

    expect(refresher).toHaveBeenCalledTimes(2);
  });
});
