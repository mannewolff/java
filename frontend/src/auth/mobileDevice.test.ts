import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  clearMobileDevice,
  isMobileDevice,
  markMobileDeviceFromUrl,
  mobilePairingUrl,
} from './mobileDevice';

describe('mobileDevice', () => {
  beforeEach(() => {
    window.localStorage.clear();
    window.history.replaceState({}, '', '/mobile');
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('markiert das Gerät und entfernt den pair-Param bei ?pair=1', () => {
    const replace = vi.spyOn(window.history, 'replaceState');
    const marked = markMobileDeviceFromUrl('?pair=1');

    expect(marked).toBe(true);
    expect(isMobileDevice()).toBe(true);
    expect(replace).toHaveBeenCalled();
    // Letztes Argument des letzten replaceState-Aufrufs ist die bereinigte URL.
    const lastUrl = replace.mock.calls.at(-1)?.[2];
    expect(lastUrl).not.toContain('pair');
  });

  it('erhält weitere Query-Parameter beim Entfernen von pair', () => {
    const replace = vi.spyOn(window.history, 'replaceState');
    markMobileDeviceFromUrl('?pair=1&foo=bar');
    const lastUrl = String(replace.mock.calls.at(-1)?.[2]);
    expect(lastUrl).toContain('foo=bar');
    expect(lastUrl).not.toContain('pair');
  });

  it('macht nichts ohne pair-Flag', () => {
    const replace = vi.spyOn(window.history, 'replaceState');
    const marked = markMobileDeviceFromUrl('?foo=bar');

    expect(marked).toBe(false);
    expect(isMobileDevice()).toBe(false);
    expect(replace).not.toHaveBeenCalled();
  });

  it('clearMobileDevice hebt die Kopplung auf', () => {
    markMobileDeviceFromUrl('?pair=1');
    expect(isMobileDevice()).toBe(true);
    clearMobileDevice();
    expect(isMobileDevice()).toBe(false);
  });

  it('mobilePairingUrl baut den Deep-Link mit pair-Flag', () => {
    expect(mobilePairingUrl('https://toolbox.example')).toBe(
      'https://toolbox.example/mobile?pair=1',
    );
  });
});
