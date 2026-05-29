package org.mwolff.api.dashboard.application;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Setzt die Hintergrundfarbe eines Dashboards. Owner-Schutz wie alle Dashboard-Use-Cases: fremde
 * Dashboards werfen {@link DashboardNotFoundException} (kein Existenz-Leak). Leerwert/{@code null}
 * entfernt den Override (Normalisierung im Domain-Konstruktor).
 */
@Component
public class SetBackgroundColorUseCase {

  private final DashboardPort dashboards;

  public SetBackgroundColorUseCase(DashboardPort dashboards) {
    this.dashboards = dashboards;
  }

  @Transactional
  public Dashboard execute(String userSub, long dashboardId, String backgroundColor) {
    final Dashboard existing =
        dashboards
            .findById(dashboardId)
            .filter(d -> d.userSub().equals(userSub))
            .orElseThrow(() -> new DashboardNotFoundException(dashboardId));
    return dashboards.save(existing.withBackgroundColor(backgroundColor));
  }
}
