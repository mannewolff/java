package org.mwolff.api.kanban.application;

import java.util.Collection;
import java.util.Set;

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
  public KanbanSettings execute(
      String userSub, int doneRetentionDays, Collection<String> activeFilters) {
    // activeFilters == null bedeutet "nicht mitgesendet" -> bestehende Filter erhalten (bzw.
    // Default, falls noch nichts gespeichert ist). So setzt ein reines Retention-Update die in
    // der Listen-Ansicht gesetzten Filter nicht zurück.
    final Set<String> filters =
        activeFilters != null
            ? KanbanSettings.sanitizeFilters(activeFilters)
            : settings
                .findByUser(userSub)
                .map(KanbanSettings::activeFilters)
                .orElse(KanbanSettings.DEFAULT_FILTERS);
    return settings.save(new KanbanSettings(userSub, doneRetentionDays, filters));
  }
}
