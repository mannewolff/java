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

let tokenGetter: TokenGetter = () => undefined;
let authExpired: AuthExpiredCallback = () => undefined;

export function setTokenGetter(getter: TokenGetter): void {
  tokenGetter = getter;
}

export function setOnAuthExpired(callback: AuthExpiredCallback): void {
  authExpired = callback;
}

export function getAccessToken(): string | undefined {
  return tokenGetter();
}

export function notifyAuthExpired(): void {
  authExpired();
}

// Test-Hook: ermöglicht es, beide Setter zwischen Tests zurückzusetzen, ohne
// den Modul-Cache zu invalidieren.
export function __resetAuthBridge(): void {
  tokenGetter = () => undefined;
  authExpired = () => undefined;
}
