package org.mwolff.api.tools.domain;

/** Eingabeparameter für die Farbpaletten-Extraktion. */
public record PaletteParams(int count) {

  public PaletteParams {
    if (count < 1) {
      throw new IllegalArgumentException("count must be >= 1");
    }
  }
}
