package org.mwolff.api.dashboard.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPort;
import org.mwolff.api.dashboard.domain.WidgetPosition;
import org.springframework.stereotype.Component;

/**
 * JPA-Implementierung von {@link DashboardPort} und {@link WidgetPort}.
 *
 * <p>Beide Ports liegen in einem Adapter, weil sie über dieselben JPA-Repos und Mappings laufen.
 * Das hält die Mapping-Logik zentral.
 */
@Component
class JpaDashboardAdapter implements DashboardPort, WidgetPort {

  private final DashboardJpaRepository dashboardRepo;
  private final WidgetJpaRepository widgetRepo;

  JpaDashboardAdapter(DashboardJpaRepository dashboardRepo, WidgetJpaRepository widgetRepo) {
    this.dashboardRepo = dashboardRepo;
    this.widgetRepo = widgetRepo;
  }

  // ----- DashboardPort -----------------------------------------------------

  @Override
  public List<Dashboard> findAllByUser(String userSub) {
    return dashboardRepo.findAllByUserSubOrderByCreatedAtAsc(userSub).stream()
        .map(JpaDashboardAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<Dashboard> findById(long id) {
    return dashboardRepo.findById(id).map(JpaDashboardAdapter::toDomain);
  }

  @Override
  public Optional<Dashboard> findDefaultByUser(String userSub) {
    return dashboardRepo
        .findFirstByUserSubAndIsDefaultTrue(userSub)
        .map(JpaDashboardAdapter::toDomain);
  }

  @Override
  public Dashboard save(Dashboard dashboard) {
    final DashboardEntity entity;
    if (dashboard.id() == null) {
      entity =
          new DashboardEntity(null, dashboard.userSub(), dashboard.name(), dashboard.isDefault());
    } else {
      entity =
          dashboardRepo
              .findById(dashboard.id())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Dashboard " + dashboard.id() + " disappeared during save"));
      entity.setDefault(dashboard.isDefault());
    }
    return toDomain(dashboardRepo.save(entity));
  }

  @Override
  public void deleteById(long id) {
    dashboardRepo.deleteById(id);
  }

  @Override
  public void clearDefaultForUser(String userSub) {
    dashboardRepo.clearDefaultForUser(userSub);
  }

  // ----- WidgetPort --------------------------------------------------------

  @Override
  public List<Widget> findAllByDashboard(long dashboardId) {
    return widgetRepo.findAllByDashboardIdOrderByIdAsc(dashboardId).stream()
        .map(JpaDashboardAdapter::toDomain)
        .toList();
  }

  @Override
  public List<Widget> replaceAllForDashboard(long dashboardId, List<Widget> widgets) {
    widgetRepo.deleteByDashboardId(dashboardId);
    widgetRepo.flush();
    final List<WidgetEntity> entities =
        widgets.stream()
            .map(
                w ->
                    new WidgetEntity(
                        dashboardId,
                        w.type(),
                        w.position().posX(),
                        w.position().posY(),
                        w.position().width(),
                        w.position().height(),
                        w.config()))
            .toList();
    return widgetRepo.saveAll(entities).stream().map(JpaDashboardAdapter::toDomain).toList();
  }

  @Override
  public void deleteByDashboard(long dashboardId) {
    widgetRepo.deleteByDashboardId(dashboardId);
  }

  // ----- Mapping -----------------------------------------------------------

  private static Dashboard toDomain(DashboardEntity entity) {
    return new Dashboard(
        entity.getId(),
        entity.getUserSub(),
        entity.getName(),
        entity.isDefault(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private static Widget toDomain(WidgetEntity entity) {
    return new Widget(
        entity.getId(),
        entity.getDashboardId(),
        entity.getType(),
        new WidgetPosition(
            entity.getPosX(), entity.getPosY(), entity.getWidth(), entity.getHeight()),
        entity.getConfig(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
