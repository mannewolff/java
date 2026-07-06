package org.mwolff.api.timeseries.application;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesDataTypeConflictException;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
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
  private final TimeSeriesEntryPort entries;

  public UpdateTimeSeriesUseCase(TimeSeriesPort timeSeries, TimeSeriesEntryPort entries) {
    this.timeSeries = timeSeries;
    this.entries = entries;
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
    // Wechsel auf INTEGER nur, wenn keine dezimalen Bestandswerte vorliegen — sonst würde die
    // INTEGER-Invariante rückwirkend verletzt (bestehende Dezimalwerte blieben, neue Werte würden
    // abgelehnt). Inkompatibler Wechsel → 409 statt stiller Invariantenverletzung.
    if (dataType == TimeSeriesDataType.INTEGER
        && existing.dataType() != TimeSeriesDataType.INTEGER
        && entries.hasFractionalValues(timeSeriesId)) {
      throw new TimeSeriesDataTypeConflictException(timeSeriesId);
    }
    return timeSeries.save(existing.withMetadata(name, description, unit, dataType));
  }
}
