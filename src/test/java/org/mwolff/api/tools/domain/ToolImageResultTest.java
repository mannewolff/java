package org.mwolff.api.tools.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ToolImageResultTest {

  @Test
  void shouldExposeBytesAndContentType() {
    final ToolImageResult result = new ToolImageResult(new byte[] {1, 2}, "image/jpeg");
    assertThat(result.bytes()).containsExactly(1, 2);
    assertThat(result.contentType()).isEqualTo("image/jpeg");
  }

  @Test
  void shouldDefensivelyCopyBytes() {
    final byte[] data = new byte[] {1, 2};
    final ToolImageResult result = new ToolImageResult(data, "image/jpeg");
    data[0] = 99;
    final byte[] returned = result.bytes();
    returned[0] = 99;
    assertThat(result.bytes()).containsExactly(1, 2);
  }

  @Test
  void shouldRejectNullBytes() {
    assertThatThrownBy(() -> new ToolImageResult(null, "image/jpeg"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectEmptyBytes() {
    assertThatThrownBy(() -> new ToolImageResult(new byte[0], "image/jpeg"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }

  @Test
  void shouldRejectNullContentType() {
    assertThatThrownBy(() -> new ToolImageResult(new byte[] {1}, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankContentType() {
    assertThatThrownBy(() -> new ToolImageResult(new byte[] {1}, "  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
  }
}
