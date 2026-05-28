package org.mwolff.api.timeseries.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class GranularityTest {

  // 2026-05-28 (Donnerstag) 14:23:45 UTC
  private static final Instant THURSDAY = Instant.parse("2026-05-28T14:23:45Z");

  @Test
  void dailyTruncatesToMidnight() {
    assertThat(Granularity.DAILY.bucketStart(THURSDAY))
        .isEqualTo(Instant.parse("2026-05-28T00:00:00Z"));
  }

  @Test
  void weeklyTruncatesToMonday() {
    // 2026-05-28 ist Donnerstag, der Montag dieser Woche ist 2026-05-25
    assertThat(Granularity.WEEKLY.bucketStart(THURSDAY))
        .isEqualTo(Instant.parse("2026-05-25T00:00:00Z"));
  }

  @Test
  void weeklyAtMondayKeepsMonday() {
    final Instant monday = Instant.parse("2026-05-25T08:00:00Z");
    assertThat(Granularity.WEEKLY.bucketStart(monday))
        .isEqualTo(Instant.parse("2026-05-25T00:00:00Z"));
  }

  @Test
  void monthlyTruncatesToFirstOfMonth() {
    assertThat(Granularity.MONTHLY.bucketStart(THURSDAY))
        .isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
  }

  @Test
  void yearlyTruncatesToJanuaryFirst() {
    assertThat(Granularity.YEARLY.bucketStart(THURSDAY))
        .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
  }
}
