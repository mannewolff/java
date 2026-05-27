package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanSettings;
import org.mwolff.api.kanban.domain.KanbanSettingsPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Liefert die Settings eines Users. Falls noch nicht persistiert: Defaults. */
@Component
public class GetSettingsUseCase {

  private final KanbanSettingsPort settings;

  public GetSettingsUseCase(KanbanSettingsPort settings) {
    this.settings = settings;
  }

  @Transactional(readOnly = true)
  public KanbanSettings execute(String userSub) {
    return settings.findByUser(userSub).orElseGet(() -> KanbanSettings.defaultFor(userSub));
  }
}
