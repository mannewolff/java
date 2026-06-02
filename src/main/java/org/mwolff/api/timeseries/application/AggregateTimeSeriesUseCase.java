package org.mwolff.api.timeseries.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.mwolff.api.timeseries.domain.AggregateBucket;
import org.mwolff.api.timeseries.domain.Granularity;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregiert eine Zeitreihe in Granularitaets-Buckets. Ergebnis: pro Bucket {@link AggregateBucket}
 * mit count/min/max/avg/last. In-memory-Aggregation — fuer die Toolbox-Datenvolumen (typischerweise
 * wenige Tausend Entries pro Zeitreihe) ausreichend.
 */
@Component
public class AggregateTimeSeriesUseCase {

  /** Hartes Maximum geladener Entries — schuetzt vor versehentlichen Vollscans. */
  public static final int MAX_ENTRIES = 100_000;

  /** Hartes Maximum fuer die zurueckgegebene Bucket-Anzahl (deckt sich mit dem Frontend). */
  public static final int MAX_LIMIT = 10_000;

  private final TimeSeriesPort timeSeries;
  private final TimeSeriesEntryPort entries;
  private final Clock clock;

  public AggregateTimeSeriesUseCase(
      TimeSeriesPort timeSeries, TimeSeriesEntryPort entries, Clock clock) {
    this.timeSeries = timeSeries;
    this.entries = entries;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AggregateBucket> execute(
      String userSub,
      long timeSeriesId,
      Granularity granularity,
      Optional<Instant> from,
      Optional<Instant> to,
      Optional<Integer> limit) {
    timeSeries
        .findById(timeSeriesId)
        .filter(ts -> ts.userSub().equals(userSub))
        .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
    final Instant now = Instant.now(clock);
    final Instant rangeTo = to.orElse(now);
    final Instant rangeFrom = from.orElseGet(() -> defaultFrom(granularity, rangeTo));

    final List<TimeSeriesEntry> raw =
        entries.findByTimeSeries(
            timeSeriesId, Optional.of(rangeFrom), Optional.of(rangeTo), MAX_ENTRIES);

    final LinkedHashMap<Instant, BucketAccumulator> buckets = new LinkedHashMap<>();
    final List<TimeSeriesEntry> sorted =
        raw.stream().sorted(Comparator.comparing(TimeSeriesEntry::timestamp)).toList();
    for (TimeSeriesEntry entry : sorted) {
      final Instant bucketStart = granularity.bucketStart(entry.timestamp());
      buckets.computeIfAbsent(bucketStart, k -> new BucketAccumulator()).add(entry.value());
    }
    final List<AggregateBucket> result = new ArrayList<>(buckets.size());
    for (var e : buckets.entrySet()) {
      result.add(e.getValue().toBucket(e.getKey()));
    }
    // Limit greift auf die juengsten N Buckets — die Liste ist nach bucketStart aufsteigend.
    if (limit.isPresent()) {
      final int effectiveLimit = Math.min(Math.max(1, limit.get()), MAX_LIMIT);
      if (result.size() > effectiveLimit) {
        return new ArrayList<>(result.subList(result.size() - effectiveLimit, result.size()));
      }
    }
    return result;
  }

  private static Instant defaultFrom(Granularity granularity, Instant rangeTo) {
    return switch (granularity) {
      case DAILY -> rangeTo.minus(Duration.ofDays(30));
      case WEEKLY -> rangeTo.minus(Duration.ofDays(7L * 12));
      case MONTHLY -> rangeTo.atOffset(ZoneOffset.UTC).minus(Period.ofMonths(12)).toInstant();
      case YEARLY -> rangeTo.atOffset(ZoneOffset.UTC).minus(Period.ofYears(5)).toInstant();
    };
  }

  private static final class BucketAccumulator {
    private long count;
    private BigDecimal sum = BigDecimal.ZERO;
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal last;

    void add(BigDecimal value) {
      count++;
      sum = sum.add(value);
      if (min == null || value.compareTo(min) < 0) {
        min = value;
      }
      if (max == null || value.compareTo(max) > 0) {
        max = value;
      }
      // Liste ist nach Timestamp aufsteigend sortiert — der jeweils zuletzt
      // verarbeitete Eintrag im Bucket hat automatisch den juengsten Timestamp.
      last = value;
    }

    AggregateBucket toBucket(Instant bucketStart) {
      final BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
      return new AggregateBucket(bucketStart, count, min, max, avg, last);
    }
  }
}
