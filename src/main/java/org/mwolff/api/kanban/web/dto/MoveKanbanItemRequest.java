package org.mwolff.api.kanban.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.mwolff.api.kanban.domain.KanbanColumn;

/** Body für einen Move zwischen oder innerhalb der Spalten. */
public record MoveKanbanItemRequest(@NotNull KanbanColumn column, @PositiveOrZero int position) {}
