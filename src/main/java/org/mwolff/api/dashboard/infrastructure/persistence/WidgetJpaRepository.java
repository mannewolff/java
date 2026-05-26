package org.mwolff.api.dashboard.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface WidgetJpaRepository extends JpaRepository<WidgetEntity, Long> {

  List<WidgetEntity> findAllByDashboardIdOrderByIdAsc(Long dashboardId);

  void deleteByDashboardId(Long dashboardId);
}
