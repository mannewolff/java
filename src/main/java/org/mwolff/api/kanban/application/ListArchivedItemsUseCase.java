package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;

/** Liefert alle archivierten Items eines Users. */
@Component
public class ListArchivedItemsUseCase {

  private final KanbanItemPort items;

  public ListArchivedItemsUseCase(KanbanItemPort items) {
    this.items = items;
  }

  public List<KanbanItem> execute(String userSub) {
    return items.findAllByUserIncludingArchived(userSub).stream()
        .filter(KanbanItem::archived)
        .toList();
  }
}
