import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { api, ApiError, authedFetch } from './client';
import {
  __resetAuthBridge,
  setOnAuthExpired,
  setTokenGetter,
  setTokenRefresher,
} from '../auth/tokenBridge';

function unauthorized(): Response {
  return new Response(JSON.stringify({ message: 'expired' }), {
    status: 401,
    headers: { 'Content-Type': 'application/json' },
  });
}

function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
}

describe('api client — auth integration', () => {
  beforeEach(() => {
    __resetAuthBridge();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    __resetAuthBridge();
  });

  it('attaches a Bearer token from tokenBridge when one is available', async () => {
    // given
    setTokenGetter(() => 'access-token-abc');
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(jsonResponse({ ok: true }));

    // when
    await api.get('/me');

    // then
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0];
    const headers = init?.headers as Record<string, string>;
    expect(headers.Authorization).toBe('Bearer access-token-abc');
  });

  it('omits the Authorization header when no token is available', async () => {
    // given — kein Token gesetzt
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(jsonResponse({ ok: true }));

    // when
    await api.get('/public-thing');

    // then
    const [, init] = fetchMock.mock.calls[0];
    const headers = init?.headers as Record<string, string>;
    expect(headers.Authorization).toBeUndefined();
  });

  it('invokes onAuthExpired and throws ApiError on 401 responses', async () => {
    // given
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ message: 'expired' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    // when
    const promise = api.get('/me');

    // then
    await expect(promise).rejects.toBeInstanceOf(ApiError);
    expect(expiredCallback).toHaveBeenCalledTimes(1);
  });

  it('does NOT trigger re-login on 401 when api.get gets suppressAuthExpired (#233)', async () => {
    const expiredCallback = vi.fn();
    const refresher = vi.fn().mockResolvedValue('fresh-token');
    setOnAuthExpired(expiredCallback);
    setTokenRefresher(refresher);
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(unauthorized());

    // Hintergrund-Call (z. B. Versionsanzeige): wirft weiterhin, aber ohne Re-Login-Loop und
    // ohne Refresh-Versuch.
    await expect(
      api.get('/app/version', { suppressAuthExpired: true }),
    ).rejects.toBeInstanceOf(ApiError);
    expect(expiredCallback).not.toHaveBeenCalled();
    expect(refresher).not.toHaveBeenCalled();
  });

  it('refreshes the token and retries once with the FRESH token, without re-login (#237)', async () => {
    // given — erster Call 401, nach erfolgreichem Refresh liefert der Retry 200
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    setTokenGetter(() => 'stale-token');
    setTokenRefresher(vi.fn().mockResolvedValue('fresh-token'));
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(unauthorized())
      .mockResolvedValueOnce(jsonResponse({ ok: true }));

    // when
    const result = await api.get<{ ok: boolean }>('/me');

    // then
    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(expiredCallback).not.toHaveBeenCalled();
    // erster Versuch mit altem Token, Retry mit dem frischen Token aus dem Refresh
    const firstHeaders = fetchMock.mock.calls[0][1]?.headers as Record<string, string>;
    const retryHeaders = fetchMock.mock.calls[1][1]?.headers as Record<string, string>;
    expect(firstHeaders.Authorization).toBe('Bearer stale-token');
    expect(retryHeaders.Authorization).toBe('Bearer fresh-token');
  });

  it('falls back to re-login when the refresh fails on 401 (#237)', async () => {
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    setTokenRefresher(vi.fn().mockResolvedValue(undefined));
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(unauthorized());

    await expect(api.get('/me')).rejects.toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(expiredCallback).toHaveBeenCalledTimes(1);
  });

  it('falls back to re-login when the retry still returns 401 (#237)', async () => {
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    setTokenRefresher(vi.fn().mockResolvedValue('fresh-token'));
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(unauthorized());

    await expect(api.get('/me')).rejects.toBeInstanceOf(ApiError);
    // ein Original-Call + genau ein Retry, danach kein weiterer Refresh-Loop
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(expiredCallback).toHaveBeenCalledTimes(1);
  });

  it('propagates non-401 errors without triggering re-login', async () => {
    // given
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ message: 'bad request' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    // when
    await expect(api.get('/something')).rejects.toBeInstanceOf(ApiError);

    // then
    expect(expiredCallback).not.toHaveBeenCalled();
  });

  it('returns undefined for 204 No Content responses', async () => {
    // given
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(null, { status: 204 }),
    );

    // when
    const result = await api.del('/thing/1');

    // then
    expect(result).toBeUndefined();
  });
});

describe('authedFetch', () => {
  beforeEach(() => {
    __resetAuthBridge();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    __resetAuthBridge();
  });

  it('attaches Bearer token to multipart uploads', async () => {
    // given
    setTokenGetter(() => 'tok-xyz');
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response(new Blob([new Uint8Array([1, 2])])));
    const body = new FormData();
    body.append('file', new Blob([new Uint8Array([7])]));

    // when
    await authedFetch('/api/tools/resize', { method: 'POST', body });

    // then
    const [, init] = fetchMock.mock.calls[0];
    const headers = init?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer tok-xyz');
  });

  it('omits Authorization when no token available', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('ok'));

    await authedFetch('/api/public', { method: 'GET' });

    const [, init] = fetchMock.mock.calls[0];
    const headers = init?.headers as Headers;
    expect(headers.get('Authorization')).toBeNull();
  });

  it('triggers notifyAuthExpired on 401', async () => {
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('', { status: 401 }),
    );

    await authedFetch('/api/tools/whatever', { method: 'POST' });

    expect(expiredCallback).toHaveBeenCalledTimes(1);
  });

  it('refreshes and retries once on 401 with the fresh token, returning the retried response (#237)', async () => {
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    setTokenGetter(() => 'stale-token');
    setTokenRefresher(vi.fn().mockResolvedValue('fresh-token'));
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response('', { status: 401 }))
      .mockResolvedValueOnce(new Response('ok', { status: 200 }));

    const response = await authedFetch('/api/tools/whatever', { method: 'POST' });

    expect(response.status).toBe(200);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(expiredCallback).not.toHaveBeenCalled();
    const retryHeaders = fetchMock.mock.calls[1][1]?.headers as Headers;
    expect(retryHeaders.get('Authorization')).toBe('Bearer fresh-token');
  });

  it('does NOT trigger notifyAuthExpired on 401 when suppressAuthExpired is set (#233)', async () => {
    const expiredCallback = vi.fn();
    setOnAuthExpired(expiredCallback);
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('', { status: 401 }),
    );

    const response = await authedFetch(
      '/api/images/5',
      {},
      { suppressAuthExpired: true },
    );

    // Aufrufer bekommt den 401 zur eigenen Behandlung, aber kein globaler Re-Login-Loop.
    expect(response.status).toBe(401);
    expect(expiredCallback).not.toHaveBeenCalled();
  });

  it('does not override caller-provided Authorization header', async () => {
    setTokenGetter(() => 'tok-xyz');
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('ok'));

    await authedFetch('/api/special', {
      method: 'POST',
      headers: { Authorization: 'Bearer custom' },
    });

    const [, init] = fetchMock.mock.calls[0];
    const headers = init?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer custom');
  });

  it('behält den Bearer-Token, wenn der Aufrufer eigene init.headers mitgibt (#316)', async () => {
    // Regression: früher überschrieb `...init` (mit init.headers) den ganzen Header-Block,
    // sodass ein eigener Header den Authorization-Token verdrängte.
    setTokenGetter(() => 'token-xyz');
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('ok'));

    await authedFetch('/api/special', {
      method: 'POST',
      headers: { 'X-Custom': 'value' },
    });

    const [, init] = fetchMock.mock.calls[0];
    const headers = new Headers(init?.headers as HeadersInit);
    expect(headers.get('Authorization')).toBe('Bearer token-xyz');
    expect(headers.get('X-Custom')).toBe('value');
  });
});
