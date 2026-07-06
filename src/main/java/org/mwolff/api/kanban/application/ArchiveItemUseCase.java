package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanItem;
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
    final KanbanItem existing =
        items
            .findById(itemId)
            .filter(i -> i.userSub().equals(userSub))
            .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    items.archiveById(itemId);
    // Lücke in der aktiven Spalte schließen (#309): das archivierte Item ist jetzt aus dem
    // aktiven Positions-Namespace (active_position = NULL), die dahinterliegenden Items rücken
    // um eins auf. Aufsteigend über die freigewordene Position — kollisionsfrei mit dem
    // Unique-Constraint uk_kanban_active_position.
    for (final KanbanItem item : items.findByUserAndColumn(userSub, existing.column())) {
      if (item.position() > existing.position()) {
        items.updatePosition(item.id(), item.position() - 1);
      }
    }
  }
}
