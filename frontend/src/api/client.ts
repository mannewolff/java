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

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
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
    notifyAuthExpired();
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

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  del: <T = void>(path: string) => request<T>(path, { method: 'DELETE' }),
};
