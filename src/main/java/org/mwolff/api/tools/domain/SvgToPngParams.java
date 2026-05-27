package org.mwolff.api.tools.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Eingabeparameter für die SVG → PNG-Konvertierung. Width und Height optional — fehlend bedeutet,
 * dass die SVG-eigene Geometrie verwendet wird. Background ist entweder {@code "transparent"} oder
 * ein hexadezimaler {@code #rrggbb}-String (case-insensitive).
 */
public record SvgToPngParams(Integer width, Integer height, String background) {

  /** Erlaubte Background-Patterns. Spiegelt den Server-Pattern aus python-tools/main.py wider. */
  public static final Pattern BACKGROUND_PATTERN =
      Pattern.compile("^(transparent|#[0-9a-fA-F]{6})$");

  /** Sentinel für den Default-Hintergrund. */
  public static final String TRANSPARENT = "transparent";

  public SvgToPngParams {
    if (width != null && width < 1) {
      throw new IllegalArgumentException("width must be >= 1");
    }
    if (height != null && height < 1) {
      throw new IllegalArgumentException("height must be >= 1");
    }
    Objects.requireNonNull(background, "background must not be null");
    if (!BACKGROUND_PATTERN.matcher(background).matches()) {
      throw new IllegalArgumentException("background must be 'transparent' or '#rrggbb'");
    }
  }
}
