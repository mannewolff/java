package org.mwolff.api.timeseries.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.timeseries.application.BulkAddEntriesUseCase.BulkEntry;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;

class BulkAddEntriesUseCaseTest {

  private final TimeSeriesPort timeSeries = mock(TimeSeriesPort.class);
  private final TimeSeriesEntryPort entries = mock(TimeSeriesEntryPort.class);

  private static final String SUB = "user-1";

  private static TimeSeries owned(TimeSeriesDataType type) {
    return new TimeSeries(1L, SUB, "n", null, "kg", type, Instant.EPOCH, Instant.EPOCH);
  }

  @Test
  void persistsAllEntriesInOneBatch() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(TimeSeriesDataType.DECIMAL)));
    given(entries.saveAll(any()))
        .willAnswer(
            inv -> {
              @SuppressWarnings("unchecked")
              final List<TimeSeriesEntry> in = (List<TimeSeriesEntry>) inv.getArgument(0);
              return in;
            });

    final int inserted =
        new BulkAddEntriesUseCase(timeSeries, entries)
            .execute(
                SUB,
                1L,
                List.of(
                    new BulkEntry(Instant.parse("2026-05-27T08:00:00Z"), new BigDecimal("1.5")),
                    new BulkEntry(Instant.parse("2026-05-27T09:00:00Z"), new BigDecimal("2.5"))));

    assertThat(inserted).isEqualTo(2);
    verify(entries).saveAll(any());
  }

  @Test
  void throwsForForeignSeries() {
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

    assertThatThrownBy(
            () ->
                new BulkAddEntriesUseCase(timeSeries, entries)
                    .execute(SUB, 1L, List.of(new BulkEntry(Instant.EPOCH, BigDecimal.ONE))))
        .isInstanceOf(TimeSeriesNotFoundException.class);
    verify(entries, never()).saveAll(any());
  }

  @Test
  void throwsWhenMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new BulkAddEntriesUseCase(timeSeries, entries)
                    .execute(SUB, 99L, List.of(new BulkEntry(Instant.EPOCH, BigDecimal.ONE))))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void rejectsDecimalForIntegerSeries() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(TimeSeriesDataType.INTEGER)));

    assertThatThrownBy(
            () ->
                new BulkAddEntriesUseCase(timeSeries, entries)
                    .execute(
                        SUB,
                        1L,
                        List.of(
                            new BulkEntry(Instant.EPOCH, new BigDecimal("1")),
                            new BulkEntry(Instant.EPOCH, new BigDecimal("1.5")))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("row 2");
    verify(entries, never()).saveAll(any());
  }

  @Test
  void acceptsIntegerSeriesWithTrailingZeros() {
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(TimeSeriesDataType.INTEGER)));
    given(entries.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

    final int inserted =
        new BulkAddEntriesUseCase(timeSeries, entries)
            .execute(SUB, 1L, List.of(new BulkEntry(Instant.EPOCH, new BigDecimal("78.00"))));

    assertThat(inserted).isEqualTo(1);
  }

  @Test
  void rejectsTooManyRows() {
    final List<BulkEntry> tooMany = new java.util.ArrayList<>();
    for (int i = 0; i <= BulkAddEntriesUseCase.MAX_ROWS; i++) {
      tooMany.add(new BulkEntry(Instant.EPOCH.plusSeconds(i), BigDecimal.ONE));
    }

    assertThatThrownBy(
            () -> new BulkAddEntriesUseCase(timeSeries, entries).execute(SUB, 1L, tooMany))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("too many rows");
    verify(entries, never()).saveAll(any());
  }

  @Test
  void acceptsExactlyMaxRows() {
    // Grenzwert-Mutant (#203): genau MAX_ROWS muss noch durchlaufen ( > MAX_ROWS, nicht >= ).
    given(timeSeries.findById(1L)).willReturn(Optional.of(owned(TimeSeriesDataType.DECIMAL)));
    given(entries.saveAll(any())).willAnswer(inv -> inv.getArgument(0));
    final List<BulkEntry> exactly = new java.util.ArrayList<>();
    for (int i = 0; i < BulkAddEntriesUseCase.MAX_ROWS; i++) {
      exactly.add(new BulkEntry(Instant.EPOCH.plusSeconds(i), BigDecimal.ONE));
    }

    final int inserted = new BulkAddEntriesUseCase(timeSeries, entries).execute(SUB, 1L, exactly);

    assertThat(inserted).isEqualTo(BulkAddEntriesUseCase.MAX_ROWS);
  }
}
