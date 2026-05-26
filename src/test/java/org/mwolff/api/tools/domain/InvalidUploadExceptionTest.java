package org.mwolff.api.tools.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvalidUploadExceptionTest {

  @Test
  void shouldExposeCodeAndMessage() {
    final InvalidUploadException ex = new InvalidUploadException("FOO", "bar");
    assertThat(ex.code()).isEqualTo("FOO");
    assertThat(ex.getMessage()).isEqualTo("bar");
  }
}
