package org.mwolff.api.kanban.application;

import java.time.Clock;
import java.time.Instant;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanItemType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt ein neues Item am Ende der gewählten Spalte an. {@code position} = aktuelle Anzahl Items in
 * dieser Spalte. Default-Spalte ist {@link KanbanColumn#BACKLOG}.
 *
 * <p>Epics (#322): Ein {@link KanbanItemType#EPIC} nimmt nicht am Spalten-Workflow teil (Position
 * 0, wird ohnehin ignoriert) und darf keinem Epic zugeordnet sein. Ein Item mit {@code parentId}
 * muss auf ein existierendes, eigenes Epic zeigen — sonst 400.
 */
@Component
public class CreateItemUseCase {

  private final KanbanItemPort items;
  private final Clock clock;

  public CreateItemUseCase(KanbanItemPort items, Clock clock) {
    this.items = items;
    this.clock = clock;
  }

  /** Legt ein normales Item ohne Epic-Zuordnung an. */
  @Transactional
  public KanbanItem execute(String userSub, String title, String body, KanbanColumn column) {
    return execute(userSub, title, body, column, KanbanItemType.ITEM, null, null);
  }

  /** Legt ein Item mit Typ/Epic-Zuordnung ohne Kürzel an (Alt-Signatur, #322). */
  @Transactional
  public KanbanItem execute(
      String userSub,
      String title,
      String body,
      KanbanColumn column,
      KanbanItemType type,
      Long parentId) {
    return execute(userSub, title, body, column, type, parentId, null);
  }

  @Transactional
  public KanbanItem execute(
      String userSub,
      String title,
      String body,
      KanbanColumn column,
      KanbanItemType type,
      Long parentId,
      String shortcode) {
    final KanbanItemType itemType = type == null ? KanbanItemType.ITEM : type;
    EpicAssignment.validateParent(items, userSub, itemType, parentId);

    final KanbanColumn target = column == null ? KanbanColumn.BACKLOG : column;
    // Epics halten keine aktive Position (V22) — Position 0 genügt und wird ignoriert.
    final int nextPosition =
        itemType == KanbanItemType.EPIC ? 0 : items.findByUserAndColumn(userSub, target).size();
    // Fortlaufende Anzeige-Nummer pro User (#187): erstes Item = 1, sonst höchste + 1.
    final int nextNumber = items.getMaxNumberForUser(userSub).map(max -> max + 1).orElse(1);
    // Die „Kürzel nur an Epics"-Invariante prüft die Domain (KanbanItem) — ein Kürzel an einem
    // ITEM → IllegalArgumentException → 400 (#329).
    return items.save(
        KanbanItem.newInstance(
                userSub,
                title,
                body,
                target,
                nextPosition,
                Instant.now(clock),
                itemType,
                parentId,
                shortcode)
            .withNumber(nextNumber));
  }
}
