package org.mwolff.api.kanban.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Test;

class KanbanSchedulingConfigTest {

  @Test
  void systemClockShouldReturnUtcClock() {
    final Clock clock = new KanbanSchedulingConfig().systemClock();
    assertThat(clock.getZone()).isEqualTo(java.time.ZoneOffset.UTC);
  }
}
