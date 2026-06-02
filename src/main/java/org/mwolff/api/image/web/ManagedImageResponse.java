package org.mwolff.api.image.web;

import java.time.Instant;

import org.mwolff.api.image.domain.ManagedImage;

/** Bild-Metadaten inkl. Verwendungszähler für den Image-Manager (#202). */
public record ManagedImageResponse(
    long id, String contentType, long sizeBytes, Instant createdAt, String hash, long usageCount) {

  public static ManagedImageResponse from(final ManagedImage managed) {
    return new ManagedImageResponse(
        managed.image().id(),
        managed.image().contentType(),
        managed.image().sizeBytes(),
        managed.image().createdAt(),
        managed.image().hash(),
        managed.usageCount());
  }
}
