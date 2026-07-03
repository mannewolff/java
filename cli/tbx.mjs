#!/usr/bin/env node
/**
 * tbx.mjs — Kommandozeilen-Client fuer die Toolbox-Kanban-API.
 * Single-File, zero dependencies (natives fetch). Lauffaehig als Kopie ohne
 * weiteren Repo-Kontext.
 *
 * Nutzung:
 *   tbx auth login [--host <url>] [--keycloak-url <url>] [--realm <name>]
 *   tbx auth status
 *   tbx auth logout
 *
 * Ausgabe: JSON auf stdout, Fehler auf stderr, Exit-Code 1 bei Fehlern.
 */

import { pathToFileURL } from 'node:url';
import { mkdirSync, readFileSync, writeFileSync, chmodSync, rmSync, existsSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join } from 'node:path';

const CLIENT_ID = 'toolbox-cli';

const PROD_DEFAULTS = {
  host: 'https://toolbox.mwolff.org',
  keycloakUrl: 'https://toolboxauth.mwolff.org',
  realm: 'toolbox',
};

const HELP = `tbx — Kommandozeilen-Client fuer die Toolbox-Kanban-API

Nutzung:
  tbx auth login [--host <url>] [--keycloak-url <url>] [--realm <name>]
  tbx auth status
  tbx auth logout

Defaults (Produktion): --host ${PROD_DEFAULTS.host} --keycloak-url ${PROD_DEFAULTS.keycloakUrl} --realm ${PROD_DEFAULTS.realm}
Dev-Beispiel: tbx auth login --host http://localhost:8080 --keycloak-url http://localhost:8081 --realm toolbox-dev
`;

// --- Storage ---------------------------------------------------------------

/**
 * Basisverzeichnis fuer Config/Tokens. `baseDir` wird explizit durchgereicht (nicht
 * ueber ein globales env var gelesen) — Tests koennen so parallel laufen, ohne sich
 * über ein gemeinsames Mutable-Global (process.env) gegenseitig zu stoeren.
 */
export function configDir(baseDir) {
  return baseDir || process.env.TBX_CONFIG_DIR || join(homedir(), '.config', 'toolbox-cli');
}

export function configPath(baseDir) {
  return join(configDir(baseDir), 'config.json');
}

export function tokensPath(baseDir) {
  return join(configDir(baseDir), 'tokens.json');
}

export function readJsonFile(path) {
  if (!existsSync(path)) return null;
  try {
    return JSON.parse(readFileSync(path, 'utf-8'));
  } catch {
    return null;
  }
}

/** Schreibt JSON mit 0600-Rechten — chmod nach dem Schreiben, damit ein bereits
 * bestehendes File (anderer Umask/Vor-Zustand) garantiert auf 0600 landet. */
export function writeJsonFileSecure(path, obj) {
  mkdirSync(dirname(path), { recursive: true, mode: 0o700 });
  writeFileSync(path, JSON.stringify(obj, null, 2), { mode: 0o600 });
  chmodSync(path, 0o600);
}

export function deleteFile(path) {
  if (existsSync(path)) rmSync(path);
}

// --- Config-Aufloesung -------------------------------------------------------

export function resolveConfig(flags, storedConfig) {
  return {
    host: flags.host || storedConfig?.host || PROD_DEFAULTS.host,
    keycloakUrl: flags['keycloak-url'] || storedConfig?.keycloakUrl || PROD_DEFAULTS.keycloakUrl,
    realm: flags.realm || storedConfig?.realm || PROD_DEFAULTS.realm,
  };
}

// --- JWT / Ablauf ------------------------------------------------------------

export function decodeJwtPayload(token) {
  const part = token.split('.')[1];
  const normalized = part.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
  return JSON.parse(Buffer.from(padded, 'base64').toString('utf-8'));
}

export function computeExpiry(expiresInSeconds, now = Date.now()) {
  return now + expiresInSeconds * 1000;
}

/** True, wenn expiresAt innerhalb von bufferMs erreicht ist oder schon vorbei. */
export function isExpired(expiresAt, now = Date.now(), bufferMs = 30_000) {
  return now >= expiresAt - bufferMs;
}

// --- Argument-Parser ---------------------------------------------------------

export function parseArgs(argv) {
  const result = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const key = a.slice(2);
      const next = argv[i + 1];
      if (next !== undefined && !next.startsWith('--')) {
        result[key] = next;
        i++;
      } else {
        result[key] = true;
      }
    } else {
      result._.push(a);
    }
  }
  return result;
}

// --- Fehler ------------------------------------------------------------------

export class AuthError extends Error {
  constructor(message, reason) {
    super(message);
    this.reason = reason;
  }
}

// --- Device Flow ---------------------------------------------------------------

export async function requestDeviceCode(cfg, fetchImpl = fetch) {
  const res = await fetchImpl(
    `${cfg.keycloakUrl}/realms/${cfg.realm}/protocol/openid-connect/auth/device`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ client_id: CLIENT_ID, scope: 'openid offline_access' }),
    },
  );
  if (!res.ok) {
    throw new AuthError(`Device-Code konnte nicht angefordert werden (HTTP ${res.status})`, 'device_request_failed');
  }
  return res.json();
}

/**
 * Pollt den Token-Endpoint bis Login bestaetigt wurde oder ein Endzustand
 * eintritt. sleepImpl ist injizierbar, damit Tests nicht real warten.
 */
export async function pollDeviceToken(
  cfg,
  device,
  { fetchImpl = fetch, sleepImpl = (ms) => new Promise((r) => setTimeout(r, ms)), onWaiting } = {},
) {
  let interval = device.interval || 5;
  const deadline = Date.now() + (device.expires_in || 600) * 1000;

  while (Date.now() < deadline) {
    await sleepImpl(interval * 1000);
    const res = await fetchImpl(
      `${cfg.keycloakUrl}/realms/${cfg.realm}/protocol/openid-connect/token`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
          client_id: CLIENT_ID,
          device_code: device.device_code,
        }),
      },
    );
    const body = await res.json();
    if (res.ok) return body;

    switch (body.error) {
      case 'authorization_pending':
        onWaiting?.('pending');
        continue;
      case 'slow_down':
        interval += 5;
        onWaiting?.('slow_down');
        continue;
      case 'access_denied':
        throw new AuthError('Login wurde abgelehnt.', 'access_denied');
      case 'expired_token':
        throw new AuthError('Der Anmelde-Code ist abgelaufen. Bitte erneut versuchen: tbx auth login', 'expired_token');
      default:
        throw new AuthError(`Unerwarteter Fehler beim Login: ${body.error || res.status}`, 'unknown');
    }
  }
  throw new AuthError('Der Anmelde-Code ist abgelaufen. Bitte erneut versuchen: tbx auth login', 'expired_token');
}

// --- Refresh -------------------------------------------------------------------

export async function refreshTokens(cfg, refreshToken, fetchImpl = fetch) {
  const res = await fetchImpl(`${cfg.keycloakUrl}/realms/${cfg.realm}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: CLIENT_ID,
      refresh_token: refreshToken,
    }),
  });
  const body = await res.json();
  if (!res.ok) {
    throw new AuthError('Bitte erneut anmelden: tbx auth login', 'refresh_failed');
  }
  return body;
}

function tokensFromResponse(body, now = Date.now()) {
  return {
    access_token: body.access_token,
    refresh_token: body.refresh_token,
    expires_at: computeExpiry(body.expires_in, now),
  };
}

// --- apiFetch: authentifizierter Aufruf gegen die Toolbox-API ------------------

/**
 * Fuehrt einen Bearer-authentifizierten Request gegen `${config.host}${path}` aus.
 * Refresht das Access-Token automatisch, wenn es abgelaufen ist. Wirft AuthError
 * mit reason 'not_logged_in' bzw. 'refresh_failed', wenn kein gueltiges Login
 * vorliegt.
 */
export async function apiFetch(path, options = {}, { fetchImpl = fetch, baseDir } = {}) {
  const config = readJsonFile(configPath(baseDir));
  let tokens = readJsonFile(tokensPath(baseDir));
  if (!config || !tokens) {
    throw new AuthError('Nicht angemeldet. Bitte zuerst: tbx auth login', 'not_logged_in');
  }

  if (isExpired(tokens.expires_at)) {
    const refreshed = await refreshTokens(config, tokens.refresh_token, fetchImpl);
    tokens = tokensFromResponse(refreshed);
    writeJsonFileSecure(tokensPath(baseDir), tokens);
  }

  return fetchImpl(`${config.host}${path}`, {
    ...options,
    headers: { ...(options.headers || {}), Authorization: `Bearer ${tokens.access_token}` },
  });
}

// --- Kommandos -----------------------------------------------------------------

async function cmdLogin(flags, io) {
  const storedConfig = readJsonFile(configPath(io.baseDir));
  const cfg = resolveConfig(flags, storedConfig);

  const device = await requestDeviceCode(cfg, io.fetchImpl);
  io.stderr(`Öffne ${device.verification_uri_complete || device.verification_uri} und bestätige den Code: ${device.user_code}\n`);

  const tokenResponse = await pollDeviceToken(cfg, device, {
    fetchImpl: io.fetchImpl,
    sleepImpl: io.sleepImpl,
    onWaiting: () => io.stderr('.'),
  });

  const tokens = tokensFromResponse(tokenResponse);
  writeJsonFileSecure(configPath(io.baseDir), cfg);
  writeJsonFileSecure(tokensPath(io.baseDir), tokens);

  const claims = decodeJwtPayload(tokenResponse.access_token);
  io.stdout(JSON.stringify({ ok: true, username: claims.preferred_username, host: cfg.host, realm: cfg.realm }, null, 2) + '\n');
}

function cmdStatus(io) {
  const config = readJsonFile(configPath(io.baseDir));
  const tokens = readJsonFile(tokensPath(io.baseDir));
  if (!config || !tokens) {
    io.stderr('Nicht angemeldet. Bitte zuerst: tbx auth login\n');
    return 1;
  }
  const claims = decodeJwtPayload(tokens.access_token);
  const remainingSeconds = Math.max(0, Math.round((tokens.expires_at - Date.now()) / 1000));
  io.stdout(
    JSON.stringify(
      {
        username: claims.preferred_username,
        host: config.host,
        realm: config.realm,
        accessTokenExpiresInSeconds: remainingSeconds,
      },
      null,
      2,
    ) + '\n',
  );
  return 0;
}

function cmdLogout(io) {
  deleteFile(tokensPath(io.baseDir));
  io.stdout(JSON.stringify({ ok: true }, null, 2) + '\n');
  return 0;
}

// --- Dispatch --------------------------------------------------------------

const defaultIo = () => ({
  stdout: (s) => process.stdout.write(s),
  stderr: (s) => process.stderr.write(s),
  fetchImpl: fetch,
  sleepImpl: (ms) => new Promise((r) => setTimeout(r, ms)),
  baseDir: undefined,
});

/** Gibt den Exit-Code zurueck, statt process.exit() aufzurufen — testbar. */
export async function main(argv, io = defaultIo()) {
  if (argv.length === 0 || argv[0] === '--help' || argv[0] === '-h') {
    io.stdout(HELP);
    return 0;
  }

  const [axis, command, ...rest] = argv;
  const flags = parseArgs(rest);

  if (axis === 'auth') {
    try {
      switch (command) {
        case 'login':
          await cmdLogin(flags, io);
          return 0;
        case 'status':
          return cmdStatus(io);
        case 'logout':
          return cmdLogout(io);
        default:
          io.stdout(HELP);
          io.stderr(`Fehler: Unbekannter auth-Befehl: '${command}'\n`);
          return 1;
      }
    } catch (e) {
      io.stderr(`Fehler: ${e.message}\n`);
      return 1;
    }
  }

  io.stdout(HELP);
  io.stderr(`Fehler: Unbekannte Achse: '${axis}'. Erwartet: auth\n`);
  return 1;
}

const isMainModule = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
if (isMainModule) {
  main(process.argv.slice(2)).then((code) => process.exit(code));
}
