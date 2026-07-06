package org.mwolff.api.kanban.web;

import java.util.List;

import org.mwolff.api.kanban.application.GetEpicsUseCase;
import org.mwolff.api.kanban.web.dto.KanbanEpicResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter für Epics (#322). Liefert die Epics des Users inkl. berechnetem Fortschritt. Wie
 * alle {@code /api/kanban/**}-Endpoints durch {@code hasRole("USER")} geschützt; Owner-Filterung im
 * Use-Case via JWT-{@code sub}.
 */
@RestController
@RequestMapping("/api/kanban/epics")
public class KanbanEpicController {

  private final GetEpicsUseCase getEpicsUseCase;

  public KanbanEpicController(GetEpicsUseCase getEpicsUseCase) {
    this.getEpicsUseCase = getEpicsUseCase;
  }

  @GetMapping
  public List<KanbanEpicResponse> list(JwtAuthenticationToken auth) {
    return getEpicsUseCase.execute(auth.getToken().getSubject()).stream()
        .map(KanbanEpicResponse::from)
        .toList();
  }
}
