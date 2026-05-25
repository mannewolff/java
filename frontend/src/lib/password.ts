import bcrypt from 'bcryptjs';

export interface GenerateOptions {
  length: number;
  useUpper: boolean;
  useLower: boolean;
  useDigits: boolean;
  /** Empty array disables the special-character class entirely. */
  specials: string[];
}

const UPPER = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
const LOWER = 'abcdefghijklmnopqrstuvwxyz';
const DIGITS = '0123456789';

/** Default set of special characters offered in the UI. */
export const DEFAULT_SPECIALS: string[] = [
  '!', '@', '#', '$', '%', '^', '&', '*',
  '(', ')', '-', '_', '=', '+', '[', ']',
  '{', '}', '|', ';', ':', ',', '.', '<',
  '>', '?', '/',
];

/**
 * Picks a uniformly random integer in [0, max) using crypto.getRandomValues
 * with rejection sampling, so the result is not biased even when max is not
 * a power of two.
 */
function secureRandomInt(max: number): number {
  if (max <= 0) {
    throw new Error('secureRandomInt: max must be positive');
  }
  const limit = Math.floor(0xffffffff / max) * max;
  const buf = new Uint32Array(1);
  let n;
  do {
    crypto.getRandomValues(buf);
    n = buf[0];
  } while (n >= limit);
  return n % max;
}

function pick(alphabet: string): string {
  return alphabet.charAt(secureRandomInt(alphabet.length));
}

function shuffle(chars: string[]): string[] {
  for (let i = chars.length - 1; i > 0; i--) {
    const j = secureRandomInt(i + 1);
    [chars[i], chars[j]] = [chars[j], chars[i]];
  }
  return chars;
}

/**
 * Generates a password according to the options. Guarantees that every
 * enabled character class is represented by at least one character in the
 * output. Throws if no class is active or if the requested length is below
 * the number of active classes.
 */
export function generate(options: GenerateOptions): string {
  const { length, useUpper, useLower, useDigits, specials } = options;
  if (length < 1) {
    throw new Error('generate: length must be at least 1');
  }
  const classes: string[] = [];
  if (useUpper) classes.push(UPPER);
  if (useLower) classes.push(LOWER);
  if (useDigits) classes.push(DIGITS);
  if (specials.length > 0) classes.push(specials.join(''));
  if (classes.length === 0) {
    throw new Error('generate: at least one character class must be active');
  }
  if (length < classes.length) {
    throw new Error('generate: length must be at least the number of active classes');
  }
  const required = classes.map((c) => pick(c));
  const alphabet = classes.join('');
  const remaining: string[] = [];
  for (let i = 0; i < length - classes.length; i++) {
    remaining.push(pick(alphabet));
  }
  return shuffle([...required, ...remaining]).join('');
}

/**
 * Computes a bcrypt hash for the given password using the supplied cost
 * factor. Returns a promise so the call site can keep the UI responsive
 * for higher cost factors.
 */
export async function hashBcrypt(password: string, costFactor: number): Promise<string> {
  if (costFactor < 4 || costFactor > 31) {
    throw new Error('hashBcrypt: cost factor must be between 4 and 31');
  }
  return bcrypt.hash(password, costFactor);
}

/**
 * Returns the Shannon entropy estimate in bits for a password of the given
 * length drawn from an alphabet of the given size. Used for the caption in
 * the UI.
 */
export function entropyBits(length: number, alphabetSize: number): number {
  if (length <= 0 || alphabetSize <= 0) return 0;
  return length * Math.log2(alphabetSize);
}
