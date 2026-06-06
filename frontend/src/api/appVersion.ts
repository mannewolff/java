import { api } from './client';

export interface AppVersion {
  major: number;
  minor: number;
}

const PATH = '/app/version';

/**
 * Liest die aktuelle Anwendungsversion (GET /api/app/version). Wirft bei Fehler ApiError.
 * Hintergrund-Call der immer gemounteten AppShell: ein 401 (z. B. abgelaufener Token) darf
 * KEIN globales Re-Login auslösen, sonst entsteht beim Mount ein Endlos-Redirect-Loop (#233).
 * Der Aufrufer behandelt den Fehler graceful (Version bleibt leer).
 */
export function getAppVersion(): Promise<AppVersion> {
  return api.get<AppVersion>(PATH, { suppressAuthExpired: true });
}
