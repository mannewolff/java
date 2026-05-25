import { describe, expect, it } from 'vitest';
import bcrypt from 'bcryptjs';
import { DEFAULT_SPECIALS, entropyBits, generate, hashBcrypt } from './password';

const SPECIAL_REGEX_CHARS = new Set(['(', ')', '[', ']', '{', '}', '|', '.', '*', '+', '?', '$', '^', '/']);

function escape(char: string): string {
  return SPECIAL_REGEX_CHARS.has(char) ? '\\' + char : char;
}

describe('generate', () => {
  it('produces a password of the requested length', () => {
    const password = generate({
      length: 20,
      useUpper: true,
      useLower: true,
      useDigits: true,
      specials: DEFAULT_SPECIALS,
    });
    expect(password).toHaveLength(20);
  });

  it('honours the minimum length of 8', () => {
    const password = generate({
      length: 8,
      useUpper: true,
      useLower: true,
      useDigits: true,
      specials: DEFAULT_SPECIALS,
    });
    expect(password).toHaveLength(8);
  });

  it('honours the maximum length of 64', () => {
    const password = generate({
      length: 64,
      useUpper: true,
      useLower: true,
      useDigits: true,
      specials: DEFAULT_SPECIALS,
    });
    expect(password).toHaveLength(64);
  });

  it('limits the output to uppercase letters when only uppercase is active', () => {
    const password = generate({
      length: 20,
      useUpper: true,
      useLower: false,
      useDigits: false,
      specials: [],
    });
    expect(password).toMatch(/^[A-Z]+$/);
  });

  it('only uses the explicitly chosen specials, no other punctuation leaks in', () => {
    const chosen = ['!', '@'];
    const password = generate({
      length: 50,
      useUpper: false,
      useLower: false,
      useDigits: false,
      specials: chosen,
    });
    const allowed = chosen.map(escape).join('');
    const regex = new RegExp('^[' + allowed + ']+$');
    expect(password).toMatch(regex);
  });

  it('includes at least one character from every active class', () => {
    // Run a few times because of randomness; even one in 50 should hit each class.
    for (let i = 0; i < 5; i++) {
      const password = generate({
        length: 20,
        useUpper: true,
        useLower: true,
        useDigits: true,
        specials: ['!'],
      });
      expect(password).toMatch(/[A-Z]/);
      expect(password).toMatch(/[a-z]/);
      expect(password).toMatch(/[0-9]/);
      expect(password).toContain('!');
    }
  });

  it('throws when no class is active', () => {
    expect(() =>
      generate({
        length: 20,
        useUpper: false,
        useLower: false,
        useDigits: false,
        specials: [],
      }),
    ).toThrow(/at least one character class/);
  });

  it('throws when the requested length is below the number of active classes', () => {
    expect(() =>
      generate({
        length: 2,
        useUpper: true,
        useLower: true,
        useDigits: true,
        specials: ['!'],
      }),
    ).toThrow(/at least the number of active classes/);
  });

  it('throws for length < 1', () => {
    expect(() =>
      generate({
        length: 0,
        useUpper: true,
        useLower: false,
        useDigits: false,
        specials: [],
      }),
    ).toThrow(/length must be at least 1/);
  });
});

describe('hashBcrypt', () => {
  it('produces a hash that verifies against the original password', async () => {
    const hash = await hashBcrypt('hunter2', 4);
    expect(hash).toMatch(/^\$2[aby]\$04\$/);
    const ok = await bcrypt.compare('hunter2', hash);
    expect(ok).toBe(true);
  });

  it('rejects cost factors outside 4..31', async () => {
    await expect(hashBcrypt('x', 3)).rejects.toThrow(/cost factor/);
    await expect(hashBcrypt('x', 32)).rejects.toThrow(/cost factor/);
  });
});

describe('entropyBits', () => {
  it('returns length * log2(alphabet) for valid inputs', () => {
    expect(entropyBits(20, 64)).toBeCloseTo(120, 5);
  });

  it('returns 0 for non-positive inputs', () => {
    expect(entropyBits(0, 64)).toBe(0);
    expect(entropyBits(20, 0)).toBe(0);
  });
});
