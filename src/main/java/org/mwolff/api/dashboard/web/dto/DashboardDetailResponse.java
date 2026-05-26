package org.mwolff.api.dashboard.web.dto;

import java.time.Instant;
import java.util.List;

import org.mwolff.api.dashboard.application.GetDashboardUseCase.DashboardWithWidgets;

/** Detail-Antwort von {@code GET /api/dashboards/{id}} — Dashboard plus alle Widgets. */
public record DashboardDetailResponse(
    long id,
    String name,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt,
    List<WidgetDto> widgets) {

  public static DashboardDetailResponse from(DashboardWithWidgets result) {
    return new DashboardDetailResponse(
        result.dashboard().id(),
        result.dashboard().name(),
        result.dashboard().isDefault(),
        result.dashboard().createdAt(),
        result.dashboard().updatedAt(),
        result.widgets().stream().map(WidgetDto::from).toList());
  }
}
