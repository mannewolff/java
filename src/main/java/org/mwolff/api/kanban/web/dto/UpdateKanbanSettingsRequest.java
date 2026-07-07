package org.mwolff.api.kanban.web.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.mwolff.api.kanban.domain.KanbanSettings;

/**
 * Body für Settings-Update.
 *
 * @param activeFilters Aktive Listen-Filter; unbekannte Keys werden serverseitig verworfen, {@code
 *     null} fällt auf die Default-Filter zurück.
 */
public record UpdateKanbanSettingsRequest(
    @Min(KanbanSettings.MIN_RETENTION_DAYS) @Max(KanbanSettings.MAX_RETENTION_DAYS)
        int doneRetentionDays,
    List<String> activeFilters) {}
