package org.mwolff.api.image.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Ein gespeichertes Bild (#181). Die Binärdaten werden defensiv kopiert, damit das Record
 * unveränderlich bleibt. {@code id} und {@code createdAt} sind erst nach dem Persistieren gesetzt.
 * {@code userSub} ist der OIDC-{@code sub} des Eigentümers — die Owner-Isolation des Image-Stores
 * (#230).
 */
public record StoredImage(
    Long id,
    String userSub,
    String contentType,
    long sizeBytes,
    byte[] data,
    Instant createdAt,
    String hash) {

  public StoredImage {
    Objects.requireNonNull(userSub, "userSub must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
    Objects.requireNonNull(data, "data must not be null");
    if (data.length == 0) {
      throw new IllegalArgumentException("data must not be empty");
    }
    data = data.clone();
  }

  /** Fabrik für ein noch nicht persistiertes Bild (ohne id/createdAt) inkl. SHA-256-Hash (#199). */
  public static StoredImage of(
      final String userSub, final String contentType, final byte[] data, final String hash) {
    return new StoredImage(null, userSub, contentType, data.length, data, null, hash);
  }

  /** Fabrik ohne Hash (z. B. Tests / Bestandscode); der Hash bleibt {@code null}. */
  public static StoredImage of(final String userSub, final String contentType, final byte[] data) {
    return of(userSub, contentType, data, null);
  }

  @Override
  public byte[] data() {
    return data.clone();
  }
}
