package org.mwolff.api.ingest.web.dto;

import java.time.Instant;

import org.mwolff.api.ingest.application.CreateIngestTokenUseCase.CreatedIngestToken;

/**
 * Antwort beim Anlegen eines Tokens. Enthaelt einmalig den Plaintext — nur in dieser Response,
 * danach existiert in der DB nur der Hash.
 */
public record CreatedIngestTokenResponse(
    long id, String name, String plaintext, Instant createdAt) {

  public static CreatedIngestTokenResponse from(CreatedIngestToken created) {
    return new CreatedIngestTokenResponse(
        created.token().id(),
        created.token().name(),
        created.plaintext(),
        created.token().createdAt());
  }
}
