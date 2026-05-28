package org.mwolff.api.timeseries.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class TimeSeriesEntryTest {

  @Test
  void newInstanceShouldStartWithoutId() {
    final TimeSeriesEntry e =
        TimeSeriesEntry.newInstance(42L, Instant.EPOCH, new BigDecimal("78.5"));

    assertThat(e.id()).isNull();
    assertThat(e.timeSeriesId()).isEqualTo(42L);
    assertThat(e.timestamp()).isEqualTo(Instant.EPOCH);
    assertThat(e.value()).isEqualByComparingTo("78.5");
  }

  @Test
  void shouldAllowMaxScale() {
    final TimeSeriesEntry e =
        TimeSeriesEntry.newInstance(42L, Instant.EPOCH, new BigDecimal("1.123456"));

    assertThat(e.value().scale()).isEqualTo(TimeSeriesEntry.MAX_VALUE_SCALE);
  }

  @Test
  void shouldRejectScaleAboveMax() {
    assertThatThrownBy(
            () -> TimeSeriesEntry.newInstance(42L, Instant.EPOCH, new BigDecimal("1.1234567")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("scale");
  }

  @Test
  void shouldRejectNullTimestamp() {
    assertThatThrownBy(() -> TimeSeriesEntry.newInstance(42L, null, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectNullValue() {
    assertThatThrownBy(() -> TimeSeriesEntry.newInstance(42L, Instant.EPOCH, null))
        .isInstanceOf(NullPointerException.class);
  }
}
