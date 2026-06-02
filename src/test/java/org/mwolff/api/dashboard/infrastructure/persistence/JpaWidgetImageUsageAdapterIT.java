package org.mwolff.api.dashboard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPosition;
import org.mwolff.api.dashboard.domain.WidgetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Integrationstest der nativen Referenz-Query {@link JpaWidgetImageUsageAdapter} gegen MariaDB
 * (#202). Verifiziert JSON_EXTRACT auf der widgets.config-Spalte: nur IMAGE-Widgets zählen, andere
 * Typen und Widgets ohne imageId werden ignoriert.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaDashboardAdapter.class, JpaWidgetImageUsageAdapter.class})
class JpaWidgetImageUsageAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaDashboardAdapter dashboardAdapter;
  @Autowired private JpaWidgetImageUsageAdapter usageAdapter;

  private static Widget image(final long dashboardId, final String config) {
    return Widget.newInstance(
        dashboardId, WidgetType.IMAGE, new WidgetPosition(0, 0, 2, 2), config);
  }

  private long seedWidgets() {
    final Dashboard d = dashboardAdapter.save(Dashboard.newInstance("user-a", "Board", true));
    dashboardAdapter.replaceAllForDashboard(
        d.id(),
        List.of(
            image(d.id(), "{\"imageId\":5}"),
            image(d.id(), "{\"imageId\":5}"),
            image(d.id(), "{\"imageId\":8}"),
            image(d.id(), "{\"imageId\":null}"),
            // Nicht-IMAGE-Widget mit zufällig gleichem Feld darf NICHT mitzählen.
            Widget.newInstance(
                d.id(), WidgetType.KPI, new WidgetPosition(0, 0, 1, 1), "{\"imageId\":5}")));
    return d.id();
  }

  @Test
  void countByImageIdCountsOnlyImageWidgets() {
    seedWidgets();

    assertThat(usageAdapter.countByImageId(5L)).isEqualTo(2L);
    assertThat(usageAdapter.countByImageId(8L)).isEqualTo(1L);
    assertThat(usageAdapter.countByImageId(999L)).isZero();
  }

  @Test
  void aggregateUsageGroupsByImageId() {
    seedWidgets();

    final Map<Long, Long> counts = usageAdapter.usageCounts();

    assertThat(counts).containsOnly(Map.entry(5L, 2L), Map.entry(8L, 1L));
  }
}
