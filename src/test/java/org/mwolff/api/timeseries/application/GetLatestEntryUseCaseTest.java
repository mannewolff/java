package org.mwolff.api.timeseries.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;

class GetLatestEntryUseCaseTest {

  private final TimeSeriesPort timeSeries = mock(TimeSeriesPort.class);
  private final TimeSeriesEntryPort entries = mock(TimeSeriesEntryPort.class);

  private static final String SUB = "user-1";

  private static TimeSeries owned(long id) {
    return new TimeSeries(
        id, SUB, "n", null, "kg", TimeSeriesDataType.DECIMAL, Instant.EPOCH, Instant.EPOCH);
  }

  @Test
  void returnsLatestEntryForOwner() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(1L)));
    final TimeSeriesEntry entry =
        new TimeSeriesEntry(99L, 1L, Instant.parse("2026-05-27T12:00:00Z"), new BigDecimal("78.5"));
    given(entries.findByTimeSeries(eq(1L), any(), any(), eq(1))).willReturn(List.of(entry));

    final TimeSeriesEntry result = new GetLatestEntryUseCase(timeSeries, entries).execute(SUB, 1L);

    assertThat(result.id()).isEqualTo(99L);
    assertThat(result.value()).isEqualByComparingTo("78.5");
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
                    "kg",
                    TimeSeriesDataType.DECIMAL,
                    Instant.EPOCH,
                    Instant.EPOCH)));

    assertThatThrownBy(() -> new GetLatestEntryUseCase(timeSeries, entries).execute(SUB, 1L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void throwsForForeignEvenWhenEntriesExist() {
    // Killt den Owner-Filter-Mutanten (#203): nur mit nicht-leeren Entries ist der Filter der
    // EINZIGE Pfad zu NotFound — bei leeren Entries würde der orElseThrow ohnehin greifen.
    given(timeSeries.findById(1L))
        .willReturn(
            Optional.of(
                new TimeSeries(
                    1L,
                    "other-user",
                    "n",
                    null,
                    "kg",
                    TimeSeriesDataType.DECIMAL,
                    Instant.EPOCH,
                    Instant.EPOCH)));
    given(entries.findByTimeSeries(eq(1L), any(), any(), eq(1)))
        .willReturn(List.of(new TimeSeriesEntry(7L, 1L, Instant.EPOCH, new BigDecimal("1"))));

    assertThatThrownBy(() -> new GetLatestEntryUseCase(timeSeries, entries).execute(SUB, 1L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void throwsWhenSeriesMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> new GetLatestEntryUseCase(timeSeries, entries).execute(SUB, 99L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void throwsWhenNoEntries() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(1L)));
    given(entries.findByTimeSeries(eq(1L), any(), any(), eq(1))).willReturn(List.of());

    assertThatThrownBy(() -> new GetLatestEntryUseCase(timeSeries, entries).execute(SUB, 1L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }
}
