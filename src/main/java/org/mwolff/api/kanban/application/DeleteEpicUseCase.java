package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.EpicHasChildrenException;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanItemType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Löscht ein Epic physisch — aber nur, wenn kein Item mehr darauf verweist (#330). Owner- und
 * Typ-Prüfung: ein fremdes, unbekanntes oder nicht als Epic geführtes Item → {@link
 * KanbanItemNotFoundException} (404). Existiert mindestens ein zugeordnetes Item (auch ein
 * archiviertes) → {@link EpicHasChildrenException} (409). Epics halten keine Board-Position, daher
 * ist keine Lücken-Reindizierung nötig (anders als beim Force-Delete normaler Items).
 */
@Component
public class DeleteEpicUseCase {

  private final KanbanItemPort items;

  public DeleteEpicUseCase(KanbanItemPort items) {
    this.items = items;
  }

  @Transactional
  public void execute(String userSub, long epicId) {
    // Existenz-, Owner- und Typprüfung; das geladene Item selbst wird danach nicht mehr gebraucht
    // (gelöscht wird per id), daher keine lokale Bindung.
    items
        .findById(epicId)
        .filter(i -> i.userSub().equals(userSub))
        .filter(i -> i.type() == KanbanItemType.EPIC)
        .orElseThrow(() -> new KanbanItemNotFoundException(epicId));
    final long children = items.countChildren(epicId);
    if (children > 0) {
      throw new EpicHasChildrenException(epicId, children);
    }
    items.deleteById(epicId);
  }
}
