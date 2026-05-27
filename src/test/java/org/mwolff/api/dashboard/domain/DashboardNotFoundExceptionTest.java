package org.mwolff.api.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DashboardNotFoundExceptionTest {

  @Test
  void shouldCarryIdInMessage() {
    final DashboardNotFoundException ex = new DashboardNotFoundException(42L);
    assertThat(ex.getMessage()).contains("42");
  }
}
