package org.mwolff.api.tools.domain;

import java.util.Objects;

/**
 * Verarbeitetes Bild als Antwort eines Tool-Use-Cases.
 *
 * @param bytes resultierende Bilddaten
 * @param contentType MIME-Type des Resultats, z.B. {@code image/jpeg} — niemals leer
 */
public record ToolImageResult(byte[] bytes, String contentType) {

  public ToolImageResult {
    Objects.requireNonNull(bytes, "bytes must not be null");
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes must not be empty");
    }
    Objects.requireNonNull(contentType, "contentType must not be null");
    if (contentType.isBlank()) {
      throw new IllegalArgumentException("contentType must not be blank");
    }
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
