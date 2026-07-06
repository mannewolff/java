package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Löscht ein Item physisch und schließt die Lücke in der Quell-Spalte. Foreign-Item → 404. */
@Component
public class ForceDeleteItemUseCase {

  private final KanbanItemPort items;

  public ForceDeleteItemUseCase(KanbanItemPort items) {
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
    // Ein archiviertes Item liegt NICHT im aktiven Positions-Namespace (active_position = NULL,
    // siehe V19/V22): seine `position` überlappt mit aktiven Items derselben Spalte. Ein Reindex
    // anhand dieser Position würde aktive Items übereinanderschieben und den Unique-Constraint
    // uk_kanban_active_position verletzen (→ 409). Beim Löschen eines archivierten Items gibt es
    // keine aktive Lücke zu schließen.
    if (existing.archived()) {
      return;
    }
    final List<KanbanItem> remaining = items.findByUserAndColumn(userSub, column);
    for (final KanbanItem other : remaining) {
      if (other.position() > gap) {
        items.updatePosition(other.id(), other.position() - 1);
      }
    }
  }
}
