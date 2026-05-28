package org.mwolff.api.timeseries.application;

import java.util.List;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Liefert alle Zeitreihen eines Users mit Eintrags-Anzahl pro Reihe. */
@Component
public class ListTimeSeriesUseCase {

  private final TimeSeriesPort timeSeries;

  public ListTimeSeriesUseCase(TimeSeriesPort timeSeries) {
    this.timeSeries = timeSeries;
  }

  /** Ergebnis-Record: Zeitreihe plus Eintrags-Anzahl fuer die Liste. */
  public record TimeSeriesWithCount(TimeSeries timeSeries, long entryCount) {}

  @Transactional(readOnly = true)
  public List<TimeSeriesWithCount> execute(String userSub) {
    return timeSeries.findAllByUser(userSub).stream()
        .map(ts -> new TimeSeriesWithCount(ts, timeSeries.countEntries(ts.id())))
        .toList();
  }
}
