package org.mwolff.api.timeseries.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class AggregateBucketTest {

  @Test
  void buildsBucketWithAllFields() {
    final AggregateBucket bucket =
        new AggregateBucket(
            Instant.EPOCH,
            5L,
            new BigDecimal("1.0"),
            new BigDecimal("9.0"),
            new BigDecimal("5.0"),
            new BigDecimal("7.0"));

    assertThat(bucket.count()).isEqualTo(5L);
    assertThat(bucket.min()).isEqualByComparingTo("1.0");
    assertThat(bucket.max()).isEqualByComparingTo("9.0");
    assertThat(bucket.avg()).isEqualByComparingTo("5.0");
    assertThat(bucket.last()).isEqualByComparingTo("7.0");
  }

  @Test
  void rejectsZeroCount() {
    assertThatThrownBy(
            () ->
                new AggregateBucket(
                    Instant.EPOCH,
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullFields() {
    assertThatThrownBy(
            () ->
                new AggregateBucket(
                    null, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new AggregateBucket(
                    Instant.EPOCH, 1L, null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new AggregateBucket(
                    Instant.EPOCH, 1L, BigDecimal.ONE, null, BigDecimal.ONE, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new AggregateBucket(
                    Instant.EPOCH, 1L, BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new AggregateBucket(
                    Instant.EPOCH, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null))
        .isInstanceOf(NullPointerException.class);
  }
}
