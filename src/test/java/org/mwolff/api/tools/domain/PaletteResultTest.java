package org.mwolff.api.tools.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PaletteResultTest {

  @Test
  void shouldExposeColors() {
    final PaletteResult result = new PaletteResult(List.of("#abcdef", "#123456"));
    assertThat(result.colors()).containsExactly("#abcdef", "#123456");
  }

  @Test
  void shouldDefensivelyCopyColorsList() {
    final List<String> mutable = new ArrayList<>(List.of("#abc123"));
    final PaletteResult result = new PaletteResult(mutable);
    mutable.add("#def456");
    assertThat(result.colors()).containsExactly("#abc123");
  }

  @Test
  void shouldRejectNullColors() {
    assertThatThrownBy(() -> new PaletteResult(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectEmptyColors() {
    assertThatThrownBy(() -> new PaletteResult(List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }
}
