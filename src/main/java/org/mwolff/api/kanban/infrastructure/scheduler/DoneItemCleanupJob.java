package org.mwolff.api.kanban.infrastructure.scheduler;

import org.mwolff.api.kanban.application.CleanupExpiredDoneItemsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spring-Scheduled-Wrapper um {@link CleanupExpiredDoneItemsUseCase}. Aktivierung und Cron sind per
 * Property steuerbar:
 *
 * <ul>
 *   <li>{@code toolbox.kanban.cleanup.enabled} — true/false (Default: true)
 *   <li>{@code toolbox.kanban.cleanup-cron} — Cron-Expression (Default: täglich 03:00 UTC)
 * </ul>
 */
@Component
@ConditionalOnProperty(
    name = "toolbox.kanban.cleanup.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class DoneItemCleanupJob {

  private static final Logger LOG = LoggerFactory.getLogger(DoneItemCleanupJob.class);

  private final CleanupExpiredDoneItemsUseCase cleanup;

  public DoneItemCleanupJob(CleanupExpiredDoneItemsUseCase cleanup) {
    this.cleanup = cleanup;
  }

  @Scheduled(cron = "${toolbox.kanban.cleanup-cron:0 0 3 * * *}")
  public void run() {
    final int archived = cleanup.execute();
    if (archived > 0) {
      LOG.info("Kanban cleanup archived {} expired DONE items", archived);
    }
  }
}
