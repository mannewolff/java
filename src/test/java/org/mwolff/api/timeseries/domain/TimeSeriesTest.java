package org.mwolff.api.timeseries.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TimeSeriesTest {

  private static final String USER = "user-1";

  @Test
  void newInstanceShouldStartWithoutIdAndTimestamps() {
    final TimeSeries ts =
        TimeSeries.newInstance(USER, "Weight", "Body weight", "kg", TimeSeriesDataType.DECIMAL);

    assertThat(ts.id()).isNull();
    assertThat(ts.createdAt()).isNull();
    assertThat(ts.updatedAt()).isNull();
    assertThat(ts.name()).isEqualTo("Weight");
    assertThat(ts.description()).isEqualTo("Body weight");
    assertThat(ts.unit()).isEqualTo("kg");
    assertThat(ts.dataType()).isEqualTo(TimeSeriesDataType.DECIMAL);
  }

  @Test
  void shouldAllowNullDescription() {
    final TimeSeries ts =
        TimeSeries.newInstance(USER, "Weight", null, "kg", TimeSeriesDataType.DECIMAL);

    assertThat(ts.description()).isNull();
  }

  @Test
  void withMetadataShouldReturnUpdatedCopyKeepingIdentity() {
    final TimeSeries original =
        new TimeSeries(
            42L,
            USER,
            "Old",
            "Old desc",
            "kg",
            TimeSeriesDataType.DECIMAL,
            Instant.EPOCH,
            Instant.EPOCH);

    final TimeSeries updated =
        original.withMetadata("New", "New desc", "g", TimeSeriesDataType.INTEGER);

    assertThat(updated.id()).isEqualTo(42L);
    assertThat(updated.userSub()).isEqualTo(USER);
    assertThat(updated.name()).isEqualTo("New");
    assertThat(updated.description()).isEqualTo("New desc");
    assertThat(updated.unit()).isEqualTo("g");
    assertThat(updated.dataType()).isEqualTo(TimeSeriesDataType.INTEGER);
    assertThat(updated.createdAt()).isEqualTo(Instant.EPOCH);
    assertThat(updated.updatedAt()).isEqualTo(Instant.EPOCH);
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(
            () ->
                new TimeSeries(null, null, "n", null, "kg", TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(
            () ->
                new TimeSeries(null, " ", "n", null, "kg", TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullName() {
    assertThatThrownBy(
            () ->
                new TimeSeries(
                    null, USER, null, null, "kg", TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new TimeSeries(null, USER, " ", null, "kg", TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNameLongerThanMax() {
    final String tooLong = "x".repeat(TimeSeries.MAX_NAME_LENGTH + 1);
    assertThatThrownBy(
            () ->
                new TimeSeries(
                    null, USER, tooLong, null, "kg", TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectDescriptionLongerThanMax() {
    final String tooLong = "x".repeat(TimeSeries.MAX_DESCRIPTION_LENGTH + 1);
    assertThatThrownBy(
            () ->
                new TimeSeries(
                    null, USER, "n", tooLong, "kg", TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullUnit() {
    assertThatThrownBy(
            () ->
                new TimeSeries(null, USER, "n", null, null, TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankUnit() {
    assertThatThrownBy(
            () ->
                new TimeSeries(null, USER, "n", null, " ", TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectUnitLongerThanMax() {
    final String tooLong = "x".repeat(TimeSeries.MAX_UNIT_LENGTH + 1);
    assertThatThrownBy(
            () ->
                new TimeSeries(
                    null, USER, "n", null, tooLong, TimeSeriesDataType.DECIMAL, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullDataType() {
    assertThatThrownBy(() -> new TimeSeries(null, USER, "n", null, "kg", null, null, null))
        .isInstanceOf(NullPointerException.class);
  }
}
