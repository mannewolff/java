package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanSettings;
import org.mwolff.api.kanban.domain.KanbanSettingsPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Aktualisiert die Settings eines Users (Upsert). */
@Component
public class UpdateSettingsUseCase {

  private final KanbanSettingsPort settings;

  public UpdateSettingsUseCase(KanbanSettingsPort settings) {
    this.settings = settings;
  }

  @Transactional
  public KanbanSettings execute(String userSub, int doneRetentionDays) {
    return settings.save(new KanbanSettings(userSub, doneRetentionDays));
  }
}
