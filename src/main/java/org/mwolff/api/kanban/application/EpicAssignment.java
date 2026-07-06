package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanItemType;

/**
 * Wiederverwendbare Prüfung der Epic-Zuordnung eines Items — beim Anlegen (#322) wie beim
 * nachträglichen Zuordnen (#339). Regeln: Ein {@link KanbanItemType#EPIC} darf keinen Parent
 * bekommen; ein Item mit {@code parentId} muss auf ein existierendes, eigenes Epic verweisen.
 * {@code parentId = null} ist immer erlaubt (keine bzw. entfernte Zuordnung). Verstöße → {@link
 * IllegalArgumentException} (vom {@code KanbanExceptionHandler} auf 400 gemappt).
 */
final class EpicAssignment {

  private EpicAssignment() {}

  static void validateParent(
      KanbanItemPort items, String userSub, KanbanItemType type, Long parentId) {
    if (type == KanbanItemType.EPIC) {
      if (parentId != null) {
        throw new IllegalArgumentException("an EPIC must not be assigned to a parent");
      }
      return;
    }
    if (parentId == null) {
      return;
    }
    final KanbanItem parent =
        items
            .findById(parentId)
            .filter(p -> p.userSub().equals(userSub))
            .orElseThrow(
                () -> new IllegalArgumentException("parent epic " + parentId + " not found"));
    if (parent.type() != KanbanItemType.EPIC) {
      throw new IllegalArgumentException("parent " + parentId + " is not an epic");
    }
  }
}
