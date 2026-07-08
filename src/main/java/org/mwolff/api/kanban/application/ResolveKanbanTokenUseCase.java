package org.mwolff.api.kanban.application;

import java.time.Clock;
import java.time.Instant;

import org.mwolff.api.common.token.TokenCryptoPort;
import org.mwolff.api.kanban.domain.InvalidKanbanTokenException;
import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.mwolff.api.kanban.domain.KanbanAccessTokenPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loest einen Plaintext-Token gegen die DB auf. Wird vom {@code KanbanTokenAuthFilter} aufgerufen.
 * Bei Treffer wird {@code lastUsedAt} aktualisiert. Liefert bei gueltigem, nicht widerrufenem Token
 * den vollen Token (inkl. {@code userSub} + {@code displayName}); sonst {@link
 * InvalidKanbanTokenException}.
 */
@Component
public class ResolveKanbanTokenUseCase {

  private final KanbanAccessTokenPort tokens;
  private final TokenCryptoPort crypto;
  private final Clock clock;

  public ResolveKanbanTokenUseCase(
      KanbanAccessTokenPort tokens, TokenCryptoPort crypto, Clock clock) {
    this.tokens = tokens;
    this.crypto = crypto;
    this.clock = clock;
  }

  @Transactional
  public KanbanAccessToken execute(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      throw new InvalidKanbanTokenException("Token must not be blank");
    }
    final String hash = crypto.hash(plaintext);
    final KanbanAccessToken token =
        tokens
            .findActiveByHash(hash)
            .orElseThrow(() -> new InvalidKanbanTokenException("No matching active token"));
    return tokens.save(token.withLastUsedAt(Instant.now(clock)));
  }
}
