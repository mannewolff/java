import { describe, expect, it } from 'vitest';
import {
  EXCERPT_DEFAULT_PCT,
  EXCERPT_MAX_PCT,
  EXCERPT_MIN_PCT,
  clampExcerptWidth,
  stripMarkdown,
} from './listExcerpt';

describe('clampExcerptWidth', () => {
  it('lässt Werte im gültigen Bereich unverändert', () => {
    expect(clampExcerptWidth(50)).toBe(50);
    expect(clampExcerptWidth(EXCERPT_MIN_PCT)).toBe(EXCERPT_MIN_PCT);
    expect(clampExcerptWidth(EXCERPT_MAX_PCT)).toBe(EXCERPT_MAX_PCT);
  });

  it('klemmt zu kleine Werte auf das Minimum', () => {
    expect(clampExcerptWidth(5)).toBe(EXCERPT_MIN_PCT);
    expect(clampExcerptWidth(-100)).toBe(EXCERPT_MIN_PCT);
  });

  it('klemmt zu große Werte auf das Maximum', () => {
    expect(clampExcerptWidth(99)).toBe(EXCERPT_MAX_PCT);
  });

  it('fällt bei NaN auf den Default zurück', () => {
    expect(clampExcerptWidth(Number.NaN)).toBe(EXCERPT_DEFAULT_PCT);
  });
});

describe('stripMarkdown', () => {
  it('entfernt Überschriften-Hashes, Betonung und Backticks', () => {
    expect(stripMarkdown('# Titel **fett** `code` _kursiv_')).toBe('Titel fett code kursiv');
  });

  it('kollabiert Zeilenumbrüche und Mehrfach-Whitespace zu einem Leerzeichen', () => {
    expect(stripMarkdown('Zeile 1\nZeile 2\n\nZeile 3')).toBe('Zeile 1 Zeile 2 Zeile 3');
  });

  it('liefert für leeren Body einen leeren String', () => {
    expect(stripMarkdown('')).toBe('');
  });
});
