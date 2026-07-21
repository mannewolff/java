package org.mwolff.api.ingest.application;

import java.time.Clock;
import java.time.Instant;

import org.mwolff.api.common.token.TokenCryptoPort;
import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.IngestTokenPort;
import org.mwolff.api.ingest.domain.InvalidIngestTokenException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loest einen Plaintext-Token gegen die DB auf. Wird vom {@code IngestTokenAuthFilter} aufgerufen.
 * Bei Treffer wird {@code lastUsedAt} aktualisiert (gleiche Transaktion, kann spaeter asynchron
 * werden, falls Latenz-Sensitiv).
 */
@Component
public class ResolveIngestTokenUseCase {

  private final IngestTokenPort tokens;
  private final TokenCryptoPort crypto;
  private final Clock clock;

  public ResolveIngestTokenUseCase(IngestTokenPort tokens, TokenCryptoPort crypto, Clock clock) {
    this.tokens = tokens;
    this.crypto = crypto;
    this.clock = clock;
  }

  @Transactional
  public IngestToken execute(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      throw new InvalidIngestTokenException("Token must not be blank");
    }
    final String hash = crypto.hash(plaintext);
    final IngestToken token =
        tokens
            .findActiveByHash(hash)
            .orElseThrow(() -> new InvalidIngestTokenException("No matching active token"));
    return tokens.save(token.withLastUsedAt(Instant.now(clock)));
  }
}
