package org.mwolff.api.kanban.domain;

import java.time.Instant;

/**
 * Metadaten eines {@link KanbanAttachment} ohne die Binärdaten — für Listen-Ansichten, damit die
 * (potenziell großen) Blobs nicht geladen werden (#349).
 *
 * @param id Anhang-ID
 * @param itemId ID des Kanban-Eintrags
 * @param filename Original-Dateiname
 * @param contentType MIME-Typ
 * @param sizeBytes Größe in Bytes
 * @param uploadedBySub Keycloak-{@code sub} des Uploaders
 * @param createdAt Erstanlage
 */
public record KanbanAttachmentMeta(
    long id,
    long itemId,
    String filename,
    String contentType,
    int sizeBytes,
    String uploadedBySub,
    Instant createdAt) {}
