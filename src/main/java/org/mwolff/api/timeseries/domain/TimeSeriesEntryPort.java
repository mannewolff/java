package org.mwolff.api.timeseries.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistenz-Port fuer Zeitreihen-Eintraege. Implementiert im Infrastructure-Layer via JPA.
 *
 * <p>Alle Methoden arbeiten auf einer einzelnen Zeitreihe (per {@code timeSeriesId}). Owner-Check
 * passiert ueber den {@link TimeSeriesPort} im Application-Layer.
 */
public interface TimeSeriesEntryPort {

  /**
   * Liefert Eintraege einer Zeitreihe in absteigender Zeitreihenfolge (neuester zuerst). Optionale
   * Filter: {@code from} (inklusiv), {@code to} (inklusiv), {@code limit} (max. Anzahl).
   */
  List<TimeSeriesEntry> findByTimeSeries(
      long timeSeriesId, Optional<Instant> from, Optional<Instant> to, int limit);

  TimeSeriesEntry save(TimeSeriesEntry entry);
}
