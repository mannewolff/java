package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Aktualisiert Title und Body eines Items. Foreign-Item → 404. */
@Component
public class UpdateItemContentUseCase {

  private final KanbanItemPort items;

  public UpdateItemContentUseCase(KanbanItemPort items) {
    this.items = items;
  }

  @Transactional
  public KanbanItem execute(String userSub, long itemId, String title, String body) {
    final KanbanItem existing =
        items
            .findById(itemId)
            .filter(i -> i.userSub().equals(userSub))
            .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    return items.save(existing.withContent(title, body));
  }
}
