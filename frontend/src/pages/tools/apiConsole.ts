// Reine Logik der API-Konsole (#211) — same-origin-Guard, Header-Bau und localStorage-
// Persistenz gespeicherter Requests. Bewusst ohne React, damit unit-testbar.

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
export const HTTP_METHODS: readonly HttpMethod[] = [
  'GET',
  'POST',
  'PUT',
  'PATCH',
  'DELETE',
] as const;

/** Methoden, die einen Request-Body tragen dürfen. */
export const METHODS_WITH_BODY: readonly HttpMethod[] = ['POST', 'PUT', 'PATCH'] as const;

export type AuthMode = 'none' | 'bearer' | 'ingest';

export interface HeaderPair {
  key: string;
  value: string;
}

export interface RequestDraft {
  method: HttpMethod;
  path: string;
  headers: HeaderPair[];
  body: string;
  authMode: AuthMode;
}

export interface SavedRequest {
  id: string;
  name: string;
  method: HttpMethod;
  path: string;
  headers: HeaderPair[];
  body: string;
  authMode: AuthMode;
}

const STORAGE_KEY = 'toolbox-api-console-requests';

/**
 * Erzwingt same-origin: löst den Pfad gegen die eigene Origin auf und gibt die absolute URL
 * zurück, wenn sie auf derselben Origin liegt — sonst null (fremde URL → abgewiesen).
 */
export function resolveSameOriginUrl(
  path: string,
  origin: string = window.location.origin,
): string | null {
  const trimmed = path.trim();
  if (trimmed === '') return null;
  try {
    const url = new URL(trimmed, origin);
    return url.origin === origin ? url.toString() : null;
  } catch {
    return null;
  }
}

/** Validiert JSON; gibt true zurück, wenn der String leer oder gültiges JSON ist. */
export function isValidJsonBody(body: string): boolean {
  if (body.trim() === '') return true;
  try {
    JSON.parse(body);
    return true;
  } catch {
    return false;
  }
}

/**
 * Baut die finalen Request-Header aus den manuellen Headern plus dem Auth-Header gemäß Modus.
 * Bearer → `Authorization: Bearer <bearerToken>`, Ingest → `X-Ingest-Token: <ingestToken>`,
 * none → kein Auth-Header. Leere Header-Zeilen werden ignoriert.
 */
export function buildHeaders(
  draft: RequestDraft,
  tokens: { bearer?: string; ingest?: string },
): Record<string, string> {
  const headers: Record<string, string> = {};
  for (const { key, value } of draft.headers) {
    if (key.trim() !== '') headers[key.trim()] = value;
  }
  if (draft.authMode === 'bearer' && tokens.bearer) {
    headers.Authorization = `Bearer ${tokens.bearer}`;
  } else if (draft.authMode === 'ingest' && tokens.ingest) {
    headers['X-Ingest-Token'] = tokens.ingest;
  }
  return headers;
}

export function loadSavedRequests(): SavedRequest[] {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as SavedRequest[]) : [];
  } catch {
    return [];
  }
}

function persist(requests: SavedRequest[]): void {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(requests));
}

function newId(): string {
  const cryptoObj = globalThis.crypto;
  if (cryptoObj && typeof cryptoObj.randomUUID === 'function') {
    return cryptoObj.randomUUID();
  }
  return `req-${Date.now()}-${loadSavedRequests().length}`;
}

/**
 * Speichert einen Request unter Namen. **Token-Werte werden bewusst NICHT persistiert** —
 * nur Methode/Pfad/Header/Body/Auth-Modus. Ein vorhandener Name wird überschrieben.
 */
export function saveRequest(name: string, draft: RequestDraft): SavedRequest[] {
  const trimmedName = name.trim();
  const requests = loadSavedRequests().filter((r) => r.name !== trimmedName);
  requests.push({
    id: newId(),
    name: trimmedName,
    method: draft.method,
    path: draft.path,
    headers: draft.headers.filter((h) => h.key.trim() !== ''),
    body: draft.body,
    authMode: draft.authMode,
  });
  persist(requests);
  return requests;
}

export function deleteSavedRequest(id: string): SavedRequest[] {
  const requests = loadSavedRequests().filter((r) => r.id !== id);
  persist(requests);
  return requests;
}
