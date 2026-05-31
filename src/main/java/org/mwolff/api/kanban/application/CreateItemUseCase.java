package org.mwolff.api.kanban.application;

import java.time.Clock;
import java.time.Instant;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt ein neues Item am Ende der gewählten Spalte an. {@code position} = aktuelle Anzahl Items in
 * dieser Spalte. Default-Spalte ist {@link KanbanColumn#BACKLOG}.
 */
@Component
public class CreateItemUseCase {

  private final KanbanItemPort items;
  private final Clock clock;

  public CreateItemUseCase(KanbanItemPort items, Clock clock) {
    this.items = items;
    this.clock = clock;
  }

  @Transactional
  public KanbanItem execute(String userSub, String title, String body, KanbanColumn column) {
    final KanbanColumn target = column == null ? KanbanColumn.BACKLOG : column;
    final int nextPosition = items.findByUserAndColumn(userSub, target).size();
    return items.save(
        KanbanItem.newInstance(userSub, title, body, target, nextPosition, Instant.now(clock)));
  }
}
