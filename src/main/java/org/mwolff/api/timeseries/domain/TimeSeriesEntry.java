package org.mwolff.api.timeseries.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Ein einzelner Messpunkt einer {@link TimeSeries}. Wert ist immer {@link BigDecimal} mit max.
 * Scale 6; bei {@link TimeSeriesDataType#INTEGER} muessen die Nachkommastellen 0 sein
 * (Application-Layer prueft das, da das Wissen ueber den Typ in der Parent-TimeSeries liegt).
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param timeSeriesId Referenz auf die Parent-Zeitreihe
 * @param timestamp Zeitpunkt der Messung
 * @param value numerischer Messwert, scale &le; 6
 */
public record TimeSeriesEntry(Long id, Long timeSeriesId, Instant timestamp, BigDecimal value) {

  public static final int MAX_VALUE_SCALE = 6;

  public TimeSeriesEntry {
    Objects.requireNonNull(timestamp, "timestamp must not be null");
    Objects.requireNonNull(value, "value must not be null");
    if (value.scale() > MAX_VALUE_SCALE) {
      throw new IllegalArgumentException(
          "value scale must be at most " + MAX_VALUE_SCALE + " digits");
    }
  }

  /** Erzeugt einen noch nicht persistierten Eintrag. */
  public static TimeSeriesEntry newInstance(
      Long timeSeriesId, Instant timestamp, BigDecimal value) {
    return new TimeSeriesEntry(null, timeSeriesId, timestamp, value);
  }
}
