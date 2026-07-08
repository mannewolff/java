package org.mwolff.api.kanban.web.dto;

import java.time.Instant;

import org.mwolff.api.kanban.application.CreateKanbanTokenUseCase.CreatedKanbanToken;

/**
 * Antwort beim Anlegen eines Tokens. Enthaelt einmalig den Plaintext — nur in dieser Response,
 * danach existiert in der DB nur der Hash.
 */
public record CreatedKanbanTokenResponse(
    long id, String name, String plaintext, Instant createdAt) {

  public static CreatedKanbanTokenResponse from(CreatedKanbanToken created) {
    return new CreatedKanbanTokenResponse(
        created.token().id(),
        created.token().name(),
        created.plaintext(),
        created.token().createdAt());
  }
}
