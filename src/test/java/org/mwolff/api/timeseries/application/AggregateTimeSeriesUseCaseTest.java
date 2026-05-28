package org.mwolff.api.timeseries.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.timeseries.domain.AggregateBucket;
import org.mwolff.api.timeseries.domain.Granularity;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;

class AggregateTimeSeriesUseCaseTest {

  private final TimeSeriesPort timeSeries = mock(TimeSeriesPort.class);
  private final TimeSeriesEntryPort entries = mock(TimeSeriesEntryPort.class);

  private static final String SUB = "user-1";

  private static TimeSeries owned(long id) {
    return new TimeSeries(
        id, SUB, "weight", null, "kg", TimeSeriesDataType.DECIMAL, Instant.EPOCH, Instant.EPOCH);
  }

  private static TimeSeriesEntry entry(long id, String ts, String value) {
    return new TimeSeriesEntry(id, 1L, Instant.parse(ts), new BigDecimal(value));
  }

  @Test
  void aggregatesPerDailyBucket() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(1L)));
    given(entries.findByTimeSeries(eq(1L), any(), any(), anyInt()))
        .willReturn(
            List.of(
                entry(1L, "2026-05-27T08:00:00Z", "10"),
                entry(2L, "2026-05-27T20:00:00Z", "20"),
                entry(3L, "2026-05-28T12:00:00Z", "30")));

    final List<AggregateBucket> result =
        new AggregateTimeSeriesUseCase(timeSeries, entries)
            .execute(SUB, 1L, Granularity.DAILY, Optional.empty(), Optional.empty());

    assertThat(result).hasSize(2);
    final AggregateBucket first = result.get(0);
    assertThat(first.bucketStart()).isEqualTo(Instant.parse("2026-05-27T00:00:00Z"));
    assertThat(first.count()).isEqualTo(2L);
    assertThat(first.min()).isEqualByComparingTo("10");
    assertThat(first.max()).isEqualByComparingTo("20");
    assertThat(first.avg()).isEqualByComparingTo("15");
    assertThat(first.last()).isEqualByComparingTo("20");

    final AggregateBucket second = result.get(1);
    assertThat(second.bucketStart()).isEqualTo(Instant.parse("2026-05-28T00:00:00Z"));
    assertThat(second.count()).isEqualTo(1L);
    assertThat(second.last()).isEqualByComparingTo("30");
  }

  @Test
  void returnsEmptyListWhenNoEntries() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(1L)));
    given(entries.findByTimeSeries(eq(1L), any(), any(), anyInt())).willReturn(List.of());

    final List<AggregateBucket> result =
        new AggregateTimeSeriesUseCase(timeSeries, entries)
            .execute(SUB, 1L, Granularity.WEEKLY, Optional.empty(), Optional.empty());

    assertThat(result).isEmpty();
  }

  @Test
  void throwsForForeignTimeSeries() {
    given(timeSeries.findById(1L))
        .willReturn(
            Optional.of(
                new TimeSeries(
                    1L,
                    "other-user",
                    "n",
                    null,
                    "u",
                    TimeSeriesDataType.DECIMAL,
                    Instant.EPOCH,
                    Instant.EPOCH)));

    assertThatThrownBy(
            () ->
                new AggregateTimeSeriesUseCase(timeSeries, entries)
                    .execute(SUB, 1L, Granularity.DAILY, Optional.empty(), Optional.empty()))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void throwsWhenMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new AggregateTimeSeriesUseCase(timeSeries, entries)
                    .execute(SUB, 99L, Granularity.DAILY, Optional.empty(), Optional.empty()))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void appliesYearlyGranularity() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(1L)));
    given(entries.findByTimeSeries(eq(1L), any(), any(), anyInt()))
        .willReturn(
            List.of(
                entry(1L, "2025-06-15T08:00:00Z", "100"),
                entry(2L, "2026-01-15T08:00:00Z", "200")));

    final List<AggregateBucket> result =
        new AggregateTimeSeriesUseCase(timeSeries, entries)
            .execute(SUB, 1L, Granularity.YEARLY, Optional.empty(), Optional.empty());

    assertThat(result).hasSize(2);
    assertThat(result.get(0).bucketStart()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    assertThat(result.get(1).bucketStart()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void appliesMonthlyGranularity() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(1L)));
    given(entries.findByTimeSeries(eq(1L), any(), any(), anyInt()))
        .willReturn(
            List.of(
                entry(1L, "2026-04-15T08:00:00Z", "1"), entry(2L, "2026-05-15T08:00:00Z", "2")));

    final List<AggregateBucket> result =
        new AggregateTimeSeriesUseCase(timeSeries, entries)
            .execute(SUB, 1L, Granularity.MONTHLY, Optional.empty(), Optional.empty());

    assertThat(result).hasSize(2);
    assertThat(result.get(0).bucketStart()).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
    assertThat(result.get(1).bucketStart()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
  }

  @Test
  void minMaxUpdateOnDescendingValues() {
    // Deckt den value.compareTo(min)<0 == true Branch nach dem ersten Eintrag
    // sowie den value.compareTo(max)>0 == false Branch.
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(1L)));
    given(entries.findByTimeSeries(eq(1L), any(), any(), anyInt()))
        .willReturn(
            List.of(
                entry(1L, "2026-05-27T08:00:00Z", "20"),
                entry(2L, "2026-05-27T09:00:00Z", "10"),
                entry(3L, "2026-05-27T10:00:00Z", "15")));

    final List<AggregateBucket> result =
        new AggregateTimeSeriesUseCase(timeSeries, entries)
            .execute(SUB, 1L, Granularity.DAILY, Optional.empty(), Optional.empty());

    final AggregateBucket bucket = result.get(0);
    assertThat(bucket.min()).isEqualByComparingTo("10");
    assertThat(bucket.max()).isEqualByComparingTo("20");
    assertThat(bucket.last()).isEqualByComparingTo("15");
  }
}
