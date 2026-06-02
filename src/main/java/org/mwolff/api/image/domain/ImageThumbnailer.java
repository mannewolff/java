package org.mwolff.api.image.domain;

/**
 * Domain-Port zum Erzeugen eines verkleinerten Thumbnails aus den Original-Binärdaten (#200). Die
 * konkrete Skalierung (JVM-seitig via ImageIO) liegt in der Infrastruktur. Ausgabeformat ist PNG.
 */
public interface ImageThumbnailer {

  /** Maximale Kantenlänge des Default-Thumbnails. */
  int DEFAULT_MAX_EDGE = 160;

  /** Untere/obere Grenze für die angeforderte Kantenlänge. */
  int MIN_MAX_EDGE = 16;

  int MAX_MAX_EDGE = 512;

  /**
   * Skaliert {@code source} so, dass die längere Kante höchstens {@code maxEdge} px misst
   * (Seitenverhältnis erhalten, kein Upscaling), und liefert PNG-Bytes.
   *
   * @throws InvalidImageUploadException wenn die Daten nicht als Bild dekodiert werden können
   */
  byte[] toThumbnailPng(byte[] source, int maxEdge);
}
