package org.mwolff.api.timeseries.web.dto;

import java.time.Instant;

import org.mwolff.api.timeseries.application.GetTimeSeriesUseCase.TimeSeriesDetail;
import org.mwolff.api.timeseries.application.ListTimeSeriesUseCase.TimeSeriesWithCount;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;

/**
 * Wire-Format einer Zeitreihe. Wird sowohl von {@code GET /api/timeseries} als auch von {@code GET
 * /api/timeseries/{id}} verwendet — die Issue-Vorlage nennt es Summary bzw. Detail, in der V1 sind
 * beide Felder-gleich. Bei Bedarf kann die Detail-Antwort spaeter erweitert werden.
 */
public record TimeSeriesSummaryResponse(
    long id,
    String name,
    String description,
    String unit,
    TimeSeriesDataType dataType,
    long entryCount,
    Instant createdAt,
    Instant updatedAt) {

  public static TimeSeriesSummaryResponse from(TimeSeries timeSeries, long entryCount) {
    return new TimeSeriesSummaryResponse(
        timeSeries.id(),
        timeSeries.name(),
        timeSeries.description(),
        timeSeries.unit(),
        timeSeries.dataType(),
        entryCount,
        timeSeries.createdAt(),
        timeSeries.updatedAt());
  }

  public static TimeSeriesSummaryResponse from(TimeSeriesWithCount entry) {
    return from(entry.timeSeries(), entry.entryCount());
  }

  public static TimeSeriesSummaryResponse from(TimeSeriesDetail detail) {
    return from(detail.timeSeries(), detail.entryCount());
  }
}
