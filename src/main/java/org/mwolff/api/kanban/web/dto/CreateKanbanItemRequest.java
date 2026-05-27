package org.mwolff.api.kanban.web.dto;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body für Anlage eines neuen Items. {@code body} kann leer sein, fehlt {@code column} → Default
 * BACKLOG im Use-Case.
 */
public record CreateKanbanItemRequest(
    @NotBlank @Size(max = KanbanItem.MAX_TITLE_LENGTH) String title,
    @Size(max = KanbanItem.MAX_BODY_LENGTH) String body,
    KanbanColumn column) {

  /** Liefert den Body oder den Leerstring, falls null/missing — Domain erwartet non-null. */
  public String bodyOrEmpty() {
    return body == null ? "" : body;
  }
}
