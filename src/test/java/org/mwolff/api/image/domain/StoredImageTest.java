package org.mwolff.api.image.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StoredImageTest {

  @Test
  void ofSetsSizeFromDataAndLeavesIdAndCreatedAtNull() {
    final StoredImage image = StoredImage.of("image/png", new byte[] {1, 2, 3});

    assertThat(image.id()).isNull();
    assertThat(image.createdAt()).isNull();
    assertThat(image.contentType()).isEqualTo("image/png");
    assertThat(image.sizeBytes()).isEqualTo(3);
    assertThat(image.data()).containsExactly(1, 2, 3);
  }

  @Test
  void dataIsDefensivelyCopiedOnConstructionAndAccess() {
    final byte[] original = {1, 2, 3};
    final StoredImage image = StoredImage.of("image/png", original);

    original[0] = 9; // darf das gespeicherte Bild nicht verändern
    assertThat(image.data()).containsExactly(1, 2, 3);

    final byte[] read = image.data();
    read[0] = 9; // verändertes Read-Ergebnis darf nicht zurückwirken
    assertThat(image.data()).containsExactly(1, 2, 3);
  }

  @Test
  void rejectsNullContentType() {
    assertThatNullPointerException()
        .isThrownBy(() -> new StoredImage(null, null, 1, new byte[] {1}, null))
        .withMessageContaining("contentType");
  }

  @Test
  void rejectsNullData() {
    assertThatNullPointerException()
        .isThrownBy(() -> new StoredImage(null, "image/png", 0, null, null))
        .withMessageContaining("data");
  }

  @Test
  void rejectsEmptyData() {
    assertThatThrownBy(() -> new StoredImage(null, "image/png", 0, new byte[0], null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }
}
