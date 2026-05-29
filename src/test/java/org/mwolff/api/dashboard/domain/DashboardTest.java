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

  @Test
  void withNameShouldReturnNewInstanceWithChangedName() {
    final Dashboard original = Dashboard.newInstance("sub-1", "Old", true);
    final Dashboard updated = original.withName("New");
    assertThat(updated.name()).isEqualTo("New");
    assertThat(original.name()).isEqualTo("Old");
    assertThat(updated.userSub()).isEqualTo(original.userSub());
    assertThat(updated.isDefault()).isEqualTo(original.isDefault());
  }

  @Test
  void withNameShouldRejectBlankName() {
    final Dashboard original = Dashboard.newInstance("sub-1", "Old", false);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> original.withName(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void newInstanceShouldHaveNullBackgroundColor() {
    assertThat(Dashboard.newInstance("sub-1", "Main", false).backgroundColor()).isNull();
  }

  @Test
  void shouldNormalizeBlankBackgroundColorToNull() {
    final Dashboard d = Dashboard.newInstance("sub-1", "Main", false).withBackgroundColor("   ");
    assertThat(d.backgroundColor()).isNull();
  }

  @Test
  void shouldRejectBackgroundColorLongerThan64Chars() {
    final String tooLong = "#".repeat(65);
    final Dashboard base = Dashboard.newInstance("sub-1", "Main", false);
    assertThatThrownBy(() -> base.withBackgroundColor(tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("64");
  }

  @Test
  void shouldAcceptBackgroundColorWithExactly64Chars() {
    final String exactly = "x".repeat(64);
    final Dashboard d = Dashboard.newInstance("sub-1", "Main", false).withBackgroundColor(exactly);
    assertThat(d.backgroundColor()).hasSize(64);
  }

  @Test
  void withBackgroundColorShouldChangeColorAndPreserveOtherFields() {
    final Dashboard original = Dashboard.newInstance("sub-1", "Main", true);
    final Dashboard updated = original.withBackgroundColor("#1a1a2e");
    assertThat(updated.backgroundColor()).isEqualTo("#1a1a2e");
    assertThat(original.backgroundColor()).isNull();
    assertThat(updated.userSub()).isEqualTo(original.userSub());
    assertThat(updated.name()).isEqualTo(original.name());
    assertThat(updated.isDefault()).isEqualTo(original.isDefault());
  }

  @Test
  void withNameAndWithDefaultShouldPreserveBackgroundColor() {
    final Dashboard colored =
        Dashboard.newInstance("sub-1", "Main", false).withBackgroundColor("#222");
    assertThat(colored.withName("Neu").backgroundColor()).isEqualTo("#222");
    assertThat(colored.withDefault(true).backgroundColor()).isEqualTo("#222");
  }
}
