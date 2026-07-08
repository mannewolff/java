package org.mwolff.api.kanban.web.dto;

import java.time.Instant;

import org.mwolff.api.kanban.domain.KanbanAccessToken;

/**
 * Wire-Format eines Kanban-Access-Tokens fuer Listen-Antworten. Kein Hash, kein Plaintext nach
 * aussen.
 */
public record KanbanTokenSummaryResponse(
    long id, String name, Instant createdAt, Instant lastUsedAt, boolean revoked) {

  public static KanbanTokenSummaryResponse from(KanbanAccessToken token) {
    return new KanbanTokenSummaryResponse(
        token.id(), token.name(), token.createdAt(), token.lastUsedAt(), token.revoked());
  }
}
