package org.mwolff.api.image.infrastructure.persistence;

import java.time.Instant;

/**
 * Geschlossene Spring-Data-Projektion für Bild-Metadaten (#198). Durch die Beschränkung auf diese
 * Properties lädt Hibernate die LONGBLOB-Spalte {@code data} nicht mit.
 */
interface StoredImageMetadataView {

  Long getId();

  String getContentType();

  int getSizeBytes();

  Instant getCreatedAt();
}
