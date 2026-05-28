package org.mwolff.api.ingest.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IngestTokenExceptionsTest {

  @Test
  void invalidTokenExceptionShouldKeepMessage() {
    final InvalidIngestTokenException ex = new InvalidIngestTokenException("nope");
    assertThat(ex.getMessage()).isEqualTo("nope");
  }

  @Test
  void notFoundExceptionShouldIncludeId() {
    final IngestTokenNotFoundException ex = new IngestTokenNotFoundException(42L);
    assertThat(ex.getMessage()).contains("42");
  }
}
