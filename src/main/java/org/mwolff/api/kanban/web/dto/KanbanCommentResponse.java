package org.mwolff.api.kanban.web.dto;

import java.time.Instant;

import org.mwolff.api.kanban.domain.KanbanComment;

/** Response-DTO eines einzelnen Kommentars. */
public record KanbanCommentResponse(
    long id, long itemId, String author, String body, Instant createdAt, Instant updatedAt) {

  public static KanbanCommentResponse from(KanbanComment comment) {
    return new KanbanCommentResponse(
        comment.id(),
        comment.itemId(),
        comment.author(),
        comment.body(),
        comment.createdAt(),
        comment.updatedAt());
  }
}
