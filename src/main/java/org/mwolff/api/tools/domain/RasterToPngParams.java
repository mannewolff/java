package org.mwolff.api.tools.domain;

/**
 * Eingabeparameter für die Raster → PNG-Konvertierung (JPEG/PNG → PNG). Width und Height sind
 * optional — ohne Angabe bleibt die Originalgröße erhalten. Bei einseitiger Angabe berechnet das
 * Backend die andere Seite proportional.
 */
public record RasterToPngParams(Integer width, Integer height) {

  public RasterToPngParams {
    if (width != null && width < 1) {
      throw new IllegalArgumentException("width must be >= 1");
    }
    if (height != null && height < 1) {
      throw new IllegalArgumentException("height must be >= 1");
    }
  }
}
