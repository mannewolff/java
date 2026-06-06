import { getAccessToken, notifyAuthExpired } from '../auth/tokenBridge';

const BASE_URL = '/api';

export interface ApiErrorBody {
  status?: number;
  error?: string;
  message?: string;
  fieldErrors?: Record<string, string>;
}

export class ApiError extends Error {
  status: number;
  body: ApiErrorBody | null;

  constructor(status: number, message: string, body: ApiErrorBody | null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  options: AuthedFetchOptions = {},
): Promise<T> {
  const token = getAccessToken();
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json',
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
    ...init,
  });

  if (response.status === 401) {
    // Token abgelaufen oder ungueltig — Re-Login ausloesen und Fehler weiterreichen,
    // damit die aufrufende Komponente ihren Loading-State sauber zurueckdrehen kann.
    // Hintergrund-/unkritische Calls (z. B. Versionsanzeige) koennen den Re-Login-Trigger
    // via suppressAuthExpired unterdruecken, damit ein einzelner 401 keinen Loop ausloest (#233).
    if (!options.suppressAuthExpired) {
      notifyAuthExpired();
    }
    const body = await safeJson<ApiErrorBody>(response);
    throw new ApiError(
      response.status,
      body?.message ?? 'Unauthorized',
      body,
    );
  }

  if (!response.ok) {
    const body = await safeJson<ApiErrorBody>(response);
    const message = body?.message ?? `${response.status} ${response.statusText}`;
    throw new ApiError(response.status, message, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function safeJson<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

/** Optionen für {@link authedFetch}. */
export interface AuthedFetchOptions {
  /**
   * Unterdrückt den globalen Re-Login-Trigger (`notifyAuthExpired`) bei 401. Für passive
   * Resource-Fetches (z. B. Bild-Blobs in Dashboard-Widgets): ein einzelnes nicht-ladbares
   * Bild darf nicht die ganze Session in einen Re-Login-Loop reißen (#233). Der Aufrufer
   * behandelt den Fehler dann selbst (Error-Anzeige).
   */
  suppressAuthExpired?: boolean;
}

/**
 * Wrapper um `fetch`, der den aktuellen Bearer-Token an den Authorization-Header haengt und
 * auf 401 mit einem Re-Login-Trigger reagiert. Anders als `api.*` parst dieser Helper die
 * Response **nicht** — Aufrufer arbeiten mit dem Response-Objekt direkt (z. B. fuer Multipart-
 * Uploads, die binaere Antworten als Blob brauchen).
 */
export async function authedFetch(
  input: RequestInfo | URL,
  init: RequestInit = {},
  options: AuthedFetchOptions = {},
): Promise<Response> {
  const token = getAccessToken();
  const headers = new Headers(init.headers);
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(input, { ...init, headers });
  if (response.status === 401 && !options.suppressAuthExpired) {
    notifyAuthExpired();
  }
  return response;
}

export const api = {
  get: <T>(path: string, options?: AuthedFetchOptions) =>
    request<T>(path, undefined, options),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  del: <T = void>(path: string) => request<T>(path, { method: 'DELETE' }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body !== undefined ? JSON.stringify(body) : undefined }),
};
