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
 * Ersetzt das komplette Widget-Layout eines Dashboards in einer Transaktion: alte Widgets weg, neue
 * rein. Eignet sich für das übliche "Layout speichern"-Pattern (Drag/Drop ändert lokalen State, ein
 * Speichern-Button synchronisiert).
 */
@Component
public class UpdateLayoutUseCase {

  private final DashboardPort dashboards;
  private final WidgetPort widgets;

  public UpdateLayoutUseCase(DashboardPort dashboards, WidgetPort widgets) {
    this.dashboards = dashboards;
    this.widgets = widgets;
  }

  @Transactional
  public List<Widget> execute(String userSub, long dashboardId, List<Widget> newWidgets) {
    final Dashboard owned =
        dashboards
            .findById(dashboardId)
            .filter(d -> d.userSub().equals(userSub))
            .orElseThrow(() -> new DashboardNotFoundException(dashboardId));
    return widgets.replaceAllForDashboard(owned.id(), newWidgets);
  }
}
