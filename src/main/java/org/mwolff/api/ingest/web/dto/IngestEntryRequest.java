package org.mwolff.api.ingest.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Body des oeffentlichen Ingest-Endpoints {@code POST /api/ingest}. */
public record IngestEntryRequest(
    @NotNull @Positive Long timeSeriesId, @NotNull Instant timestamp, @NotNull BigDecimal value) {}
