package org.mwolff.api.timeseries.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import org.mwolff.api.timeseries.domain.TimeSeriesEntry;

/** Wire-Format eines Zeitreihen-Eintrags. */
public record TimeSeriesEntryResponse(long id, Instant timestamp, BigDecimal value) {

  public static TimeSeriesEntryResponse from(TimeSeriesEntry entry) {
    return new TimeSeriesEntryResponse(entry.id(), entry.timestamp(), entry.value());
  }
}
