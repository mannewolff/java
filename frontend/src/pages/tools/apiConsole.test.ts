import { beforeEach, describe, expect, it } from 'vitest';

import {
  buildHeaders,
  deleteSavedRequest,
  isValidJsonBody,
  loadSavedRequests,
  resolveSameOriginUrl,
  saveRequest,
  type RequestDraft,
} from './apiConsole';

const origin = window.location.origin;

function draft(over: Partial<RequestDraft> = {}): RequestDraft {
  return {
    method: 'GET',
    path: '/api/timeseries',
    headers: [],
    body: '',
    authMode: 'none',
    ...over,
  };
}

describe('resolveSameOriginUrl', () => {
  it('akzeptiert relative /api-Pfade', () => {
    expect(resolveSameOriginUrl('/api/timeseries')).toBe(`${origin}/api/timeseries`);
  });

  it('akzeptiert absolute URLs auf der eigenen Origin', () => {
    expect(resolveSameOriginUrl(`${origin}/api/version`)).toBe(`${origin}/api/version`);
  });

  it('weist fremde Origins ab', () => {
    expect(resolveSameOriginUrl('https://evil.example/x')).toBeNull();
  });

  it('weist leere Eingaben ab', () => {
    expect(resolveSameOriginUrl('   ')).toBeNull();
  });
});

describe('isValidJsonBody', () => {
  it('leerer Body gilt als gültig', () => {
    expect(isValidJsonBody('  ')).toBe(true);
  });
  it('gültiges JSON', () => {
    expect(isValidJsonBody('{"a":1}')).toBe(true);
  });
  it('ungültiges JSON', () => {
    expect(isValidJsonBody('{a:1')).toBe(false);
  });
});

describe('buildHeaders', () => {
  it('setzt im Bearer-Modus den Authorization-Header', () => {
    const h = buildHeaders(draft({ authMode: 'bearer' }), { bearer: 'TKN' });
    expect(h.Authorization).toBe('Bearer TKN');
    expect(h['X-Ingest-Token']).toBeUndefined();
  });

  it('setzt im Ingest-Modus den X-Ingest-Token-Header', () => {
    const h = buildHeaders(draft({ authMode: 'ingest' }), { ingest: 'ING' });
    expect(h['X-Ingest-Token']).toBe('ING');
    expect(h.Authorization).toBeUndefined();
  });

  it('setzt im none-Modus keinen Auth-Header', () => {
    const h = buildHeaders(draft({ authMode: 'none' }), { bearer: 'TKN', ingest: 'ING' });
    expect(h.Authorization).toBeUndefined();
    expect(h['X-Ingest-Token']).toBeUndefined();
  });

  it('übernimmt manuelle Header und ignoriert leere Namen', () => {
    const h = buildHeaders(
      draft({ headers: [{ key: 'Accept', value: 'application/json' }, { key: '  ', value: 'x' }] }),
      {},
    );
    expect(h.Accept).toBe('application/json');
    expect(Object.keys(h)).toHaveLength(1);
  });
});

describe('localStorage-Persistenz', () => {
  beforeEach(() => window.localStorage.clear());

  it('speichert ohne Token-Werte und lädt wieder', () => {
    const list = saveRequest('Liste', draft({ authMode: 'bearer' }));
    expect(list).toHaveLength(1);
    expect(list[0].name).toBe('Liste');
    expect(list[0].authMode).toBe('bearer');
    // Es werden keine Token-Felder gespeichert.
    expect(JSON.stringify(list[0])).not.toContain('Bearer');
    expect(loadSavedRequests()).toHaveLength(1);
  });

  it('überschreibt gleichnamige Requests statt zu duplizieren', () => {
    saveRequest('A', draft());
    const list = saveRequest('A', draft({ method: 'POST' }));
    expect(list).toHaveLength(1);
    expect(list[0].method).toBe('POST');
  });

  it('löscht per id', () => {
    const list = saveRequest('A', draft());
    const after = deleteSavedRequest(list[0].id);
    expect(after).toHaveLength(0);
    expect(loadSavedRequests()).toHaveLength(0);
  });

  it('liefert [] bei kaputtem localStorage-Inhalt', () => {
    window.localStorage.setItem('toolbox-api-console-requests', 'not json');
    expect(loadSavedRequests()).toEqual([]);
  });
});
