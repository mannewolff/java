package org.mwolff.api.timeseries.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TimeSeriesNotFoundExceptionTest {

  @Test
  void messageShouldIncludeId() {
    final TimeSeriesNotFoundException ex = new TimeSeriesNotFoundException(42L);

    assertThat(ex.getMessage()).contains("42");
  }
}
