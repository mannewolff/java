import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { DEFAULT_SPECIALS } from './password';
import {
  DEFAULT_PASSWORD_SETTINGS,
  loadPasswordSettings,
  savePasswordSettings,
  type PasswordSettings,
} from './passwordSettings';

describe('passwordSettings', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => localStorage.clear());

  it('liefert die Defaults, wenn localStorage leer ist (First Visit)', () => {
    expect(loadPasswordSettings()).toEqual(DEFAULT_PASSWORD_SETTINGS);
  });

  it('speichert alle Werte unter password-generator-* Keys', () => {
    const settings: PasswordSettings = {
      length: 32,
      useUpper: false,
      useLower: true,
      useDigits: false,
      specialsActive: true,
      specialsPicked: [DEFAULT_SPECIALS[0], DEFAULT_SPECIALS[1]],
      costFactor: 12,
    };
    savePasswordSettings(settings);

    expect(JSON.parse(localStorage.getItem('password-generator-length')!)).toBe(32);
    expect(JSON.parse(localStorage.getItem('password-generator-useUpper')!)).toBe(false);
    expect(JSON.parse(localStorage.getItem('password-generator-useDigits')!)).toBe(false);
    expect(JSON.parse(localStorage.getItem('password-generator-specialsActive')!)).toBe(true);
    expect(JSON.parse(localStorage.getItem('password-generator-specialsPicked')!)).toEqual([
      DEFAULT_SPECIALS[0],
      DEFAULT_SPECIALS[1],
    ]);
    expect(JSON.parse(localStorage.getItem('password-generator-costFactor')!)).toBe(12);
  });

  it('round-trip: save dann load ergibt dieselben Werte (Second Visit)', () => {
    const settings: PasswordSettings = {
      length: 24,
      useUpper: true,
      useLower: false,
      useDigits: true,
      specialsActive: false,
      specialsPicked: [DEFAULT_SPECIALS[2]],
      costFactor: 11,
    };
    savePasswordSettings(settings);
    expect(loadPasswordSettings()).toEqual(settings);
  });

  it('fällt bei ungültigem JSON pro Key auf den Default zurück (Invalid JSON)', () => {
    localStorage.setItem('password-generator-length', 'nicht-json');
    localStorage.setItem('password-generator-useUpper', '{kaputt');
    expect(loadPasswordSettings().length).toBe(DEFAULT_PASSWORD_SETTINGS.length);
    expect(loadPasswordSettings().useUpper).toBe(DEFAULT_PASSWORD_SETTINGS.useUpper);
  });

  it('klemmt die Länge in den gültigen Bereich (8–64)', () => {
    localStorage.setItem('password-generator-length', JSON.stringify(999));
    expect(loadPasswordSettings().length).toBe(64);
    localStorage.setItem('password-generator-length', JSON.stringify(1));
    expect(loadPasswordSettings().length).toBe(8);
  });

  it('klemmt den Cost-Faktor in den gültigen Bereich (8–12)', () => {
    localStorage.setItem('password-generator-costFactor', JSON.stringify(31));
    expect(loadPasswordSettings().costFactor).toBe(12);
    localStorage.setItem('password-generator-costFactor', JSON.stringify(4));
    expect(loadPasswordSettings().costFactor).toBe(8);
  });

  it('ignoriert beim Wert-Typ-Mismatch (z. B. boolean als Zahl) und nimmt Default', () => {
    localStorage.setItem('password-generator-useDigits', JSON.stringify(123));
    expect(loadPasswordSettings().useDigits).toBe(DEFAULT_PASSWORD_SETTINGS.useDigits);
  });

  it('filtert unbekannte Sonderzeichen heraus und behält die kanonische Reihenfolge', () => {
    localStorage.setItem(
      'password-generator-specialsPicked',
      JSON.stringify(['NICHT-ERLAUBT', DEFAULT_SPECIALS[1], DEFAULT_SPECIALS[0]]),
    );
    // Reihenfolge folgt DEFAULT_SPECIALS, Müll ist raus.
    expect(loadPasswordSettings().specialsPicked).toEqual([DEFAULT_SPECIALS[0], DEFAULT_SPECIALS[1]]);
  });

  it('akzeptiert eine leere Sonderzeichen-Auswahl als gültigen Zustand', () => {
    localStorage.setItem('password-generator-specialsPicked', JSON.stringify([]));
    expect(loadPasswordSettings().specialsPicked).toEqual([]);
  });

  it('fällt bei nicht-Array Sonderzeichen auf die Default-Auswahl zurück', () => {
    localStorage.setItem('password-generator-specialsPicked', JSON.stringify('foo'));
    expect(loadPasswordSettings().specialsPicked).toEqual(DEFAULT_PASSWORD_SETTINGS.specialsPicked);
  });
});
