package org.mwolff.api.tools.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PythonToolsExceptionTest {

  @Test
  void shouldCarryMessage() {
    final PythonToolsException ex = new PythonToolsException("boom");
    assertThat(ex.getMessage()).isEqualTo("boom");
  }

  @Test
  void shouldCarryCause() {
    final Throwable cause = new IllegalStateException("root");
    final PythonToolsException ex = new PythonToolsException("wrapper", cause);
    assertThat(ex.getMessage()).isEqualTo("wrapper");
    assertThat(ex.getCause()).isSameAs(cause);
  }
}
