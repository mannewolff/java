package org.mwolff.api.tools.domain;

import java.util.List;
import java.util.Objects;

/**
 * Ergebnis einer Farbpaletten-Extraktion.
 *
 * @param colors Hex-Farbcodes (z.B. {@code "#abc123"}) in dominanter Reihenfolge — niemals leer
 */
public record PaletteResult(List<String> colors) {

  public PaletteResult {
    Objects.requireNonNull(colors, "colors must not be null");
    if (colors.isEmpty()) {
      throw new IllegalArgumentException("colors must not be empty");
    }
    colors = List.copyOf(colors);
  }
}
