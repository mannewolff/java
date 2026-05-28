package org.mwolff.api.ingest.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.mwolff.api.ingest.domain.IngestToken;

/** Anlegen eines neuen Ingest-Tokens. */
public record CreateIngestTokenRequest(
    @NotBlank @Size(max = IngestToken.MAX_NAME_LENGTH) String name) {}
