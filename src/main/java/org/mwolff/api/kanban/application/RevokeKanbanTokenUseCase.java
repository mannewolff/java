package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.mwolff.api.kanban.domain.KanbanAccessTokenPort;
import org.mwolff.api.kanban.domain.KanbanTokenNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Widerruft einen Kanban-Access-Token via Soft-Delete (Flag {@code revoked = true}). Idempotent —
 * mehrfaches Widerrufen ist OK. Owner-Schutz: fremde/unbekannte Tokens werden als 404 gemeldet.
 */
@Component
public class RevokeKanbanTokenUseCase {

  private final KanbanAccessTokenPort tokens;

  public RevokeKanbanTokenUseCase(KanbanAccessTokenPort tokens) {
    this.tokens = tokens;
  }

  @Transactional
  public void execute(String userSub, long tokenId) {
    final KanbanAccessToken existing =
        tokens
            .findById(tokenId)
            .filter(t -> t.userSub().equals(userSub))
            .orElseThrow(() -> new KanbanTokenNotFoundException(tokenId));
    if (!existing.revoked()) {
      tokens.save(existing.withRevoked());
    }
  }
}
