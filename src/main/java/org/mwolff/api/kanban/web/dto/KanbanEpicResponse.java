package org.mwolff.api.kanban.web.dto;

import org.mwolff.api.kanban.application.GetEpicsUseCase.EpicWithProgress;
import org.mwolff.api.kanban.domain.KanbanItemType;

/**
 * Response-DTO eines Epics inkl. Fortschritt (#322). {@code progress.done}/{@code progress.total}
 * zählen die (nicht-archivierten) Kind-Items bzw. die davon in DONE.
 */
public record KanbanEpicResponse(
    long id,
    int number,
    String title,
    String body,
    KanbanItemType type,
    String shortcode,
    Progress progress) {

  /** Aggregierter Fortschritt eines Epics. */
  public record Progress(int done, int total) {}

  public static KanbanEpicResponse from(EpicWithProgress ep) {
    return new KanbanEpicResponse(
        ep.epic().id(),
        ep.epic().number(),
        ep.epic().title(),
        ep.epic().body(),
        ep.epic().type(),
        ep.epic().shortcode(),
        new Progress(ep.done(), ep.total()));
  }
}
