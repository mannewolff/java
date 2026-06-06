// Reload-fester Loop-Breaker für den 401→Re-Login-Pfad (#233).
//
// Ein 401 auf einem geschützten Endpoint löst `signinRedirect()` aus — einen vollständigen
// Keycloak-Redirect, der die Seite neu lädt. Behebt das Re-Login den 401 nicht (abgelaufene
// SSO-Session, dauerhaft abgelehnter Token), würde der nächste Seitenaufbau sofort wieder
// einen Redirect anstoßen → Endlos-"Flackern". Der In-Page-Guard in AuthProvider hilft hier
// nicht, weil er beim Reload zurückgesetzt wird.
//
// Dieser Guard merkt sich den Zeitpunkt des letzten Re-Login-Versuchs in sessionStorage und
// erlaubt höchstens einen Versuch pro Zeitfenster. Schlägt das Re-Login fehl, wird der Loop
// nach einem Versuch unterbrochen und der Aufrufer zeigt seinen Fehler an. Nach erfolgreichem
// Login wird der Marker via {@link clearReloginGuard} gelöscht.

const STORAGE_KEY = 'toolbox.reloginAttemptAt';

/** Mindestabstand zwischen zwei automatischen Re-Login-Versuchen. */
export const RELOGIN_WINDOW_MS = 20_000;

/**
 * Liefert {@code true}, wenn jetzt ein automatischer Re-Login versucht werden darf, und merkt
 * sich den Zeitpunkt. Liefert {@code false}, wenn bereits kürzlich (innerhalb
 * {@link RELOGIN_WINDOW_MS}) ein Versuch lief — dann wird der Loop unterbrochen.
 */
export function shouldAttemptRelogin(now: number = Date.now()): boolean {
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY);
    if (raw !== null) {
      const last = Number(raw);
      if (Number.isFinite(last) && now - last < RELOGIN_WINDOW_MS) {
        return false;
      }
    }
    window.sessionStorage.setItem(STORAGE_KEY, String(now));
    return true;
  } catch {
    // Ohne sessionStorage (z. B. Privacy-Modus): lieber einmal versuchen als nie.
    return true;
  }
}

/** Setzt den Guard zurück — nach erfolgreichem Login, damit spätere echte 401 wieder greifen. */
export function clearReloginGuard(): void {
  try {
    window.sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // ignore
  }
}
