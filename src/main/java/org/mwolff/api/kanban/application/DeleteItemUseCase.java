package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Löscht ein Item und schließt die Lücke in der Quell-Spalte. Foreign-Item → 404. */
@Component
public class DeleteItemUseCase {

  private final KanbanItemPort items;

  public DeleteItemUseCase(KanbanItemPort items) {
    this.items = items;
  }

  @Transactional
  public void execute(String userSub, long itemId) {
    final KanbanItem existing =
        items
            .findById(itemId)
            .filter(i -> i.userSub().equals(userSub))
            .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    final KanbanColumn column = existing.column();
    final int gap = existing.position();
    items.deleteById(itemId);
    final List<KanbanItem> remaining = items.findByUserAndColumn(userSub, column);
    for (final KanbanItem other : remaining) {
      if (other.position() > gap) {
        items.updatePosition(other.id(), other.position() - 1);
      }
    }
  }
}
