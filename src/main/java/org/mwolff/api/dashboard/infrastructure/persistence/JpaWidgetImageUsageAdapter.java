package org.mwolff.api.dashboard.infrastructure.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mwolff.api.dashboard.domain.WidgetImageUsagePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Persistenz-Adapter für {@link WidgetImageUsagePort} auf Basis der widgets-Tabelle (#202). */
@Component
class JpaWidgetImageUsageAdapter implements WidgetImageUsagePort {

  private final WidgetJpaRepository repository;

  JpaWidgetImageUsageAdapter(final WidgetJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public long countByImageId(final long imageId) {
    return repository.countByImageId(imageId);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<Long, Long> usageCounts() {
    final Map<Long, Long> counts = new LinkedHashMap<>();
    for (final WidgetJpaRepository.ImageUsageRow row : repository.aggregateUsage()) {
      counts.put(row.getImageId(), row.getUsageCount());
    }
    return counts;
  }
}
