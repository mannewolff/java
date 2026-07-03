/**
 * Reine Hilfsfunktionen fuer die Kanban-Listenansicht (#282). Bewusst frei von React und
 * localStorage, damit Klemm- und Strip-Logik deterministisch unit-testbar ist.
 */

/** Minimale, maximale und Default-Breite der Excerpt-Spalte in Prozent. */
export const EXCERPT_MIN_PCT = 25;
export const EXCERPT_MAX_PCT = 75;
export const EXCERPT_DEFAULT_PCT = 50;

/** Klemmt einen Prozentwert in den erlaubten Bereich; NaN faellt auf den Default. */
export function clampExcerptWidth(pct: number): number {
  if (Number.isNaN(pct)) return EXCERPT_DEFAULT_PCT;
  return Math.min(EXCERPT_MAX_PCT, Math.max(EXCERPT_MIN_PCT, pct));
}

/**
 * Strippt Markdown-Steuerzeichen aus einem Body und macht daraus eine einzeilige Vorschau.
 * Entfernt Ueberschriften-Hashes, Betonungszeichen und Backticks, kollabiert Zeilenumbrueche.
 */
export function stripMarkdown(raw: string): string {
  return raw
    .replace(/\r?\n/g, ' ')
    .replace(/#+\s*/g, '')
    .replace(/[*_`]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}
