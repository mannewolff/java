package org.mwolff.api.timeseries.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert die Eintraege einer Zeitreihe in absteigender Zeitreihenfolge (neuester zuerst). Owner-
 * Schutz wie alle TimeSeries-Use-Cases.
 */
@Component
public class ListEntriesUseCase {

  /** Standard-Limit, wenn der Aufrufer keins setzt. */
  public static final int DEFAULT_LIMIT = 1000;

  /** Hartes Maximum — laesst keine unbeschraenkte Abfrage zu. */
  public static final int MAX_LIMIT = 10_000;

  private final TimeSeriesPort timeSeries;
  private final TimeSeriesEntryPort entries;

  public ListEntriesUseCase(TimeSeriesPort timeSeries, TimeSeriesEntryPort entries) {
    this.timeSeries = timeSeries;
    this.entries = entries;
  }

  @Transactional(readOnly = true)
  public List<TimeSeriesEntry> execute(
      String userSub,
      long timeSeriesId,
      Optional<Instant> from,
      Optional<Instant> to,
      Optional<Integer> limit) {
    timeSeries
        .findById(timeSeriesId)
        .filter(ts -> ts.userSub().equals(userSub))
        .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
    final int effectiveLimit =
        limit.map(l -> Math.min(Math.max(1, l), MAX_LIMIT)).orElse(DEFAULT_LIMIT);
    return entries.findByTimeSeries(timeSeriesId, from, to, effectiveLimit);
  }
}
