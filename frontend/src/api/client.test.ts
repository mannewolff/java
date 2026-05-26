import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { api, ApiError, authedFetch } from './client';
import {
  __resetAuthBridge,
  setOnAuthExpired,
  setTokenGetter,
} from '../auth/tokenBridge';

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
});
