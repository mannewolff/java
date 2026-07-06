package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert alle Epics eines Users mit berechnetem Fortschritt (#322). Kinder eines Epics sind die
 * nicht-archivierten Items (Typ ITEM) mit {@code parentId == epic.id}; {@code done} sind davon die
 * in Spalte {@link KanbanColumn#DONE}.
 *
 * <p>Der Fortschritt wird aus {@link KanbanItemPort#findAllByUser(String)} (liefert bereits nur
 * nicht-archivierte ITEMs) in einem einzigen Durchlauf berechnet — kein N+1 pro Epic.
 */
@Component
public class GetEpicsUseCase {

  private final KanbanItemPort items;

  public GetEpicsUseCase(KanbanItemPort items) {
    this.items = items;
  }

  /** Epic mit aggregiertem Fortschritt. */
  public record EpicWithProgress(KanbanItem epic, int done, int total) {}

  @Transactional(readOnly = true)
  public List<EpicWithProgress> execute(String userSub) {
    final List<KanbanItem> children = items.findAllByUser(userSub);
    return items.findEpicsByUser(userSub).stream().map(epic -> toProgress(epic, children)).toList();
  }

  private static EpicWithProgress toProgress(KanbanItem epic, List<KanbanItem> children) {
    int total = 0;
    int done = 0;
    for (final KanbanItem child : children) {
      if (epic.id().equals(child.parentId())) {
        total++;
        if (child.column() == KanbanColumn.DONE) {
          done++;
        }
      }
    }
    return new EpicWithProgress(epic, done, total);
  }
}
