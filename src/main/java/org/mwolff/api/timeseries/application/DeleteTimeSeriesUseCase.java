package org.mwolff.api.timeseries.application;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loescht eine Zeitreihe — Eintraege gehen per FK-CASCADE auf DB-Ebene mit weg. Owner-Schutz wie
 * alle TimeSeries-Use-Cases.
 */
@Component
public class DeleteTimeSeriesUseCase {

  private final TimeSeriesPort timeSeries;

  public DeleteTimeSeriesUseCase(TimeSeriesPort timeSeries) {
    this.timeSeries = timeSeries;
  }

  @Transactional
  public void execute(String userSub, long timeSeriesId) {
    final TimeSeries owned =
        timeSeries
            .findById(timeSeriesId)
            .filter(ts -> ts.userSub().equals(userSub))
            .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
    timeSeries.deleteById(owned.id());
  }
}
