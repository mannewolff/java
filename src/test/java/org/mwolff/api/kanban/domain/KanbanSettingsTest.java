package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KanbanSettingsTest {

  @Test
  void defaultForShouldUseDefaultRetention() {
    final KanbanSettings s = KanbanSettings.defaultFor("sub-1");
    assertThat(s.userSub()).isEqualTo("sub-1");
    assertThat(s.doneRetentionDays()).isEqualTo(KanbanSettings.DEFAULT_RETENTION_DAYS);
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(() -> new KanbanSettings(null, 5)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(() -> new KanbanSettings("  ", 5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectRetentionBelowMin() {
    assertThatThrownBy(() -> new KanbanSettings("u", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectRetentionAboveMax() {
    assertThatThrownBy(() -> new KanbanSettings("u", 31))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAcceptMinBoundary() {
    assertThat(new KanbanSettings("u", 1).doneRetentionDays()).isEqualTo(1);
  }

  @Test
  void shouldAcceptMaxBoundary() {
    assertThat(new KanbanSettings("u", 30).doneRetentionDays()).isEqualTo(30);
  }
}
