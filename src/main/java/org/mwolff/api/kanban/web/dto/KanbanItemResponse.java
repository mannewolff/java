package org.mwolff.api.kanban.web.dto;

import java.time.Instant;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;

/** Response-DTO eines einzelnen Items. */
public record KanbanItemResponse(
    long id,
    String title,
    String body,
    KanbanColumn column,
    int position,
    Instant createdAt,
    Instant updatedAt,
    Instant movedToDoneAt) {

  public static KanbanItemResponse from(KanbanItem item) {
    return new KanbanItemResponse(
        item.id(),
        item.title(),
        item.body(),
        item.column(),
        item.position(),
        item.createdAt(),
        item.updatedAt(),
        item.movedToDoneAt());
  }
}
