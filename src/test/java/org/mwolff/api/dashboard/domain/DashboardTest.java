package org.mwolff.api.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DashboardTest {

  @Test
  void newInstanceShouldHaveNullIdAndTimestamps() {
    final Dashboard d = Dashboard.newInstance("sub-1", "Main", true);
    assertThat(d.id()).isNull();
    assertThat(d.userSub()).isEqualTo("sub-1");
    assertThat(d.name()).isEqualTo("Main");
    assertThat(d.isDefault()).isTrue();
    assertThat(d.createdAt()).isNull();
    assertThat(d.updatedAt()).isNull();
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(() -> Dashboard.newInstance("  ", "Main", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userSub");
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(() -> Dashboard.newInstance(null, "Main", false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(() -> Dashboard.newInstance("sub-1", "  ", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void shouldRejectNullName() {
    assertThatThrownBy(() -> Dashboard.newInstance("sub-1", null, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectNameLongerThan100Chars() {
    final String tooLong = "x".repeat(101);
    assertThatThrownBy(() -> Dashboard.newInstance("sub-1", tooLong, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("100");
  }

  @Test
  void shouldAcceptNameWithExactly100Chars() {
    final String hundred = "x".repeat(100);
    final Dashboard d = Dashboard.newInstance("sub-1", hundred, false);
    assertThat(d.name()).hasSize(100);
  }

  @Test
  void withDefaultShouldReturnNewInstanceWithSwappedFlag() {
    final Dashboard original = Dashboard.newInstance("sub-1", "Main", false);
    final Dashboard updated = original.withDefault(true);
    assertThat(updated.isDefault()).isTrue();
    assertThat(original.isDefault()).isFalse();
    assertThat(updated.userSub()).isEqualTo(original.userSub());
    assertThat(updated.name()).isEqualTo(original.name());
  }
}
