package org.mwolff.api.dashboard.application;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Benennt ein Dashboard um. Owner-Schutz wie alle Dashboard-Use-Cases: fremde Dashboards werfen
 * {@link DashboardNotFoundException} (kein Existenz-Leak).
 */
@Component
public class RenameDashboardUseCase {

  private final DashboardPort dashboards;

  public RenameDashboardUseCase(DashboardPort dashboards) {
    this.dashboards = dashboards;
  }

  @Transactional
  public Dashboard execute(String userSub, long dashboardId, String newName) {
    final Dashboard existing =
        dashboards
            .findById(dashboardId)
            .filter(d -> d.userSub().equals(userSub))
            .orElseThrow(() -> new DashboardNotFoundException(dashboardId));
    return dashboards.save(existing.withName(newName));
  }
}
