package org.mwolff.api.tools.domain;

import java.util.Objects;
import java.util.Set;

/** Eingabeparameter für den Resize-Use-Case. */
public record ResizeParams(int width, int height, String outputFormat, int quality) {

  // Fachliche Invariante (#134): nur diese Ausgabeformate sind gueltig — deckungsgleich mit dem
  // Web-Vertrag in ResizeController, damit kein Adapter ein unbekanntes Format in die
  // Application-Schicht traegt.
  private static final Set<String> ALLOWED_FORMATS = Set.of("auto", "png", "jpeg", "webp");

  public ResizeParams {
    if (width < 1) {
      throw new IllegalArgumentException("width must be >= 1");
    }
    if (height < 1) {
      throw new IllegalArgumentException("height must be >= 1");
    }
    Objects.requireNonNull(outputFormat, "outputFormat must not be null");
    if (!ALLOWED_FORMATS.contains(outputFormat)) {
      throw new IllegalArgumentException(
          "outputFormat must be one of " + ALLOWED_FORMATS + " but was: " + outputFormat);
    }
    if (quality < 50 || quality > 95) {
      throw new IllegalArgumentException("quality must be between 50 and 95");
    }
  }
}
