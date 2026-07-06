package org.mwolff.api.kanban.web;

import java.util.List;

import jakarta.validation.constraints.Min;

import org.mwolff.api.kanban.application.DeleteEpicUseCase;
import org.mwolff.api.kanban.application.GetEpicsUseCase;
import org.mwolff.api.kanban.web.dto.KanbanEpicResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter für Epics (#322). Liefert die Epics des Users inkl. berechnetem Fortschritt und
 * erlaubt das Löschen eines Epics (#330). Wie alle {@code /api/kanban/**}-Endpoints durch {@code
 * hasRole("USER")} geschützt; Owner-Filterung im Use-Case via JWT-{@code sub}. Das Bearbeiten von
 * Titel/Body/Kürzel läuft über den generischen Pfad {@code PUT /api/kanban/items/{id}}.
 */
@RestController
@RequestMapping("/api/kanban/epics")
public class KanbanEpicController {

  private final GetEpicsUseCase getEpicsUseCase;
  private final DeleteEpicUseCase deleteEpicUseCase;

  public KanbanEpicController(
      GetEpicsUseCase getEpicsUseCase, DeleteEpicUseCase deleteEpicUseCase) {
    this.getEpicsUseCase = getEpicsUseCase;
    this.deleteEpicUseCase = deleteEpicUseCase;
  }

  @GetMapping
  public List<KanbanEpicResponse> list(JwtAuthenticationToken auth) {
    return getEpicsUseCase.execute(auth.getToken().getSubject()).stream()
        .map(KanbanEpicResponse::from)
        .toList();
  }

  /**
   * Löscht ein Epic. 204 bei Erfolg; 409, wenn noch ein Item darauf verweist; 404 bei
   * fremdem/unbekanntem/kein-Epic-{@code id}.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(JwtAuthenticationToken auth, @PathVariable @Min(1) long id) {
    deleteEpicUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }
}
