package org.mwolff.api.timeseries.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregations-Ergebnis fuer einen Granularitaets-Bucket einer Zeitreihe. {@code last} ist der
 * letzte (juengste) Wert innerhalb des Buckets.
 */
public record AggregateBucket(
    Instant bucketStart,
    long count,
    BigDecimal min,
    BigDecimal max,
    BigDecimal avg,
    BigDecimal last) {

  public AggregateBucket {
    Objects.requireNonNull(bucketStart, "bucketStart must not be null");
    if (count < 1) {
      throw new IllegalArgumentException("count must be >= 1");
    }
    Objects.requireNonNull(min, "min must not be null");
    Objects.requireNonNull(max, "max must not be null");
    Objects.requireNonNull(avg, "avg must not be null");
    Objects.requireNonNull(last, "last must not be null");
  }
}
