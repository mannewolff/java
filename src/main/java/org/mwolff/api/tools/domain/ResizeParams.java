package org.mwolff.api.tools.domain;

import java.util.Objects;

/** Eingabeparameter für den Resize-Use-Case. */
public record ResizeParams(int width, int height, String outputFormat, int quality) {

  public ResizeParams {
    if (width < 1) {
      throw new IllegalArgumentException("width must be >= 1");
    }
    if (height < 1) {
      throw new IllegalArgumentException("height must be >= 1");
    }
    Objects.requireNonNull(outputFormat, "outputFormat must not be null");
    if (outputFormat.isBlank()) {
      throw new IllegalArgumentException("outputFormat must not be blank");
    }
    if (quality < 1 || quality > 100) {
      throw new IllegalArgumentException("quality must be between 1 and 100");
    }
  }
}
