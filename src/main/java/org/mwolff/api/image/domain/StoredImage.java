package org.mwolff.api.image.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Ein gespeichertes Bild (#181). Die Binärdaten werden defensiv kopiert, damit das Record
 * unveränderlich bleibt. {@code id} und {@code createdAt} sind erst nach dem Persistieren gesetzt.
 */
public record StoredImage(
    Long id, String contentType, long sizeBytes, byte[] data, Instant createdAt) {

  public StoredImage {
    Objects.requireNonNull(contentType, "contentType must not be null");
    Objects.requireNonNull(data, "data must not be null");
    if (data.length == 0) {
      throw new IllegalArgumentException("data must not be empty");
    }
    data = data.clone();
  }

  /** Fabrik für ein noch nicht persistiertes Bild (ohne id/createdAt). */
  public static StoredImage of(final String contentType, final byte[] data) {
    return new StoredImage(null, contentType, data.length, data, null);
  }

  @Override
  public byte[] data() {
    return data.clone();
  }
}
