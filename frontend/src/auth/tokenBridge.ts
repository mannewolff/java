// Brücke zwischen react-oidc-context (Hook-Land) und dem reinen fetch-Client.
//
// Der API-Client (frontend/src/api/client.ts) ist kein React-Hook und darf daher nicht
// useAuth() aufrufen. Stattdessen registriert die AuthBridge bei jedem User-Wechsel den
// aktuellen Token-Getter und Re-Login-Callback hier.
//
// Beide Funktionen sind initial No-Ops. Wenn niemand sie setzt (z.B. in Tests ohne
// Provider) verhält sich der Client genauso wie ohne Auth.

type TokenGetter = () => string | undefined;
type AuthExpiredCallback = () => void;
type TokenRefresher = () => Promise<string | undefined>;

let tokenGetter: TokenGetter = () => undefined;
let authExpired: AuthExpiredCallback = () => undefined;
let tokenRefresher: TokenRefresher = async () => undefined;

// In-Flight-Refresh, damit mehrere parallele 401 (z. B. von mehreren Widgets) nur einen
// einzigen Refresh-Versuch teilen statt N gleichzeitige signinSilent() auszulösen.
let refreshInFlight: Promise<string | undefined> | null = null;

export function setTokenGetter(getter: TokenGetter): void {
  tokenGetter = getter;
}

export function setOnAuthExpired(callback: AuthExpiredCallback): void {
  authExpired = callback;
}

/**
 * Registriert die Funktion, die einen abgelaufenen Access-Token über den (Offline-)Refresh-Token
 * erneuert — gesetzt von der AuthBridge auf {@code auth.signinSilent()}. Liefert den **frischen**
 * Access-Token zurück (oder {@code undefined} bei Fehlschlag), damit der Aufrufer ihn direkt für
 * den Retry nutzen kann, statt auf das React-Re-Render des Token-Getters zu warten.
 */
export function setTokenRefresher(refresher: TokenRefresher): void {
  tokenRefresher = refresher;
}

export function getAccessToken(): string | undefined {
  return tokenGetter();
}

export function notifyAuthExpired(): void {
  authExpired();
}

/**
 * Versucht, den Access-Token still zu erneuern. Parallele Aufrufe teilen sich denselben
 * laufenden Versuch (Dedup). Liefert den frischen Access-Token bei Erfolg, sonst {@code undefined}.
 */
export function tryRefreshToken(): Promise<string | undefined> {
  if (refreshInFlight !== null) {
    return refreshInFlight;
  }
  // Refresher synchron starten, damit parallele Aufrufe garantiert denselben laufenden
  // Versuch sehen; ein synchroner Throw wird trotzdem zu `undefined`.
  let result: Promise<string | undefined>;
  try {
    result = Promise.resolve(tokenRefresher());
  } catch {
    result = Promise.resolve(undefined);
  }
  refreshInFlight = result
    .catch(() => undefined)
    .finally(() => {
      refreshInFlight = null;
    });
  return refreshInFlight;
}

// Test-Hook: setzt alle Bridge-Setter und den In-Flight-Refresh zwischen Tests zurück,
// ohne den Modul-Cache zu invalidieren.
export function __resetAuthBridge(): void {
  tokenGetter = () => undefined;
  authExpired = () => undefined;
  tokenRefresher = async () => undefined;
  refreshInFlight = null;
}
