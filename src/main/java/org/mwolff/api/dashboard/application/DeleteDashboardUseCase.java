package org.mwolff.api.dashboard.application;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.mwolff.api.dashboard.domain.WidgetPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Löscht ein Dashboard inkl. aller seiner Widgets. Owner-Check ist Pflicht — Fremdzugriff wird als
 * {@link DashboardNotFoundException} (404) behandelt.
 */
@Component
public class DeleteDashboardUseCase {

  private final DashboardPort dashboards;
  private final WidgetPort widgets;

  public DeleteDashboardUseCase(DashboardPort dashboards, WidgetPort widgets) {
    this.dashboards = dashboards;
    this.widgets = widgets;
  }

  @Transactional
  public void execute(String userSub, long dashboardId) {
    final Dashboard owned =
        dashboards
            .findById(dashboardId)
            .filter(d -> d.userSub().equals(userSub))
            .orElseThrow(() -> new DashboardNotFoundException(dashboardId));
    widgets.deleteByDashboard(owned.id());
    dashboards.deleteById(owned.id());
  }
}
