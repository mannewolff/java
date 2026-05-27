package org.mwolff.api.kanban.domain;

import java.util.Optional;

/** Persistenz-Port für {@link KanbanSettings}. */
public interface KanbanSettingsPort {

  Optional<KanbanSettings> findByUser(String userSub);

  KanbanSettings save(KanbanSettings settings);
}
