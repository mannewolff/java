package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Stellt ein archiviertes Item wieder her (setzt archived=false). Foreign-Item → 404. */
@Component
public class RestoreItemUseCase {

  private final KanbanItemPort items;

  public RestoreItemUseCase(KanbanItemPort items) {
    this.items = items;
  }

  @Transactional
  public KanbanItem execute(String userSub, long itemId) {
    final KanbanItem existing =
        items
            .findById(itemId)
            .filter(i -> i.userSub().equals(userSub))
            .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    // Ans Ende der aktiven Zielspalte einsortieren (#309) statt die alte Position zu erzwingen —
    // die ist längst anderweitig vergeben. Position VOR dem Reaktivieren setzen: solange
    // archived=true ist active_position NULL, das Update kann also nicht kollidieren. Erst
    // restoreById macht die (freie) Endposition aktiv.
    final int endPosition = items.findByUserAndColumn(userSub, existing.column()).size();
    items.updatePosition(itemId, endPosition);
    items.restoreById(itemId);
    return items.findById(itemId).orElse(existing);
  }
}
