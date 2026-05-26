package org.mwolff.api.tools.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UploadedImageTest {

  @Test
  void shouldExposeBytesAndMetadata() {
    final byte[] data = new byte[] {1, 2, 3};
    final UploadedImage image = new UploadedImage(data, "image/png", "x.png");
    assertThat(image.bytes()).containsExactly(1, 2, 3);
    assertThat(image.contentType()).isEqualTo("image/png");
    assertThat(image.originalFilename()).isEqualTo("x.png");
    assertThat(image.size()).isEqualTo(3);
  }

  @Test
  void shouldDefensivelyCopyBytesOnConstruction() {
    final byte[] data = new byte[] {1, 2, 3};
    final UploadedImage image = new UploadedImage(data, "image/png", "x.png");
    data[0] = 99;
    assertThat(image.bytes()).containsExactly(1, 2, 3);
  }

  @Test
  void shouldDefensivelyCopyBytesOnAccess() {
    final UploadedImage image = new UploadedImage(new byte[] {1, 2, 3}, "image/png", "x.png");
    final byte[] copy = image.bytes();
    copy[0] = 99;
    assertThat(image.bytes()).containsExactly(1, 2, 3);
  }

  @Test
  void shouldRejectNullBytes() {
    assertThatThrownBy(() -> new UploadedImage(null, "image/png", "x.png"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("bytes");
  }

  @Test
  void shouldRejectEmptyBytes() {
    assertThatThrownBy(() -> new UploadedImage(new byte[0], "image/png", "x.png"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }

  @Test
  void shouldAllowNullContentTypeAndFilename() {
    final UploadedImage image = new UploadedImage(new byte[] {1}, null, null);
    assertThat(image.contentType()).isNull();
    assertThat(image.originalFilename()).isNull();
  }
}
