package org.mwolff.api.dashboard.application;

import java.util.List;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert ein Dashboard inkl. seiner Widgets. Wenn der aufrufende User nicht Eigentümer ist, wird
 * {@link DashboardNotFoundException} geworfen (kein Existenz-Leak via 403).
 */
@Component
public class GetDashboardUseCase {

  private final DashboardPort dashboards;
  private final WidgetPort widgets;

  public GetDashboardUseCase(DashboardPort dashboards, WidgetPort widgets) {
    this.dashboards = dashboards;
    this.widgets = widgets;
  }

  /** Ergebnis-Record: Dashboard plus seine Widgets. */
  public record DashboardWithWidgets(Dashboard dashboard, List<Widget> widgets) {}

  @Transactional(readOnly = true)
  public DashboardWithWidgets execute(String userSub, long dashboardId) {
    final Dashboard dashboard = loadOwned(userSub, dashboardId);
    return new DashboardWithWidgets(dashboard, widgets.findAllByDashboard(dashboardId));
  }

  private Dashboard loadOwned(String userSub, long dashboardId) {
    return dashboards
        .findById(dashboardId)
        .filter(d -> d.userSub().equals(userSub))
        .orElseThrow(() -> new DashboardNotFoundException(dashboardId));
  }
}
