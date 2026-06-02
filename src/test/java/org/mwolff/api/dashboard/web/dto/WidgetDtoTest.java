package org.mwolff.api.dashboard.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPosition;
import org.mwolff.api.dashboard.domain.WidgetType;

class WidgetDtoTest {

  @Test
  void fromMapsAllFields() {
    final Widget widget =
        Widget.newInstance(7L, WidgetType.IMAGE, new WidgetPosition(1, 2, 3, 4), "{\"imageId\":5}");

    final WidgetDto dto = WidgetDto.from(widget);

    assertThat(dto).isNotNull();
    assertThat(dto.type()).isEqualTo(WidgetType.IMAGE);
    assertThat(dto.posX()).isEqualTo(1);
    assertThat(dto.posY()).isEqualTo(2);
    assertThat(dto.width()).isEqualTo(3);
    assertThat(dto.height()).isEqualTo(4);
    assertThat(dto.config()).isEqualTo("{\"imageId\":5}");
  }

  @Test
  void toDomainRoundTripsPosition() {
    final WidgetDto dto = new WidgetDto(null, WidgetType.KPI, 0, 0, 2, 2, "{}");

    final Widget widget = dto.toDomain(9L);

    assertThat(widget.dashboardId()).isEqualTo(9L);
    assertThat(widget.position()).isEqualTo(new WidgetPosition(0, 0, 2, 2));
    assertThat(widget.config()).isEqualTo("{}");
  }
}
