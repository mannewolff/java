package org.mwolff.api.image.web;

import java.time.Instant;

import org.mwolff.api.image.domain.ImageMetadata;

/**
 * Metadaten eines Bildes für die Galerie (#198). {@code hash} ist bis zur Duplikat-Erkennung (#199)
 * {@code null}.
 */
public record ImageMetadataResponse(
    long id, String contentType, long sizeBytes, Instant createdAt, String hash) {

  public static ImageMetadataResponse from(final ImageMetadata metadata) {
    return new ImageMetadataResponse(
        metadata.id(),
        metadata.contentType(),
        metadata.sizeBytes(),
        metadata.createdAt(),
        metadata.hash());
  }
}
