package org.mwolff.api.dashboard.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPosition;
import org.mwolff.api.dashboard.domain.WidgetType;

/**
 * Wire-Format eines Widgets. Wird beim Anlegen (POST/PUT) ohne {@code id} geschickt und beim Lesen
 * (GET) mit {@code id}.
 */
public record WidgetDto(
    Long id,
    @NotNull WidgetType type,
    @Min(0) int posX,
    @Min(0) int posY,
    @Min(1) int width,
    @Min(1) int height,
    @NotNull @NotBlank @Size(max = Widget.MAX_CONFIG_BYTES) String config) {

  public static WidgetDto from(Widget widget) {
    return new WidgetDto(
        widget.id(),
        widget.type(),
        widget.position().posX(),
        widget.position().posY(),
        widget.position().width(),
        widget.position().height(),
        widget.config());
  }

  public Widget toDomain(Long dashboardId) {
    return Widget.newInstance(
        dashboardId, type, new WidgetPosition(posX, posY, width, height), config);
  }
}
