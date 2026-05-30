package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Archiviert ein Item (Soft-Delete). Foreign-Item oder nicht gefunden → 404. */
@Component
public class ArchiveItemUseCase {

  private final KanbanItemPort items;

  public ArchiveItemUseCase(KanbanItemPort items) {
    this.items = items;
  }

  @Transactional
  public void execute(String userSub, long itemId) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    items.archiveById(itemId);
  }
}
