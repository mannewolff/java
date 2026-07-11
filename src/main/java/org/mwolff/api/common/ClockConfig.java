package org.mwolff.api.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stellt eine modul-neutrale {@link Clock}-Bean bereit, die Use-Cases für deterministische
 * "now"-Aufrufe injizieren (Tests setzen eine feste Clock ein). Lag früher im Kanban-Modul ({@code
 * KanbanSchedulingConfig}); nach dessen Entfernung wandert der von mehreren Modulen (appversion,
 * timeseries, ingest) genutzte Bean hierher ins neutrale {@code common}-Paket.
 */
@Configuration
public class ClockConfig {

  @Bean
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
