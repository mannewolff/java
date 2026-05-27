package org.mwolff.api.kanban.web;

import jakarta.validation.Valid;

import org.mwolff.api.kanban.application.GetSettingsUseCase;
import org.mwolff.api.kanban.application.UpdateSettingsUseCase;
import org.mwolff.api.kanban.web.dto.KanbanSettingsResponse;
import org.mwolff.api.kanban.web.dto.UpdateKanbanSettingsRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-Adapter für die per-User Kanban-Settings. */
@RestController
@RequestMapping("/api/kanban/settings")
public class KanbanSettingsController {

  private final GetSettingsUseCase getUseCase;
  private final UpdateSettingsUseCase updateUseCase;

  public KanbanSettingsController(GetSettingsUseCase getUseCase, UpdateSettingsUseCase updateUseCase) {
    this.getUseCase = getUseCase;
    this.updateUseCase = updateUseCase;
  }

  @GetMapping
  public KanbanSettingsResponse get(JwtAuthenticationToken auth) {
    return KanbanSettingsResponse.from(getUseCase.execute(auth.getToken().getSubject()));
  }

  @PutMapping
  public KanbanSettingsResponse update(
      JwtAuthenticationToken auth, @Valid @RequestBody UpdateKanbanSettingsRequest body) {
    return KanbanSettingsResponse.from(
        updateUseCase.execute(auth.getToken().getSubject(), body.doneRetentionDays()));
  }
}
