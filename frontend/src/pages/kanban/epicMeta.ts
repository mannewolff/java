/**
 * Ableitung von Anzeige-Kürzel (Shortcode) und Farbe eines Epics (#324), portiert aus der
 * Kit-Referenz `kit/board-ui.mjs`. Beides ist rein client-seitig abgeleitet — das Backend
 * speichert weder Kürzel noch Farbe. So bleiben Board-Karten-Badges (#325) und Epic-Ansicht
 * (#326) konsistent.
 */

/** Feste Palette mittel-kräftiger Töne (Light Mode) für Epics. */
export const EPIC_PALETTE: readonly string[] = [
  '#534AB7',
  '#1D9E75',
  '#D4537E',
  '#185FA5',
  '#BA7517',
  '#993C1D',
  '#0F6E56',
  '#0C447C',
];

/** Deterministischer Hash über die Epic-ID (djb2-Variante wie board-ui). */
function hashId(id: number): number {
  const s = String(id);
  let h = 0;
  for (let i = 0; i < s.length; i++) {
    h = (h * 31 + s.charCodeAt(i)) >>> 0;
  }
  return h;
}

/** Farbe eines Epics: aus der Palette anhand seiner ID gewählt (stabil pro Epic). */
export function epicColor(id: number): string {
  return EPIC_PALETTE[hashId(id) % EPIC_PALETTE.length];
}

/**
 * Kürzel eines Epics: Initialen der (max. drei ersten) Titelwörter in Großbuchstaben, z. B.
 * „10-Tage Workshop IT-Bildungshaus" → „1WI". Leerer/whitespace-Titel → „EPIC".
 */
export function epicShortcode(title: string): string {
  const words = title.split(/\s+/).filter(Boolean);
  const initials = words
    .map((w) => w[0])
    .join('')
    .slice(0, 3)
    .toUpperCase();
  return initials || 'EPIC';
}
