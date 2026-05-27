package org.mwolff.api.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WidgetTest {

  private final WidgetPosition pos = new WidgetPosition(0, 0, 2, 2);

  @Test
  void newInstanceShouldHaveNullIdAndTimestamps() {
    final Widget w = Widget.newInstance(1L, WidgetType.TEXTBOX, pos, "{}");
    assertThat(w.id()).isNull();
    assertThat(w.dashboardId()).isEqualTo(1L);
    assertThat(w.type()).isEqualTo(WidgetType.TEXTBOX);
    assertThat(w.position()).isEqualTo(pos);
    assertThat(w.config()).isEqualTo("{}");
  }

  @Test
  void shouldRejectNullType() {
    assertThatThrownBy(() -> Widget.newInstance(1L, null, pos, "{}"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectNullPosition() {
    assertThatThrownBy(() -> Widget.newInstance(1L, WidgetType.KPI, null, "{}"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectNullConfig() {
    assertThatThrownBy(() -> Widget.newInstance(1L, WidgetType.KPI, pos, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldAllowNullDashboardIdForUnsavedWidgets() {
    final Widget w = Widget.newInstance(null, WidgetType.TEXTBOX, pos, "{}");
    assertThat(w.dashboardId()).isNull();
  }
}
