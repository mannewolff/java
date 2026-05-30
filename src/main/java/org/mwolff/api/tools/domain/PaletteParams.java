package org.mwolff.api.tools.domain;

/** Eingabeparameter für die Farbpaletten-Extraktion. */
public record PaletteParams(int count) {

  public PaletteParams {
    // Fachliche Invariante (#134): deckungsgleich mit dem Web-Vertrag in PaletteController.
    if (count < 2 || count > 10) {
      throw new IllegalArgumentException("count must be between 2 and 10");
    }
  }
}
