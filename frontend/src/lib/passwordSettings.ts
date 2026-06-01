import { DEFAULT_SPECIALS } from './password';

/** Persistierte Einstellungen des Passwortgenerators (#178). */
export interface PasswordSettings {
  length: number;
  useUpper: boolean;
  useLower: boolean;
  useDigits: boolean;
  specialsActive: boolean;
  /** Ausgewählte Sonderzeichen, Teilmenge von DEFAULT_SPECIALS. Leeres Array = bewusst keine. */
  specialsPicked: string[];
  costFactor: number;
}

/** Grenzen — identisch zu den Slidern in PasswordPage. */
export const PASSWORD_SETTINGS_LIMITS = {
  minLength: 8,
  maxLength: 64,
  minCost: 8,
  maxCost: 12,
} as const;

export const DEFAULT_PASSWORD_SETTINGS: PasswordSettings = {
  length: 20,
  useUpper: true,
  useLower: true,
  useDigits: true,
  specialsActive: true,
  specialsPicked: [...DEFAULT_SPECIALS],
  costFactor: 10,
};

/** localStorage-Keys — einzeln, damit sie in den DevTools sichtbar/inspizierbar sind (#178). */
const STORAGE_KEYS = {
  length: 'password-generator-length',
  useUpper: 'password-generator-useUpper',
  useLower: 'password-generator-useLower',
  useDigits: 'password-generator-useDigits',
  specialsActive: 'password-generator-specialsActive',
  specialsPicked: 'password-generator-specialsPicked',
  costFactor: 'password-generator-costFactor',
} as const;

/** localStorage kann fehlen (SSR/Tests) oder werfen (Private Mode, Quota) — immer defensiv. */
function readRaw(key: string): string | null {
  try {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeRaw(key: string, value: string): void {
  try {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(key, value);
  } catch {
    /* Schreiben darf nie crashen — Einstellung bleibt dann einfach unpersistiert. */
  }
}

function parseJson(raw: string | null): unknown {
  if (raw === null) return undefined;
  try {
    return JSON.parse(raw);
  } catch {
    return undefined;
  }
}

function loadNumber(key: string, fallback: number, min: number, max: number): number {
  const v = parseJson(readRaw(key));
  if (typeof v === 'number' && Number.isFinite(v)) {
    return Math.min(max, Math.max(min, Math.round(v)));
  }
  return fallback;
}

function loadBoolean(key: string, fallback: boolean): boolean {
  const v = parseJson(readRaw(key));
  return typeof v === 'boolean' ? v : fallback;
}

function loadSpecials(key: string, fallback: string[]): string[] {
  const v = parseJson(readRaw(key));
  if (Array.isArray(v)) {
    // Auf bekannte Zeichen reduzieren und in kanonische DEFAULT_SPECIALS-Reihenfolge bringen.
    return DEFAULT_SPECIALS.filter((c) => v.includes(c));
  }
  return fallback;
}

const L = PASSWORD_SETTINGS_LIMITS;
const D = DEFAULT_PASSWORD_SETTINGS;

/** Lädt die Einstellungen aus localStorage; jeder Key fällt einzeln auf seinen Default zurück. */
export function loadPasswordSettings(): PasswordSettings {
  return {
    length: loadNumber(STORAGE_KEYS.length, D.length, L.minLength, L.maxLength),
    useUpper: loadBoolean(STORAGE_KEYS.useUpper, D.useUpper),
    useLower: loadBoolean(STORAGE_KEYS.useLower, D.useLower),
    useDigits: loadBoolean(STORAGE_KEYS.useDigits, D.useDigits),
    specialsActive: loadBoolean(STORAGE_KEYS.specialsActive, D.specialsActive),
    specialsPicked: loadSpecials(STORAGE_KEYS.specialsPicked, D.specialsPicked),
    costFactor: loadNumber(STORAGE_KEYS.costFactor, D.costFactor, L.minCost, L.maxCost),
  };
}

/** Speichert alle Einstellungen einzeln als JSON unter den password-generator-* Keys. */
export function savePasswordSettings(settings: PasswordSettings): void {
  writeRaw(STORAGE_KEYS.length, JSON.stringify(settings.length));
  writeRaw(STORAGE_KEYS.useUpper, JSON.stringify(settings.useUpper));
  writeRaw(STORAGE_KEYS.useLower, JSON.stringify(settings.useLower));
  writeRaw(STORAGE_KEYS.useDigits, JSON.stringify(settings.useDigits));
  writeRaw(STORAGE_KEYS.specialsActive, JSON.stringify(settings.specialsActive));
  writeRaw(STORAGE_KEYS.specialsPicked, JSON.stringify(settings.specialsPicked));
  writeRaw(STORAGE_KEYS.costFactor, JSON.stringify(settings.costFactor));
}
