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
import { mkdirSync, readFileSync, writeFileSync, chmodSync, rmSync, existsSync, realpathSync } from 'node:fs';
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

  tbx issue create --title <text> [--body <text>]
  tbx issue get <nummer>
  tbx issue list [--status <status>]
  tbx issue move <nummer> <status>
  tbx issue comment <nummer> --text <text>

Status-Werte: backlog, ready, in_progress, in_review, done

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

/** Schreibt JSON mit 0600-Rechten in ein 0700-Verzeichnis — chmod jeweils nach
 * dem Anlegen/Schreiben, damit ein bereits bestehendes Verzeichnis oder File
 * (anderer Umask/Vor-Zustand, z. B. eine aeltere CLI-Version) garantiert auf
 * die restriktiven Rechte landet; mkdirSync wendet `mode` sonst nur bei
 * tatsaechlicher Neuanlage an. */
export function writeJsonFileSecure(path, obj) {
  const dir = dirname(path);
  mkdirSync(dir, { recursive: true, mode: 0o700 });
  chmodSync(dir, 0o700);
  writeFileSync(path, JSON.stringify(obj, null, 2), { mode: 0o600 });
  chmodSync(path, 0o600);
}

export function deleteFile(path) {
  if (existsSync(path)) rmSync(path);
}

// --- Config-Aufloesung -------------------------------------------------------

/** Wirft, wenn ein Flag ohne folgenden Wert angegeben wurde (parseArgs liefert dann
 * boolean true statt eines Strings) — sonst landet "true" unvalidiert in einer URL. */
function requireStringFlag(flags, name) {
  if (flags[name] === true) {
    throw new CliError(`--${name} erwartet einen Wert`);
  }
  return flags[name];
}

export function resolveConfig(flags, storedConfig) {
  return {
    host: requireStringFlag(flags, 'host') || storedConfig?.host || PROD_DEFAULTS.host,
    keycloakUrl: requireStringFlag(flags, 'keycloak-url') || storedConfig?.keycloakUrl || PROD_DEFAULTS.keycloakUrl,
    realm: requireStringFlag(flags, 'realm') || storedConfig?.realm || PROD_DEFAULTS.realm,
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

// --- Status-Mapping (Kit-Status <-> Backend-Spalte) ---------------------------

export const STATUS_TO_COLUMN = {
  backlog: 'BACKLOG',
  ready: 'READY',
  in_progress: 'IN_PROGRESS',
  in_review: 'IN_REVIEW',
  done: 'DONE',
};

export const COLUMN_TO_STATUS = Object.fromEntries(
  Object.entries(STATUS_TO_COLUMN).map(([status, column]) => [column, status]),
);

export const VALID_STATUSES = Object.keys(STATUS_TO_COLUMN);

/** Allgemeiner CLI-Fehler (Validierung, Not-Found, API-Fehler) — main() faengt ihn wie jeden Error. */
export class CliError extends Error {}

export function toColumn(status) {
  const column = STATUS_TO_COLUMN[status];
  if (!column) {
    throw new CliError(`Ungültiger Status '${status}'. Gültig: ${VALID_STATUSES.join(', ')}`);
  }
  return column;
}

export function toStatus(column) {
  return COLUMN_TO_STATUS[column] || column;
}

// --- Board-Zugriff -------------------------------------------------------------

/** Liest das gruppierte Board und liefert eine flache, mit `status` angereicherte Liste. */
export async function fetchBoardItems(io) {
  const res = await apiFetch('/api/kanban/items', {}, { fetchImpl: io.fetchImpl, baseDir: io.baseDir });
  await ensureOk(res);
  const grouped = await res.json();
  return Object.values(grouped)
    .flat()
    .map((item) => ({ ...item, status: toStatus(item.column) }));
}

export function findItemByNumber(items, number) {
  return items.find((i) => i.number === number) || null;
}

/** Parst ein CLI-Argument als Issue-Nummer; wirft bei nicht-numerischer Eingabe
 * einen klaren CliError statt ein ungeprueftes NaN durchzureichen. */
export function parseIssueNumber(numberArg) {
  const number = Number(numberArg);
  if (Number.isNaN(number)) {
    throw new CliError(`Ungültige Issue-Nummer: '${numberArg}'`);
  }
  return number;
}

async function resolveItemByNumber(number, io) {
  const items = await fetchBoardItems(io);
  const item = findItemByNumber(items, number);
  if (!item) {
    throw new CliError(`Issue ${number} nicht gefunden`);
  }
  return item;
}

/** Wirft bei 401 einen anmelde-spezifischen Fehler, sonst bei Nicht-2xx die Server-Message. */
async function ensureOk(res) {
  if (res.status === 401) {
    throw new CliError('Bitte anmelden: tbx auth login');
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const fieldErrors = body.fieldErrors
      ? ` (${Object.entries(body.fieldErrors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(', ')})`
      : '';
    throw new CliError(`${body.message || `HTTP ${res.status}`}${fieldErrors}`);
  }
  return res;
}

function toGenericIssue(item) {
  return { id: item.number, title: item.title, body: item.body, status: item.status };
}

// --- Kommandos: issue -----------------------------------------------------------

async function cmdIssueCreate(flags, io) {
  if (!flags.title) throw new CliError('--title ist erforderlich');
  const config = readJsonFile(configPath(io.baseDir));
  const res = await apiFetch(
    '/api/kanban/items',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: flags.title, body: flags.body || '', column: 'BACKLOG' }),
    },
    { fetchImpl: io.fetchImpl, baseDir: io.baseDir },
  );
  await ensureOk(res);
  const created = await res.json();
  io.stdout(JSON.stringify({ id: created.number, url: `${config.host}/kanban` }, null, 2) + '\n');
}

async function cmdIssueGet(numberArg, io) {
  const number = parseIssueNumber(numberArg);
  const item = await resolveItemByNumber(number, io);
  io.stdout(JSON.stringify(toGenericIssue(item), null, 2) + '\n');
}

async function cmdIssueList(flags, io) {
  if (flags.status && !VALID_STATUSES.includes(flags.status)) {
    throw new CliError(`Ungültiger Status '${flags.status}'. Gültig: ${VALID_STATUSES.join(', ')}`);
  }
  const items = await fetchBoardItems(io);
  const filtered = (flags.status ? items.filter((i) => i.status === flags.status) : items)
    .slice()
    .sort((a, b) => a.number - b.number);
  io.stdout(JSON.stringify(filtered.map(toGenericIssue), null, 2) + '\n');
}

async function cmdIssueMove(numberArg, statusArg, io) {
  const number = parseIssueNumber(numberArg);
  const column = toColumn(statusArg);
  const items = await fetchBoardItems(io);
  const item = findItemByNumber(items, number);
  if (!item) throw new CliError(`Issue ${number} nicht gefunden`);

  const targetPosition =
    item.column === column ? item.position : items.filter((i) => i.column === column).length;
  const res = await apiFetch(
    `/api/kanban/items/${item.id}/move`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ column, position: targetPosition }),
    },
    { fetchImpl: io.fetchImpl, baseDir: io.baseDir },
  );
  await ensureOk(res);
  io.stdout(JSON.stringify({ ok: true, id: number, status: statusArg }, null, 2) + '\n');
}

async function cmdIssueComment(numberArg, flags, io) {
  if (!flags.text) throw new CliError('--text ist erforderlich');
  const number = parseIssueNumber(numberArg);
  const item = await resolveItemByNumber(number, io);
  const res = await apiFetch(
    `/api/kanban/items/${item.id}/comments`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ body: flags.text }),
    },
    { fetchImpl: io.fetchImpl, baseDir: io.baseDir },
  );
  await ensureOk(res);
  io.stdout(JSON.stringify({ ok: true, id: number }, null, 2) + '\n');
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

  if (axis === 'issue') {
    try {
      switch (command) {
        case 'create':
          await cmdIssueCreate(flags, io);
          return 0;
        case 'get':
          await cmdIssueGet(rest[0], io);
          return 0;
        case 'list':
          await cmdIssueList(flags, io);
          return 0;
        case 'move':
          await cmdIssueMove(rest[0], rest[1], io);
          return 0;
        case 'comment':
          await cmdIssueComment(rest[0], flags, io);
          return 0;
        default:
          io.stdout(HELP);
          io.stderr(`Fehler: Unbekannter issue-Befehl: '${command}'\n`);
          return 1;
      }
    } catch (e) {
      io.stderr(`Fehler: ${e.message}\n`);
      return 1;
    }
  }

  io.stdout(HELP);
  io.stderr(`Fehler: Unbekannte Achse: '${axis}'. Erwartet: auth, issue\n`);
  return 1;
}

/**
 * realpathSync noetig, da import.meta.url immer den aufgeloesten Pfad traegt —
 * ein Aufruf ueber einen symbolischen Link (z.B. macOS /tmp -> /private/tmp,
 * oder ein `~/bin/tbx`-Symlink) wuerde sonst nie erkannt und main() nie laufen.
 * try/catch noetig, falls argv1 nicht (mehr) existiert oder unlesbar ist —
 * realpathSync wirft dann synchron und wuerde das ganze Modul beim Laden
 * crashen statt geordnet "nicht das Hauptmodul" zu erkennen (Issue #299).
 */
export function resolveIsMainModule(argv1, metaUrl) {
  if (!argv1) return false;
  try {
    return metaUrl === pathToFileURL(realpathSync(argv1)).href;
  } catch {
    return false;
  }
}

const isMainModule = resolveIsMainModule(process.argv[1], import.meta.url);
if (isMainModule) {
  main(process.argv.slice(2)).then((code) => process.exit(code));
}
