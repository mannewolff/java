import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, rmSync, statSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import {
  parseArgs,
  isExpired,
  computeExpiry,
  decodeJwtPayload,
  resolveConfig,
  writeJsonFileSecure,
  readJsonFile,
  configPath,
  tokensPath,
  requestDeviceCode,
  pollDeviceToken,
  refreshTokens,
  apiFetch,
  main,
  AuthError,
} from './tbx.mjs';

/**
 * Jeder Test bekommt sein eigenes, isoliertes Temp-Verzeichnis und reicht es
 * explizit als baseDir durch — kein gemeinsames Mutable-Global (process.env),
 * damit Tests unter Node's Default-Parallelitaet (node:test) sich nicht
 * gegenseitig stoeren (siehe #285-Testlauf: race condition mit env-var-Ansatz).
 */
function withTempConfigDir(fn) {
  const dir = mkdtempSync(join(tmpdir(), 'tbx-test-'));
  try {
    return fn(dir);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

function fakeJwt(claims) {
  const header = Buffer.from(JSON.stringify({ alg: 'none' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify(claims)).toString('base64url');
  return `${header}.${payload}.sig`;
}

function jsonResponse(status, body) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

function io(overrides = {}) {
  const stdoutLines = [];
  const stderrLines = [];
  return {
    stdout: (s) => stdoutLines.push(s),
    stderr: (s) => stderrLines.push(s),
    fetchImpl: overrides.fetchImpl,
    sleepImpl: overrides.sleepImpl || (() => Promise.resolve()),
    baseDir: overrides.baseDir,
    stdoutLines,
    stderrLines,
  };
}

// --- 1. Argument-Parser ------------------------------------------------------

test('parseArgs: liest Flags mit Werten und Positional-Argumente', () => {
  const result = parseArgs(['--host', 'http://x', 'foo', '--flag-only']);
  assert.equal(result.host, 'http://x');
  assert.equal(result['flag-only'], true);
  assert.deepEqual(result._, ['foo']);
});

test('parseArgs: liefert leere Defaults ohne Argumente', () => {
  assert.deepEqual(parseArgs([]), { _: [] });
});

test('main: ohne Argumente zeigt Hilfe und liefert Exit-Code 0', async () => {
  const i = io();
  const code = await main([], i);
  assert.equal(code, 0);
  assert.match(i.stdoutLines.join(''), /tbx —/);
});

test('main: unbekannter auth-Befehl zeigt Hilfe und liefert Exit-Code 1', async () => {
  const i = io();
  const code = await main(['auth', 'nonsense'], i);
  assert.equal(code, 1);
  assert.match(i.stdoutLines.join(''), /tbx —/);
  assert.match(i.stderrLines.join(''), /Unbekannter auth-Befehl/);
});

test('main: unbekannte Achse zeigt Hilfe und liefert Exit-Code 1', async () => {
  const i = io();
  const code = await main(['issue', 'list'], i);
  assert.equal(code, 1);
  assert.match(i.stderrLines.join(''), /Unbekannte Achse/);
});

// --- 2. Token-Ablauf-Logik ---------------------------------------------------

test('isExpired: false deutlich vor Ablauf', () => {
  const now = 1_000_000;
  assert.equal(isExpired(now + 60_000, now), false);
});

test('isExpired: true innerhalb des Puffers vor Ablauf', () => {
  const now = 1_000_000;
  assert.equal(isExpired(now + 10_000, now, 30_000), true);
});

test('isExpired: true nach Ablauf', () => {
  const now = 1_000_000;
  assert.equal(isExpired(now - 1, now), true);
});

test('computeExpiry: addiert expires_in Sekunden auf now', () => {
  assert.equal(computeExpiry(300, 1_000_000), 1_000_000 + 300_000);
});

test('decodeJwtPayload: liest Claims aus einem base64url-JWT', () => {
  const token = fakeJwt({ preferred_username: 'alice', exp: 123 });
  assert.deepEqual(decodeJwtPayload(token), { preferred_username: 'alice', exp: 123 });
});

// --- 3. Refresh-Fluss ---------------------------------------------------------

test('refreshTokens: 200 liefert neue Tokens', async () => {
  const fetchImpl = async () => jsonResponse(200, { access_token: 'new-at', refresh_token: 'new-rt', expires_in: 300 });
  const result = await refreshTokens({ keycloakUrl: 'http://kc', realm: 'r' }, 'old-rt', fetchImpl);
  assert.equal(result.access_token, 'new-at');
});

test('refreshTokens: 400 invalid_grant wirft AuthError mit Login-Hinweis', async () => {
  const fetchImpl = async () => jsonResponse(400, { error: 'invalid_grant' });
  await assert.rejects(
    () => refreshTokens({ keycloakUrl: 'http://kc', realm: 'r' }, 'old-rt', fetchImpl),
    (err) => {
      assert.ok(err instanceof AuthError);
      assert.match(err.message, /tbx auth login/);
      return true;
    },
  );
});

test('apiFetch: refresht automatisch, wenn das Access-Token abgelaufen ist', () =>
  withTempConfigDir(async (dir) => {
    writeJsonFileSecure(configPath(dir), { host: 'http://api', keycloakUrl: 'http://kc', realm: 'r' });
    writeJsonFileSecure(tokensPath(dir), { access_token: 'old-at', refresh_token: 'rt', expires_at: Date.now() - 1000 });

    const calls = [];
    const fetchImpl = async (url, opts) => {
      calls.push(url);
      if (url.includes('/protocol/openid-connect/token')) {
        return jsonResponse(200, { access_token: 'fresh-at', refresh_token: 'rt2', expires_in: 300 });
      }
      assert.equal(opts.headers.Authorization, 'Bearer fresh-at');
      return jsonResponse(200, { ok: true });
    };

    const res = await apiFetch('/api/kanban/items', {}, { fetchImpl, baseDir: dir });
    assert.equal(res.ok, true);
    const persisted = readJsonFile(tokensPath(dir));
    assert.equal(persisted.access_token, 'fresh-at');
  }));

test('apiFetch: wirft not_logged_in ohne gespeicherte Tokens', () =>
  withTempConfigDir(async (dir) => {
    await assert.rejects(
      () => apiFetch('/api/kanban/items', {}, { fetchImpl: async () => jsonResponse(200, {}), baseDir: dir }),
      (err) => {
        assert.ok(err instanceof AuthError);
        assert.equal(err.reason, 'not_logged_in');
        return true;
      },
    );
  }));

// --- 4. Device-Flow-Polling ----------------------------------------------------

test('pollDeviceToken: authorization_pending pollt weiter bis Erfolg', async () => {
  let calls = 0;
  const fetchImpl = async () => {
    calls++;
    if (calls < 3) return jsonResponse(400, { error: 'authorization_pending' });
    return jsonResponse(200, { access_token: 'at', refresh_token: 'rt', expires_in: 300 });
  };
  const result = await pollDeviceToken(
    { keycloakUrl: 'http://kc', realm: 'r' },
    { device_code: 'dc', interval: 5, expires_in: 600 },
    { fetchImpl, sleepImpl: async () => {} },
  );
  assert.equal(result.access_token, 'at');
  assert.equal(calls, 3);
});

test('pollDeviceToken: slow_down erhöht das Poll-Intervall', async () => {
  let calls = 0;
  const sleeps = [];
  const fetchImpl = async () => {
    calls++;
    if (calls === 1) return jsonResponse(400, { error: 'slow_down' });
    return jsonResponse(200, { access_token: 'at', refresh_token: 'rt', expires_in: 300 });
  };
  await pollDeviceToken(
    { keycloakUrl: 'http://kc', realm: 'r' },
    { device_code: 'dc', interval: 5, expires_in: 600 },
    { fetchImpl, sleepImpl: async (ms) => sleeps.push(ms) },
  );
  assert.deepEqual(sleeps, [5000, 10000]);
});

test('pollDeviceToken: access_denied bricht mit AuthError ab', async () => {
  const fetchImpl = async () => jsonResponse(400, { error: 'access_denied' });
  await assert.rejects(
    () =>
      pollDeviceToken(
        { keycloakUrl: 'http://kc', realm: 'r' },
        { device_code: 'dc', interval: 5, expires_in: 600 },
        { fetchImpl, sleepImpl: async () => {} },
      ),
    (err) => {
      assert.ok(err instanceof AuthError);
      assert.equal(err.reason, 'access_denied');
      return true;
    },
  );
});

test('pollDeviceToken: expired_token bricht mit AuthError ab', async () => {
  const fetchImpl = async () => jsonResponse(400, { error: 'expired_token' });
  await assert.rejects(
    () =>
      pollDeviceToken(
        { keycloakUrl: 'http://kc', realm: 'r' },
        { device_code: 'dc', interval: 5, expires_in: 600 },
        { fetchImpl, sleepImpl: async () => {} },
      ),
    (err) => {
      assert.ok(err instanceof AuthError);
      assert.equal(err.reason, 'expired_token');
      return true;
    },
  );
});

test('requestDeviceCode: liefert device_code und user_code', async () => {
  const fetchImpl = async () =>
    jsonResponse(200, { device_code: 'dc', user_code: 'ABCD-1234', verification_uri: 'http://kc/device', interval: 5, expires_in: 600 });
  const result = await requestDeviceCode({ keycloakUrl: 'http://kc', realm: 'r' }, fetchImpl);
  assert.equal(result.user_code, 'ABCD-1234');
});

test('requestDeviceCode: HTTP-Fehler wirft AuthError', async () => {
  const fetchImpl = async () => jsonResponse(400, {});
  await assert.rejects(() => requestDeviceCode({ keycloakUrl: 'http://kc', realm: 'r' }, fetchImpl), AuthError);
});

// --- 5. Storage-Roundtrip -------------------------------------------------------

test('writeJsonFileSecure + readJsonFile: Roundtrip mit 0600-Rechten', () =>
  withTempConfigDir((dir) => {
    writeJsonFileSecure(tokensPath(dir), { access_token: 'secret' });
    const loaded = readJsonFile(tokensPath(dir));
    assert.deepEqual(loaded, { access_token: 'secret' });

    const mode = statSync(tokensPath(dir)).mode & 0o777;
    assert.equal(mode, 0o600);
  }));

test('writeJsonFileSecure: erzwingt 0600 auch beim Ueberschreiben einer bestehenden Datei', () =>
  withTempConfigDir((dir) => {
    writeJsonFileSecure(tokensPath(dir), { a: 1 });
    writeJsonFileSecure(tokensPath(dir), { a: 2 });
    const mode = statSync(tokensPath(dir)).mode & 0o777;
    assert.equal(mode, 0o600);
    assert.deepEqual(readJsonFile(tokensPath(dir)), { a: 2 });
  }));

test('readJsonFile: liefert null, wenn die Datei nicht existiert', () =>
  withTempConfigDir((dir) => {
    assert.equal(readJsonFile(join(dir, 'missing.json')), null);
  }));

// --- resolveConfig ------------------------------------------------------------

test('resolveConfig: Flags überschreiben gespeicherte Config und Defaults', () => {
  const cfg = resolveConfig({ host: 'http://flag' }, { host: 'http://stored', keycloakUrl: 'http://stored-kc', realm: 'stored-realm' });
  assert.equal(cfg.host, 'http://flag');
  assert.equal(cfg.keycloakUrl, 'http://stored-kc');
  assert.equal(cfg.realm, 'stored-realm');
});

test('resolveConfig: fällt ohne Flags/Config auf Produktions-Defaults zurück', () => {
  const cfg = resolveConfig({}, null);
  assert.equal(cfg.host, 'https://toolbox.mwolff.org');
  assert.equal(cfg.keycloakUrl, 'https://toolboxauth.mwolff.org');
  assert.equal(cfg.realm, 'toolbox');
});

// --- Kommandos end-to-end (gemockt) --------------------------------------------

test('main auth login: schreibt Config+Tokens und meldet den User', () =>
  withTempConfigDir(async (dir) => {
    const fetchImpl = async (url) => {
      if (url.includes('/auth/device')) {
        return jsonResponse(200, { device_code: 'dc', user_code: 'CODE-1', verification_uri: 'http://kc/device', interval: 0, expires_in: 600 });
      }
      return jsonResponse(200, { access_token: fakeJwt({ preferred_username: 'alice' }), refresh_token: 'rt', expires_in: 300 });
    };
    const i = io({ fetchImpl, sleepImpl: async () => {}, baseDir: dir });
    const code = await main(['auth', 'login', '--host', 'http://api', '--keycloak-url', 'http://kc', '--realm', 'dev'], i);

    assert.equal(code, 0);
    assert.match(i.stdoutLines.join(''), /"username": "alice"/);
    assert.deepEqual(readJsonFile(configPath(dir)), { host: 'http://api', keycloakUrl: 'http://kc', realm: 'dev' });
    assert.ok(readJsonFile(tokensPath(dir)).access_token);
  }));

test('main auth status: meldet nicht angemeldet mit Exit 1, wenn keine Tokens vorliegen', () =>
  withTempConfigDir(async (dir) => {
    const i = io({ baseDir: dir });
    const code = await main(['auth', 'status'], i);
    assert.equal(code, 1);
    assert.match(i.stderrLines.join(''), /Nicht angemeldet/);
  }));

test('main auth status: zeigt User und Restlaufzeit, wenn angemeldet', () =>
  withTempConfigDir(async (dir) => {
    writeJsonFileSecure(configPath(dir), { host: 'http://api', keycloakUrl: 'http://kc', realm: 'dev' });
    writeJsonFileSecure(tokensPath(dir), {
      access_token: fakeJwt({ preferred_username: 'bob' }),
      refresh_token: 'rt',
      expires_at: Date.now() + 120_000,
    });
    const i = io({ baseDir: dir });
    const code = await main(['auth', 'status'], i);
    assert.equal(code, 0);
    const parsed = JSON.parse(i.stdoutLines.join(''));
    assert.equal(parsed.username, 'bob');
    assert.ok(parsed.accessTokenExpiresInSeconds > 0);
  }));

test('main auth logout: entfernt die Token-Datei', () =>
  withTempConfigDir(async (dir) => {
    writeJsonFileSecure(configPath(dir), { host: 'h', keycloakUrl: 'k', realm: 'r' });
    writeJsonFileSecure(tokensPath(dir), { access_token: 'x' });
    const code = await main(['auth', 'logout'], io({ baseDir: dir }));
    assert.equal(code, 0);
    assert.equal(readJsonFile(tokensPath(dir)), null);
  }));

// --- Kein Secret-Leak in Ausgaben ----------------------------------------------

test('main auth login: das Access-Token erscheint nicht im Klartext auf stdout', () =>
  withTempConfigDir(async (dir) => {
    const secretToken = fakeJwt({ preferred_username: 'carol' });
    const fetchImpl = async (url) => {
      if (url.includes('/auth/device')) {
        return jsonResponse(200, { device_code: 'dc', user_code: 'CODE-2', verification_uri: 'http://kc/device', interval: 0, expires_in: 600 });
      }
      return jsonResponse(200, { access_token: secretToken, refresh_token: 'rt-secret', expires_in: 300 });
    };
    const i = io({ fetchImpl, sleepImpl: async () => {}, baseDir: dir });
    await main(['auth', 'login', '--host', 'http://api', '--keycloak-url', 'http://kc', '--realm', 'dev'], i);

    const allOutput = i.stdoutLines.join('') + i.stderrLines.join('');
    assert.ok(!allOutput.includes(secretToken));
    assert.ok(!allOutput.includes('rt-secret'));
  }));
