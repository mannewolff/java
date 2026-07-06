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

  @Test
  void shouldAcceptConfigAtMaxBytes() {
    final String atLimit = "a".repeat(Widget.MAX_CONFIG_BYTES); // ASCII: 1 Byte pro Zeichen
    final Widget w = Widget.newInstance(1L, WidgetType.TEXTBOX, pos, atLimit);
    assertThat(w.config()).hasSize(Widget.MAX_CONFIG_BYTES);
  }

  @Test
  void shouldRejectConfigExceedingMaxBytes() {
    final String tooLong = "a".repeat(Widget.MAX_CONFIG_BYTES + 1);
    assertThatThrownBy(() -> Widget.newInstance(1L, WidgetType.TEXTBOX, pos, tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bytes");
  }

  @Test
  void shouldMeasureConfigInBytesNotChars() {
    // "ä" ist 1 Zeichen, aber 2 Bytes in UTF-8. Eine Zeichenkette knapp unter dem Byte-Limit in
    // Zeichen, aber darüber in Bytes, muss abgelehnt werden — sonst Truncation/500 in der DB.
    final int chars = Widget.MAX_CONFIG_BYTES / 2 + 1; // 2 Bytes/Zeichen -> > MAX_CONFIG_BYTES
    final String multiByte = "ä".repeat(chars);
    assertThat(multiByte.length()).isLessThanOrEqualTo(Widget.MAX_CONFIG_BYTES);
    assertThatThrownBy(() -> Widget.newInstance(1L, WidgetType.TEXTBOX, pos, multiByte))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bytes");
  }
}
