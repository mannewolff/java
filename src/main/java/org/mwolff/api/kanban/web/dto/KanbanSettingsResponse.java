package org.mwolff.api.kanban.web.dto;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanSettings;

/** Response-DTO der User-Settings. */
public record KanbanSettingsResponse(int doneRetentionDays, List<String> activeFilters) {

  public static KanbanSettingsResponse from(KanbanSettings settings) {
    return new KanbanSettingsResponse(
        settings.doneRetentionDays(), KanbanSettings.orderedFilters(settings.activeFilters()));
  }
}
