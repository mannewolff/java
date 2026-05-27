/**
 * Berechnet den verbleibenden Cleanup-Countdown fuer ein Item in der DONE-Spalte. Reine
 * Funktion, damit die Komponente sie deterministisch (mit fixed-Date) testen kann.
 *
 * @param movedToDoneAt ISO-Timestamp, an dem das Item nach DONE wechselte
 * @param retentionDays User-Setting in Tagen
 * @param now aktuelle Zeit (Default {@code Date.now()})
 * @returns Anzahl Tage bis zum Loeschen, mindestens 0
 */
export function cleanupDaysRemaining(
  movedToDoneAt: string,
  retentionDays: number,
  now: number = Date.now(),
): number {
  const moved = new Date(movedToDoneAt).getTime();
  const elapsed = now - moved;
  const remaining = retentionDays * 86_400_000 - elapsed;
  return Math.max(0, Math.ceil(remaining / 86_400_000));
}

/** Liefert den Label-Text fuer den DONE-Cleanup-Hinweis. */
export function cleanupCountdownLabel(days: number): string {
  if (days === 0) return 'wird heute gelöscht';
  if (days === 1) return 'wird morgen gelöscht';
  return `wird in ${days} Tagen gelöscht`;
}
