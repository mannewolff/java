package org.mwolff.api.kanban.web.dto;

import java.time.Instant;

import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.domain.KanbanAttachmentMeta;

/** Response-DTO eines Anhangs — Metadaten OHNE die Binärdaten. */
public record KanbanAttachmentResponse(
    long id,
    String filename,
    String contentType,
    long sizeBytes,
    String uploadedBy,
    Instant createdAt) {

  public static KanbanAttachmentResponse from(KanbanAttachmentMeta meta) {
    return new KanbanAttachmentResponse(
        meta.id(),
        meta.filename(),
        meta.contentType(),
        meta.sizeBytes(),
        meta.uploadedBySub(),
        meta.createdAt());
  }

  public static KanbanAttachmentResponse from(KanbanAttachment attachment) {
    return new KanbanAttachmentResponse(
        attachment.id(),
        attachment.filename(),
        attachment.contentType(),
        attachment.sizeBytes(),
        attachment.uploadedBySub(),
        attachment.createdAt());
  }
}
