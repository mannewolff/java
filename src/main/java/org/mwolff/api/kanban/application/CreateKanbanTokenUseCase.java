package org.mwolff.api.kanban.application;

import org.mwolff.api.common.token.TokenCryptoPort;
import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.mwolff.api.kanban.domain.KanbanAccessTokenPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt einen neuen Kanban-Access-Token an. Returnt den Plaintext-Wert einmalig — danach existiert
 * in der DB nur noch der Hash.
 */
@Component
public class CreateKanbanTokenUseCase {

  private final KanbanAccessTokenPort tokens;
  private final TokenCryptoPort crypto;

  public CreateKanbanTokenUseCase(KanbanAccessTokenPort tokens, TokenCryptoPort crypto) {
    this.tokens = tokens;
    this.crypto = crypto;
  }

  /** Rueckgabe-Record: persistierter Token plus einmalig sichtbarer Plaintext. */
  public record CreatedKanbanToken(KanbanAccessToken token, String plaintext) {}

  @Transactional
  public CreatedKanbanToken execute(String userSub, String displayName, String name) {
    final String plaintext = crypto.generatePlaintext();
    final String hash = crypto.hash(plaintext);
    final KanbanAccessToken saved =
        tokens.save(KanbanAccessToken.newInstance(userSub, displayName, name, hash));
    return new CreatedKanbanToken(saved, plaintext);
  }
}
