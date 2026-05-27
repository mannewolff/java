package org.mwolff.api.kanban.application;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert alle Items eines Users gruppiert nach Spalte, pro Spalte nach {@code position}
 * aufsteigend sortiert.
 */
@Component
public class ListItemsUseCase {

  private final KanbanItemPort items;

  public ListItemsUseCase(KanbanItemPort items) {
    this.items = items;
  }

  @Transactional(readOnly = true)
  public Map<KanbanColumn, List<KanbanItem>> execute(String userSub) {
    final Map<KanbanColumn, List<KanbanItem>> grouped = new EnumMap<>(KanbanColumn.class);
    for (final KanbanColumn col : KanbanColumn.values()) {
      grouped.put(col, new java.util.ArrayList<>());
    }
    // Adapter liefert bereits nach (column, position) sortiert — wir gruppieren nur noch.
    for (final KanbanItem item : items.findAllByUser(userSub)) {
      grouped.get(item.column()).add(item);
    }
    final Map<KanbanColumn, List<KanbanItem>> immutable = new HashMap<>();
    for (final KanbanColumn col : KanbanColumn.values()) {
      immutable.put(col, List.copyOf(grouped.get(col)));
    }
    return Map.copyOf(immutable);
  }
}
