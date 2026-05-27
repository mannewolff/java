package org.mwolff.api.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WidgetPositionTest {

  @Test
  void shouldAcceptZeroAxisOriginAndOneByOne() {
    final WidgetPosition p = new WidgetPosition(0, 0, 1, 1);
    assertThat(p.posX()).isZero();
    assertThat(p.posY()).isZero();
    assertThat(p.width()).isEqualTo(1);
    assertThat(p.height()).isEqualTo(1);
  }

  @Test
  void shouldRejectNegativePosX() {
    assertThatThrownBy(() -> new WidgetPosition(-1, 0, 1, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("posX");
  }

  @Test
  void shouldRejectNegativePosY() {
    assertThatThrownBy(() -> new WidgetPosition(0, -1, 1, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("posY");
  }

  @Test
  void shouldRejectZeroWidth() {
    assertThatThrownBy(() -> new WidgetPosition(0, 0, 0, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("width");
  }

  @Test
  void shouldRejectZeroHeight() {
    assertThatThrownBy(() -> new WidgetPosition(0, 0, 1, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("height");
  }
}
