package org.mwolff.api.kanban.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemType;

/**
 * Body für Anlage eines neuen Items. {@code body} kann leer sein, fehlt {@code column} → Default
 * BACKLOG im Use-Case. {@code type} fehlt → {@link KanbanItemType#ITEM}. {@code parentId} ordnet
 * ein Item einem Epic zu (Validierung im Use-Case); für Epics muss es leer bleiben (#322).
 */
public record CreateKanbanItemRequest(
    @NotBlank @Size(max = KanbanItem.MAX_TITLE_LENGTH) String title,
    @Size(max = KanbanItem.MAX_BODY_LENGTH) String body,
    KanbanColumn column,
    KanbanItemType type,
    Long parentId,
    @Size(max = KanbanItem.MAX_SHORTCODE_LENGTH) String shortcode) {

  /** Liefert den Body oder den Leerstring, falls null/missing — Domain erwartet non-null. */
  public String bodyOrEmpty() {
    return body == null ? "" : body;
  }

  /** Liefert den Typ oder {@link KanbanItemType#ITEM}, falls null/missing. */
  public KanbanItemType typeOrDefault() {
    return type == null ? KanbanItemType.ITEM : type;
  }
}
