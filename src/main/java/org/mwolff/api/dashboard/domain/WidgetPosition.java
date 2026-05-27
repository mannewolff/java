package org.mwolff.api.dashboard.domain;

/**
 * Position und Größe eines Widgets im Grid-Layout. Werte sind Grid-Einheiten (nicht Pixel).
 *
 * <p>Alle vier Felder sind {@code >= 0} für {@code posX}/{@code posY} bzw. {@code >= 1} für {@code
 * width}/{@code height}. Ein 0×0-Widget existiert nicht.
 */
public record WidgetPosition(int posX, int posY, int width, int height) {

  public WidgetPosition {
    if (posX < 0) {
      throw new IllegalArgumentException("posX must be >= 0");
    }
    if (posY < 0) {
      throw new IllegalArgumentException("posY must be >= 0");
    }
    if (width < 1) {
      throw new IllegalArgumentException("width must be >= 1");
    }
    if (height < 1) {
      throw new IllegalArgumentException("height must be >= 1");
    }
  }
}
