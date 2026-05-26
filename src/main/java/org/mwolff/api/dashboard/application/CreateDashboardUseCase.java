package org.mwolff.api.dashboard.application;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erstellt ein neues Dashboard für den User. Wenn der User noch keins hatte, wird das neue
 * automatisch als Default markiert; sonst bleibt der bisherige Default erhalten.
 */
@Component
public class CreateDashboardUseCase {

  private final DashboardPort dashboards;

  public CreateDashboardUseCase(DashboardPort dashboards) {
    this.dashboards = dashboards;
  }

  @Transactional
  public Dashboard execute(String userSub, String name) {
    final boolean isFirst = dashboards.findAllByUser(userSub).isEmpty();
    return dashboards.save(Dashboard.newInstance(userSub, name, isFirst));
  }
}
