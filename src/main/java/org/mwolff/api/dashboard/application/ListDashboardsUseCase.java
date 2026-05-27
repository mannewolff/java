package org.mwolff.api.dashboard.application;

import java.util.List;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.springframework.stereotype.Component;

/** Liefert alle Dashboards eines Users. */
@Component
public class ListDashboardsUseCase {

  private final DashboardPort dashboards;

  public ListDashboardsUseCase(DashboardPort dashboards) {
    this.dashboards = dashboards;
  }

  public List<Dashboard> execute(String userSub) {
    return dashboards.findAllByUser(userSub);
  }
}
