package org.mwolff.api.ingest.web.dto;

import java.time.Instant;

import org.mwolff.api.ingest.domain.IngestToken;

/** Wire-Format eines Ingest-Tokens fuer Listen-Antworten. Kein Hash, kein Plaintext nach aussen. */
public record IngestTokenSummaryResponse(
    long id, String name, Instant createdAt, Instant lastUsedAt, boolean revoked) {

  public static IngestTokenSummaryResponse from(IngestToken token) {
    return new IngestTokenSummaryResponse(
        token.id(), token.name(), token.createdAt(), token.lastUsedAt(), token.revoked());
  }
}
