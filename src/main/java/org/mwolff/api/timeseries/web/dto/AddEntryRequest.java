package org.mwolff.api.timeseries.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/** Neuer Eintrag fuer eine Zeitreihe. */
public record AddEntryRequest(@NotNull Instant timestamp, @NotNull BigDecimal value) {}
