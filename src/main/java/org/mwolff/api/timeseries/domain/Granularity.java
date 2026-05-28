package org.mwolff.api.timeseries.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/**
 * Zeit-Granularitaet fuer Aggregations-Buckets. Jede Variante kennt eine Truncate-Funktion, die
 * einen Instant auf den Anfang seines Buckets normalisiert.
 *
 * <p>Truncation ist UTC-basiert. Wer abend-zone-spezifische Auswertungen will, muss das beim
 * Bucket-Start explizit beruecksichtigen (Out-of-Scope V1).
 */
public enum Granularity {
  DAILY {
    @Override
    public Instant bucketStart(Instant ts) {
      return ts.atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }
  },
  WEEKLY {
    @Override
    public Instant bucketStart(Instant ts) {
      final LocalDate date = ts.atOffset(ZoneOffset.UTC).toLocalDate();
      final LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      return monday.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
  },
  MONTHLY {
    @Override
    public Instant bucketStart(Instant ts) {
      final LocalDate date = ts.atOffset(ZoneOffset.UTC).toLocalDate();
      return LocalDate.of(date.getYear(), date.getMonth(), 1)
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant();
    }
  },
  YEARLY {
    @Override
    public Instant bucketStart(Instant ts) {
      final int year = ts.atOffset(ZoneOffset.UTC).getYear();
      return LocalDateTime.of(year, Month.JANUARY, 1, 0, 0).toInstant(ZoneOffset.UTC);
    }
  };

  /** Liefert den Bucket-Anfang (inklusiv) fuer einen Zeitpunkt. */
  public abstract Instant bucketStart(Instant ts);
}
