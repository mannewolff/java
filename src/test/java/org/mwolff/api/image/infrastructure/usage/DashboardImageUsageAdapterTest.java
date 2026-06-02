package org.mwolff.api.image.infrastructure.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mwolff.api.dashboard.domain.WidgetImageUsagePort;

class DashboardImageUsageAdapterTest {

  private final WidgetImageUsagePort widgetUsage = mock(WidgetImageUsagePort.class);
  private final DashboardImageUsageAdapter adapter = new DashboardImageUsageAdapter(widgetUsage);

  @Test
  void countUsagesDelegates() {
    when(widgetUsage.countByImageId(7L)).thenReturn(4L);

    assertThat(adapter.countUsages(7L)).isEqualTo(4L);
  }

  @Test
  void usageCountsDelegates() {
    when(widgetUsage.usageCounts()).thenReturn(Map.of(7L, 4L));

    assertThat(adapter.usageCounts()).containsEntry(7L, 4L);
  }
}
