package org.mwolff.api.dashboard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JpaWidgetImageUsageAdapterTest {

  private final WidgetJpaRepository repository = mock(WidgetJpaRepository.class);
  private final JpaWidgetImageUsageAdapter adapter = new JpaWidgetImageUsageAdapter(repository);

  private static WidgetJpaRepository.ImageUsageRow row(final long imageId, final long count) {
    final WidgetJpaRepository.ImageUsageRow r = mock(WidgetJpaRepository.ImageUsageRow.class);
    when(r.getImageId()).thenReturn(imageId);
    when(r.getUsageCount()).thenReturn(count);
    return r;
  }

  @Test
  void countByImageIdDelegates() {
    when(repository.countByImageId(5L)).thenReturn(3L);

    assertThat(adapter.countByImageId(5L)).isEqualTo(3L);
  }

  @Test
  void usageCountsMapsRowsToMap() {
    // Rows vorab erstellen — nicht inline im when(), sonst „unfinished stubbing".
    final WidgetJpaRepository.ImageUsageRow r1 = row(5L, 2L);
    final WidgetJpaRepository.ImageUsageRow r2 = row(8L, 1L);
    when(repository.aggregateUsage()).thenReturn(List.of(r1, r2));

    final Map<Long, Long> counts = adapter.usageCounts();

    assertThat(counts).containsOnly(Map.entry(5L, 2L), Map.entry(8L, 1L));
  }

  @Test
  void usageCountsEmptyWhenNoReferences() {
    when(repository.aggregateUsage()).thenReturn(List.of());

    assertThat(adapter.usageCounts()).isEmpty();
  }
}
