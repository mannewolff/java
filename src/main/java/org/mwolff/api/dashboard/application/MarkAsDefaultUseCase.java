package org.mwolff.api.dashboard.application;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Markiert ein Dashboard als Default des Users — atomar: alle anderen Defaults werden zuerst
 * zurückgesetzt, dann das gewählte gesetzt. Beides in einer Transaktion.
 */
@Component
public class MarkAsDefaultUseCase {

  private final DashboardPort dashboards;

  public MarkAsDefaultUseCase(DashboardPort dashboards) {
    this.dashboards = dashboards;
  }

  @Transactional
  public Dashboard execute(String userSub, long dashboardId) {
    final Dashboard existing =
        dashboards
            .findById(dashboardId)
            .filter(d -> d.userSub().equals(userSub))
            .orElseThrow(() -> new DashboardNotFoundException(dashboardId));
    dashboards.clearDefaultForUser(userSub);
    return dashboards.save(existing.withDefault(true));
  }
}
