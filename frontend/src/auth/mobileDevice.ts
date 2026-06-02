// Persistente Kennzeichnung eines "gekoppelten" Mobilgeräts (#206).
//
// Der Desktop zeigt einen QR-Code, der auf die Mobile-Seite mit dem Pairing-Flag
// `?pair=1` zeigt. Öffnet das Handy diesen Link, merken wir uns das Gerät dauerhaft
// in localStorage. Im Mobile-Modus läuft die Keycloak-Anmeldung mit `offline_access`
// und localStorage-Token-Storage (siehe buildOidcConfig) — das Handy bleibt damit bis
// zu 30 Tage (Keycloak offlineSessionIdleTimeout) ohne Neuanmeldung eingeloggt.

const MOBILE_DEVICE_KEY = 'toolbox-mobile-device';
const PAIR_PARAM = 'pair';

/**
 * Wertet das Pairing-Flag aus der URL aus. Ist `?pair=1` gesetzt, wird das Gerät als
 * Mobilgerät markiert und der Query-Parameter aus der Adresszeile entfernt (damit ein
 * späterer Reload nicht erneut „pairt" und die saubere URL erhalten bleibt).
 *
 * @returns true, wenn in diesem Aufruf neu gekoppelt wurde.
 */
export function markMobileDeviceFromUrl(search: string = window.location.search): boolean {
  const params = new URLSearchParams(search);
  if (params.get(PAIR_PARAM) !== '1') return false;

  window.localStorage.setItem(MOBILE_DEVICE_KEY, '1');

  params.delete(PAIR_PARAM);
  const query = params.toString();
  const cleanUrl = `${window.location.pathname}${query ? `?${query}` : ''}${window.location.hash}`;
  window.history.replaceState({}, document.title, cleanUrl);
  return true;
}

/** True, wenn dieses Gerät als persistentes Mobilgerät gekoppelt ist. */
export function isMobileDevice(): boolean {
  return window.localStorage.getItem(MOBILE_DEVICE_KEY) === '1';
}

/** Hebt die Kopplung auf (z. B. „Kopplung aufheben" + Logout auf dem Handy). */
export function clearMobileDevice(): void {
  window.localStorage.removeItem(MOBILE_DEVICE_KEY);
}

/** Der Deep-Link, den der Desktop als QR-Code anzeigt. */
export function mobilePairingUrl(origin: string = window.location.origin): string {
  return `${origin}/mobile?${PAIR_PARAM}=1`;
}
