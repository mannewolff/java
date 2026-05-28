package org.mwolff.api.timeseries.application;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert eine Zeitreihe inkl. ihrer Eintrags-Anzahl. Wenn der aufrufende User nicht Eigentuemer
 * ist, wird {@link TimeSeriesNotFoundException} geworfen (kein Existenz-Leak).
 */
@Component
public class GetTimeSeriesUseCase {

  private final TimeSeriesPort timeSeries;

  public GetTimeSeriesUseCase(TimeSeriesPort timeSeries) {
    this.timeSeries = timeSeries;
  }

  /** Ergebnis-Record: Zeitreihe plus Eintrags-Anzahl. */
  public record TimeSeriesDetail(TimeSeries timeSeries, long entryCount) {}

  @Transactional(readOnly = true)
  public TimeSeriesDetail execute(String userSub, long timeSeriesId) {
    final TimeSeries owned = loadOwned(userSub, timeSeriesId);
    return new TimeSeriesDetail(owned, timeSeries.countEntries(owned.id()));
  }

  private TimeSeries loadOwned(String userSub, long timeSeriesId) {
    return timeSeries
        .findById(timeSeriesId)
        .filter(ts -> ts.userSub().equals(userSub))
        .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
  }
}
