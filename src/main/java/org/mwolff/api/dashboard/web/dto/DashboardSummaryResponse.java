package org.mwolff.api.dashboard.web.dto;

import java.time.Instant;

import org.mwolff.api.dashboard.domain.Dashboard;

/** Listen-Eintrag für {@code GET /api/dashboards}. Ohne Widgets — die kommen per Detail-Call. */
public record DashboardSummaryResponse(
    long id, String name, boolean isDefault, Instant createdAt, Instant updatedAt) {

  public static DashboardSummaryResponse from(Dashboard dashboard) {
    return new DashboardSummaryResponse(
        dashboard.id(),
        dashboard.name(),
        dashboard.isDefault(),
        dashboard.createdAt(),
        dashboard.updatedAt());
  }
}
