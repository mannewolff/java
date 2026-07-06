package org.mwolff.api.kanban.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.mwolff.api.kanban.domain.KanbanItem;

/**
 * Body für ein Content-Update (Title + Body, optional Epic-Kürzel). {@code shortcode} ist optional
 * ({@code null} = keins) und nur an Epics zulässig — die Regel erzwingt das Domänenmodell (#330).
 */
public record UpdateKanbanItemRequest(
    @NotBlank @Size(max = KanbanItem.MAX_TITLE_LENGTH) String title,
    @NotNull @Size(max = KanbanItem.MAX_BODY_LENGTH) String body,
    @Size(max = KanbanItem.MAX_SHORTCODE_LENGTH) String shortcode) {}
