import { api } from './client';

export interface AppVersion {
  major: number;
  minor: number;
}

const PATH = '/app/version';

/** Liest die aktuelle Anwendungsversion (GET /api/app/version). Wirft bei Fehler ApiError. */
export function getAppVersion(): Promise<AppVersion> {
  return api.get<AppVersion>(PATH);
}
