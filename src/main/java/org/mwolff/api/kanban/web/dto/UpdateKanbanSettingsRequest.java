package org.mwolff.api.kanban.web.dto;

import org.mwolff.api.kanban.domain.KanbanSettings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Body für Settings-Update. */
public record UpdateKanbanSettingsRequest(
    @Min(KanbanSettings.MIN_RETENTION_DAYS) @Max(KanbanSettings.MAX_RETENTION_DAYS)
        int doneRetentionDays) {}
