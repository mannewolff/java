package org.mwolff.api.kanban.infrastructure.scheduler;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Schaltet {@link org.springframework.scheduling.annotation.Scheduled @Scheduled} im Stack frei
 * und stellt eine {@link Clock}-Bean bereit, die Use-Cases für deterministische "now"-Aufrufe
 * injizieren können.
 */
@Configuration
@EnableScheduling
public class KanbanSchedulingConfig {

  @Bean
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
