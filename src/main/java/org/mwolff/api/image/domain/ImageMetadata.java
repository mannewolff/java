package org.mwolff.api.image.domain;

import java.time.Instant;

/**
 * Metadaten eines gespeicherten Bildes — ohne Binärdaten (#198). Dient den Galerie-Ansichten, die
 * viele Bilder listen, ohne die LONGBLOB-Daten zu laden. {@code hash} ist der SHA-256-Hash und wird
 * erst mit der Duplikat-Erkennung (#199) befüllt; bis dahin {@code null}.
 */
public record ImageMetadata(
    long id, String contentType, long sizeBytes, Instant createdAt, String hash) {}
