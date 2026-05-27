package org.mwolff.api.kanban.web.dto;

import org.mwolff.api.kanban.domain.KanbanSettings;

/** Response-DTO der User-Settings. */
public record KanbanSettingsResponse(int doneRetentionDays) {

  public static KanbanSettingsResponse from(KanbanSettings settings) {
    return new KanbanSettingsResponse(settings.doneRetentionDays());
  }
}
