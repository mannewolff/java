package org.mwolff.api.image.infrastructure.usage;

import java.util.Map;

import org.mwolff.api.dashboard.domain.WidgetImageUsagePort;
import org.mwolff.api.image.domain.ImageUsagePort;
import org.springframework.stereotype.Component;

/**
 * Implementiert den Image-{@link ImageUsagePort} über den Dashboard-{@link WidgetImageUsagePort}
 * (#202). Dies ist die einzige Stelle der bewusst dokumentierten Cross-Modul-Kante {@code image →
 * dashboard} (siehe CrossModuleArchitectureTest).
 */
@Component
class DashboardImageUsageAdapter implements ImageUsagePort {

  private final WidgetImageUsagePort widgetUsage;

  DashboardImageUsageAdapter(final WidgetImageUsagePort widgetUsage) {
    this.widgetUsage = widgetUsage;
  }

  @Override
  public long countUsages(final long imageId) {
    return widgetUsage.countByImageId(imageId);
  }

  @Override
  public Map<Long, Long> usageCounts() {
    return widgetUsage.usageCounts();
  }
}
