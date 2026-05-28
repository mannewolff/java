package org.mwolff.api.timeseries.application;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aktualisiert die Metadaten einer Zeitreihe (Name, Beschreibung, Einheit, DataType). Owner-Schutz
 * wie alle TimeSeries-Use-Cases.
 */
@Component
public class UpdateTimeSeriesUseCase {

  private final TimeSeriesPort timeSeries;

  public UpdateTimeSeriesUseCase(TimeSeriesPort timeSeries) {
    this.timeSeries = timeSeries;
  }

  @Transactional
  public TimeSeries execute(
      String userSub,
      long timeSeriesId,
      String name,
      String description,
      String unit,
      TimeSeriesDataType dataType) {
    final TimeSeries existing =
        timeSeries
            .findById(timeSeriesId)
            .filter(ts -> ts.userSub().equals(userSub))
            .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
    return timeSeries.save(existing.withMetadata(name, description, unit, dataType));
  }
}
