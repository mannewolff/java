package org.mwolff.api.timeseries.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.timeseries.application.GetTimeSeriesUseCase.TimeSeriesDetail;
import org.mwolff.api.timeseries.application.ListTimeSeriesUseCase.TimeSeriesWithCount;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;

class TimeSeriesUseCasesTest {

  private final TimeSeriesPort timeSeries = mock(TimeSeriesPort.class);
  private final TimeSeriesEntryPort entries = mock(TimeSeriesEntryPort.class);

  private static final String SUB_OWNER = "user-1";
  private static final String SUB_OTHER = "user-2";

  private static TimeSeries ts(long id, String userSub, TimeSeriesDataType type) {
    return new TimeSeries(id, userSub, "Weight", "desc", "kg", type, Instant.EPOCH, Instant.EPOCH);
  }

  // ----- list ---------------------------------------------------------------

  @Test
  void listShouldReturnSeriesWithEntryCount() {
    given(timeSeries.findAllByUser(SUB_OWNER))
        .willReturn(List.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));
    given(timeSeries.countEntries(1L)).willReturn(7L);

    final List<TimeSeriesWithCount> result =
        new ListTimeSeriesUseCase(timeSeries).execute(SUB_OWNER);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).entryCount()).isEqualTo(7L);
  }

  // ----- create -------------------------------------------------------------

  @Test
  void createShouldPersistNewTimeSeries() {
    given(timeSeries.save(any())).willAnswer(inv -> withId((TimeSeries) inv.getArgument(0), 1L));

    final TimeSeries created =
        new CreateTimeSeriesUseCase(timeSeries)
            .execute(SUB_OWNER, "Weight", "desc", "kg", TimeSeriesDataType.DECIMAL);

    assertThat(created.id()).isEqualTo(1L);
    assertThat(created.name()).isEqualTo("Weight");
  }

  // ----- get ----------------------------------------------------------------

  @Test
  void getShouldReturnTimeSeriesAndCountForOwner() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));
    given(timeSeries.countEntries(1L)).willReturn(3L);

    final TimeSeriesDetail result = new GetTimeSeriesUseCase(timeSeries).execute(SUB_OWNER, 1L);

    assertThat(result.timeSeries().id()).isEqualTo(1L);
    assertThat(result.entryCount()).isEqualTo(3L);
  }

  @Test
  void getShouldThrowNotFoundForForeignSeries() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));

    assertThatThrownBy(() -> new GetTimeSeriesUseCase(timeSeries).execute(SUB_OTHER, 1L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void getShouldThrowNotFoundWhenMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> new GetTimeSeriesUseCase(timeSeries).execute(SUB_OWNER, 99L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  // ----- update -------------------------------------------------------------

  @Test
  void updateShouldPersistChangesForOwner() {
    final TimeSeries existing = ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL);
    given(timeSeries.findById(1L)).willReturn(Optional.of(existing));
    given(timeSeries.save(any())).willAnswer(inv -> inv.getArgument(0));

    final TimeSeries result =
        new UpdateTimeSeriesUseCase(timeSeries)
            .execute(SUB_OWNER, 1L, "New", "newDesc", "g", TimeSeriesDataType.INTEGER);

    assertThat(result.name()).isEqualTo("New");
    assertThat(result.description()).isEqualTo("newDesc");
    assertThat(result.unit()).isEqualTo("g");
    assertThat(result.dataType()).isEqualTo(TimeSeriesDataType.INTEGER);
    verify(timeSeries).save(any());
  }

  @Test
  void updateShouldThrowForForeignSeries() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));

    assertThatThrownBy(
            () ->
                new UpdateTimeSeriesUseCase(timeSeries)
                    .execute(SUB_OTHER, 1L, "n", null, "kg", TimeSeriesDataType.DECIMAL))
        .isInstanceOf(TimeSeriesNotFoundException.class);
    verify(timeSeries, never()).save(any());
  }

  @Test
  void updateShouldThrowWhenMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new UpdateTimeSeriesUseCase(timeSeries)
                    .execute(SUB_OWNER, 99L, "n", null, "kg", TimeSeriesDataType.DECIMAL))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  // ----- delete -------------------------------------------------------------

  @Test
  void deleteShouldRemoveForOwner() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));

    new DeleteTimeSeriesUseCase(timeSeries).execute(SUB_OWNER, 1L);

    verify(timeSeries).deleteById(1L);
  }

  @Test
  void deleteShouldThrowForForeignSeries() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));

    assertThatThrownBy(() -> new DeleteTimeSeriesUseCase(timeSeries).execute(SUB_OTHER, 1L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
    verify(timeSeries, never()).deleteById(anyLong());
  }

  @Test
  void deleteShouldThrowWhenMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> new DeleteTimeSeriesUseCase(timeSeries).execute(SUB_OWNER, 99L))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  // ----- addEntry -----------------------------------------------------------

  @Test
  void addEntryShouldPersistForOwner() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));
    given(entries.save(any()))
        .willAnswer(inv -> withEntryId((TimeSeriesEntry) inv.getArgument(0), 99L));

    final TimeSeriesEntry result =
        new AddEntryUseCase(timeSeries, entries)
            .execute(SUB_OWNER, 1L, Instant.EPOCH, new BigDecimal("78.5"));

    assertThat(result.id()).isEqualTo(99L);
    assertThat(result.value()).isEqualByComparingTo("78.5");
  }

  @Test
  void addEntryShouldRejectDecimalForIntegerSeries() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.INTEGER)));

    assertThatThrownBy(
            () ->
                new AddEntryUseCase(timeSeries, entries)
                    .execute(SUB_OWNER, 1L, Instant.EPOCH, new BigDecimal("78.5")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("INTEGER");
    verify(entries, never()).save(any());
  }

  @Test
  void addEntryShouldAcceptIntegerValueForIntegerSeries() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.INTEGER)));
    given(entries.save(any())).willAnswer(inv -> inv.getArgument(0));

    final TimeSeriesEntry result =
        new AddEntryUseCase(timeSeries, entries)
            .execute(SUB_OWNER, 1L, Instant.EPOCH, new BigDecimal("78"));

    assertThat(result.value()).isEqualByComparingTo("78");
  }

  @Test
  void addEntryShouldAcceptTrailingZerosForIntegerSeries() {
    // 78.00 -> stripTrailingZeros -> 78 -> scale 0 -> erlaubt
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.INTEGER)));
    given(entries.save(any())).willAnswer(inv -> inv.getArgument(0));

    final TimeSeriesEntry result =
        new AddEntryUseCase(timeSeries, entries)
            .execute(SUB_OWNER, 1L, Instant.EPOCH, new BigDecimal("78.00"));

    assertThat(result.value()).isEqualByComparingTo("78");
  }

  @Test
  void addEntryShouldThrowForForeignSeries() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));

    assertThatThrownBy(
            () ->
                new AddEntryUseCase(timeSeries, entries)
                    .execute(SUB_OTHER, 1L, Instant.EPOCH, BigDecimal.ONE))
        .isInstanceOf(TimeSeriesNotFoundException.class);
    verify(entries, never()).save(any());
  }

  @Test
  void addEntryShouldThrowWhenMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new AddEntryUseCase(timeSeries, entries)
                    .execute(SUB_OWNER, 99L, Instant.EPOCH, BigDecimal.ONE))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  // ----- listEntries --------------------------------------------------------

  @Test
  void listEntriesShouldReturnEntriesForOwnerWithDefaultLimit() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));
    final TimeSeriesEntry e = entry(1L, 10L);
    given(
            entries.findByTimeSeries(
                1L, Optional.empty(), Optional.empty(), ListEntriesUseCase.DEFAULT_LIMIT))
        .willReturn(List.of(e));

    final List<TimeSeriesEntry> result =
        new ListEntriesUseCase(timeSeries, entries)
            .execute(SUB_OWNER, 1L, Optional.empty(), Optional.empty(), Optional.empty());

    assertThat(result).containsExactly(e);
  }

  @Test
  void listEntriesShouldClampLimitToMax() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));
    given(
            entries.findByTimeSeries(
                1L, Optional.empty(), Optional.empty(), ListEntriesUseCase.MAX_LIMIT))
        .willReturn(List.of());

    new ListEntriesUseCase(timeSeries, entries)
        .execute(
            SUB_OWNER,
            1L,
            Optional.empty(),
            Optional.empty(),
            Optional.of(ListEntriesUseCase.MAX_LIMIT + 5));

    verify(entries)
        .findByTimeSeries(1L, Optional.empty(), Optional.empty(), ListEntriesUseCase.MAX_LIMIT);
  }

  @Test
  void listEntriesShouldClampLimitToOneWhenZero() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));
    given(entries.findByTimeSeries(1L, Optional.empty(), Optional.empty(), 1))
        .willReturn(List.of());

    new ListEntriesUseCase(timeSeries, entries)
        .execute(SUB_OWNER, 1L, Optional.empty(), Optional.empty(), Optional.of(0));

    verify(entries).findByTimeSeries(1L, Optional.empty(), Optional.empty(), 1);
  }

  @Test
  void listEntriesShouldPassThroughFromAndTo() {
    final Instant from = Instant.parse("2026-01-01T00:00:00Z");
    final Instant to = Instant.parse("2026-12-31T00:00:00Z");
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));
    given(entries.findByTimeSeries(1L, Optional.of(from), Optional.of(to), 50))
        .willReturn(List.of());

    new ListEntriesUseCase(timeSeries, entries)
        .execute(SUB_OWNER, 1L, Optional.of(from), Optional.of(to), Optional.of(50));

    verify(entries).findByTimeSeries(1L, Optional.of(from), Optional.of(to), 50);
  }

  @Test
  void listEntriesShouldThrowForForeignSeries() {
    given(timeSeries.findById(1L))
        .willReturn(Optional.of(ts(1L, SUB_OWNER, TimeSeriesDataType.DECIMAL)));

    assertThatThrownBy(
            () ->
                new ListEntriesUseCase(timeSeries, entries)
                    .execute(SUB_OTHER, 1L, Optional.empty(), Optional.empty(), Optional.empty()))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  @Test
  void listEntriesShouldThrowWhenMissing() {
    given(timeSeries.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new ListEntriesUseCase(timeSeries, entries)
                    .execute(SUB_OWNER, 99L, Optional.empty(), Optional.empty(), Optional.empty()))
        .isInstanceOf(TimeSeriesNotFoundException.class);
  }

  // ----- helpers ------------------------------------------------------------

  private static TimeSeries withId(TimeSeries source, long newId) {
    return new TimeSeries(
        newId,
        source.userSub(),
        source.name(),
        source.description(),
        source.unit(),
        source.dataType(),
        Instant.EPOCH,
        Instant.EPOCH);
  }

  private static TimeSeriesEntry entry(Long timeSeriesId, Long id) {
    return new TimeSeriesEntry(id, timeSeriesId, Instant.EPOCH, BigDecimal.ONE);
  }

  private static TimeSeriesEntry withEntryId(TimeSeriesEntry source, long newId) {
    return new TimeSeriesEntry(newId, source.timeSeriesId(), source.timestamp(), source.value());
  }
}
