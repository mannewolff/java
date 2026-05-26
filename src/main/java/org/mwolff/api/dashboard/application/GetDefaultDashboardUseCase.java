package org.mwolff.api.dashboard.application;

import java.util.Optional;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert das Default-Dashboard des Users — oder leer, wenn er gar keins hat.
 *
 * <p>Wenn kein explizites Default markiert ist, fällt die Methode auf das erste Dashboard
 * (chronologisch) zurück. So ist ein deterministisches Verhalten garantiert, ohne dass das Frontend
 * Default-Auswahl-Logik braucht.
 */
@Component
public class GetDefaultDashboardUseCase {

  private final DashboardPort dashboards;

  public GetDefaultDashboardUseCase(DashboardPort dashboards) {
    this.dashboards = dashboards;
  }

  @Transactional(readOnly = true)
  public Optional<Dashboard> execute(String userSub) {
    return dashboards
        .findDefaultByUser(userSub)
        .or(() -> dashboards.findAllByUser(userSub).stream().findFirst());
  }
}
