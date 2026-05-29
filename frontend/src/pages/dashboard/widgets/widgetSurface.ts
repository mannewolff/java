export interface WidgetSurfaceConfig {
  /** Lese-Modus: leichter Rahmen anzeigen. Default false. */
  showBorder?: boolean;
  /** Lese-Modus: CSS-Hintergrundfarbe; leer/undefined = transparent. */
  backgroundColor?: string;
}

export interface WidgetSurface {
  variant: 'outlined' | 'elevation';
  elevation: number;
  sx: { bgcolor?: string };
}

/**
 * Berechnet Paper-Variante + sx-Fragment fuer das aeussere Widget-Paper.
 *
 * Edit-Modus ({@code readOnly === false}): immer {@code outlined} mit Default-Hintergrund — der
 * User soll die Kachelgrenzen sehen, unabhaengig von der Lese-Modus-Konfiguration.
 *
 * Lese-Modus: die Kachel ist standardmaessig unsichtbar (kein Rahmen, transparenter Hintergrund).
 * {@code showBorder} aktiviert den {@code outlined}-Rahmen, {@code backgroundColor} setzt eine
 * Hintergrundfarbe.
 */
export function widgetSurface(readOnly: boolean, cfg: WidgetSurfaceConfig): WidgetSurface {
  if (!readOnly) {
    return { variant: 'outlined', elevation: 0, sx: {} };
  }
  const bg = cfg.backgroundColor?.trim();
  return {
    variant: cfg.showBorder === true ? 'outlined' : 'elevation',
    elevation: 0,
    sx: { bgcolor: bg != null && bg !== '' ? bg : 'transparent' },
  };
}

/** Liest die beiden Darstellungs-Felder defensiv aus einer geparsten Config. */
export function parseSurfaceConfig(parsed: Record<string, unknown>): {
  showBorder: boolean;
  backgroundColor?: string;
} {
  const bg =
    typeof parsed.backgroundColor === 'string' && parsed.backgroundColor.trim() !== ''
      ? parsed.backgroundColor
      : undefined;
  return { showBorder: parsed.showBorder === true, backgroundColor: bg };
}
