package org.mwolff.api.tools.domain;

import java.util.Objects;

/**
 * Framework-freie Repräsentation eines hochgeladenen Bildes.
 *
 * <p>Web-Adapter mappen Spring's {@code MultipartFile} auf diesen Record, bevor sie ihn an
 * Application-Use-Cases oder das Domain-Modell weiterreichen. Bytes werden defensiv kopiert, damit
 * der Aufrufer das Array später nicht mehr mutieren kann.
 *
 * @param bytes Roh-Bytes der Datei — niemals {@code null}, niemals leer
 * @param contentType vom Client gemeldete (nicht-vertrauenswürdige) MIME-Type; {@code null} erlaubt
 * @param originalFilename Original-Dateiname vom Client; {@code null} erlaubt
 */
public record UploadedImage(byte[] bytes, String contentType, String originalFilename) {

  public UploadedImage {
    Objects.requireNonNull(bytes, "bytes must not be null");
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes must not be empty");
    }
    bytes = bytes.clone();
  }

  /** Defensive Kopie — verhindert dass Aufrufer das interne Array mutiert. */
  @Override
  public byte[] bytes() {
    return bytes.clone();
  }

  /** Größe in Bytes — kürzer als {@code bytes().length} und ohne Kopierkosten. */
  public long size() {
    return bytes.length;
  }
}
