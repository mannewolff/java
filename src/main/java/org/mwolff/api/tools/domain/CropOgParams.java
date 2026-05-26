package org.mwolff.api.tools.domain;

/** Eingabeparameter für den Open-Graph-Crop-Use-Case. */
public record CropOgParams(double yOffset, double xOffset, int quality, int width, int height) {

  public CropOgParams {
    if (yOffset < 0.0 || yOffset > 1.0) {
      throw new IllegalArgumentException("yOffset must be between 0.0 and 1.0");
    }
    if (xOffset < 0.0 || xOffset > 1.0) {
      throw new IllegalArgumentException("xOffset must be between 0.0 and 1.0");
    }
    if (quality < 1 || quality > 100) {
      throw new IllegalArgumentException("quality must be between 1 and 100");
    }
    if (width < 1) {
      throw new IllegalArgumentException("width must be >= 1");
    }
    if (height < 1) {
      throw new IllegalArgumentException("height must be >= 1");
    }
  }
}
