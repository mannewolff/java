package org.mwolff.api.kanban.web.dto;

import org.mwolff.api.kanban.domain.KanbanColumn;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body für einen Move zwischen oder innerhalb der Spalten. */
public record MoveKanbanItemRequest(
    @NotNull KanbanColumn column, @PositiveOrZero int position) {}
