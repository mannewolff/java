package org.mwolff.api.timeseries.domain;

import java.util.List;
import java.util.Optional;

/**
 * Persistenz-Port fuer Zeitreihen-Metadaten. Implementiert im Infrastructure-Layer via JPA.
 *
 * <p>{@code findById}/{@code save}/{@code delete} sind owner-agnostisch — der Owner-Check liegt im
 * Application-Layer, identisch zum Dashboard-Pattern.
 */
public interface TimeSeriesPort {

  List<TimeSeries> findAllByUser(String userSub);

  Optional<TimeSeries> findById(long id);

  TimeSeries save(TimeSeries timeSeries);

  void deleteById(long id);

  /** Zaehlt die Eintraege einer Zeitreihe — fuer Summary-Antworten in der Liste. */
  long countEntries(long timeSeriesId);
}
