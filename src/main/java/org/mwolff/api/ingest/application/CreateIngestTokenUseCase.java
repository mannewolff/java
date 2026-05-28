package org.mwolff.api.ingest.application;

import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.IngestTokenPort;
import org.mwolff.api.ingest.domain.TokenCryptoPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt einen neuen Ingest-Token an. Returnt den Plaintext-Wert einmalig — danach existiert in der
 * DB nur noch der Hash.
 */
@Component
public class CreateIngestTokenUseCase {

  private final IngestTokenPort tokens;
  private final TokenCryptoPort crypto;

  public CreateIngestTokenUseCase(IngestTokenPort tokens, TokenCryptoPort crypto) {
    this.tokens = tokens;
    this.crypto = crypto;
  }

  /** Rueckgabe-Record: persistierter Token plus einmalig sichtbarer Plaintext. */
  public record CreatedIngestToken(IngestToken token, String plaintext) {}

  @Transactional
  public CreatedIngestToken execute(String userSub, String name) {
    final String plaintext = crypto.generatePlaintext();
    final String hash = crypto.hash(plaintext);
    final IngestToken saved = tokens.save(IngestToken.newInstance(userSub, name, hash));
    return new CreatedIngestToken(saved, plaintext);
  }
}
