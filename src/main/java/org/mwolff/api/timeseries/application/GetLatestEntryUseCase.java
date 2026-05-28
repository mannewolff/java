package org.mwolff.api.timeseries.application;

import java.util.Optional;

import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert den letzten (juengsten) Eintrag einer Zeitreihe. Wirft {@link
 * TimeSeriesNotFoundException} sowohl bei Fremdbesitz als auch bei leerer Zeitreihe — der Endpoint
 * gibt 404 zurueck, beide Faelle sind aus Sicht des Konsumenten "Keine Daten".
 */
@Component
public class GetLatestEntryUseCase {

  private final TimeSeriesPort timeSeries;
  private final TimeSeriesEntryPort entries;

  public GetLatestEntryUseCase(TimeSeriesPort timeSeries, TimeSeriesEntryPort entries) {
    this.timeSeries = timeSeries;
    this.entries = entries;
  }

  @Transactional(readOnly = true)
  public TimeSeriesEntry execute(String userSub, long timeSeriesId) {
    timeSeries
        .findById(timeSeriesId)
        .filter(ts -> ts.userSub().equals(userSub))
        .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
    return entries.findByTimeSeries(timeSeriesId, Optional.empty(), Optional.empty(), 1).stream()
        .findFirst()
        .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
  }
}
