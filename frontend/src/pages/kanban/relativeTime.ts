const RTF = new Intl.RelativeTimeFormat('de', { numeric: 'auto' });

const THRESHOLDS: ReadonlyArray<[limitSeconds: number, perUnit: number, unit: Intl.RelativeTimeFormatUnit]> =
  [
    [60, 1, 'second'],
    [3600, 60, 'minute'],
    [86_400, 3600, 'hour'],
    [2_592_000, 86_400, 'day'],
    [31_536_000, 2_592_000, 'month'],
    [Infinity, 31_536_000, 'year'],
  ];

/**
 * Formatiert einen ISO-Zeitstempel als relatives, deutsches Label (z. B. „vor 2 Stunden"). Reine
 * Funktion mit injizierbarem {@code now}, damit Komponenten sie deterministisch testen koennen.
 */
export function relativeTime(iso: string, now: number = Date.now()): string {
  const deltaSeconds = (new Date(iso).getTime() - now) / 1000;
  const abs = Math.abs(deltaSeconds);
  for (const [limit, perUnit, unit] of THRESHOLDS) {
    if (abs < limit) {
      return RTF.format(Math.round(deltaSeconds / perUnit), unit);
    }
  }
  return RTF.format(Math.round(deltaSeconds / 31_536_000), 'year');
}
