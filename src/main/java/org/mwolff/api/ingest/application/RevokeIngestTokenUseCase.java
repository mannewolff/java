package org.mwolff.api.ingest.application;

import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.IngestTokenNotFoundException;
import org.mwolff.api.ingest.domain.IngestTokenPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Widerruft einen Ingest-Token via Soft-Delete (Flag {@code revoked = true}). Idempotent —
 * mehrfaches Widerrufen ist OK. Owner-Schutz wie bei Dashboard/TimeSeries.
 */
@Component
public class RevokeIngestTokenUseCase {

  private final IngestTokenPort tokens;

  public RevokeIngestTokenUseCase(IngestTokenPort tokens) {
    this.tokens = tokens;
  }

  @Transactional
  public void execute(String userSub, long tokenId) {
    final IngestToken existing =
        tokens
            .findById(tokenId)
            .filter(t -> t.userSub().equals(userSub))
            .orElseThrow(() -> new IngestTokenNotFoundException(tokenId));
    if (!existing.revoked()) {
      tokens.save(existing.withRevoked());
    }
  }
}
