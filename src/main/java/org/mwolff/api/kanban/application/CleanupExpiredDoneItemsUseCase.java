package org.mwolff.api.kanban.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanSettings;
import org.mwolff.api.kanban.domain.KanbanSettingsPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Iteriert alle User mit mindestens einem DONE-Item, ermittelt deren Retention (Settings oder
 * Default) und löscht abgelaufene Items.
 *
 * <p>Wird vom Scheduler aufgerufen. Pro User eine eigene Transaktion — vermeidet eine große
 * Lock-Geschichte, wenn die Cleanup-Operation lange läuft.
 */
@Component
public class CleanupExpiredDoneItemsUseCase {

  private final KanbanItemPort items;
  private final KanbanSettingsPort settings;
  private final Clock clock;

  public CleanupExpiredDoneItemsUseCase(
      KanbanItemPort items, KanbanSettingsPort settings, Clock clock) {
    this.items = items;
    this.settings = settings;
    this.clock = clock;
  }

  /** Liefert die Summe aller gelöschten Items über alle User — für Logging. */
  public int execute() {
    final List<String> users = items.distinctUsersWithDoneItems();
    int total = 0;
    for (final String userSub : users) {
      total += deleteForUser(userSub);
    }
    return total;
  }

  @Transactional
  protected int deleteForUser(String userSub) {
    final KanbanSettings effective =
        settings.findByUser(userSub).orElseGet(() -> KanbanSettings.defaultFor(userSub));
    final Instant threshold =
        Instant.now(clock).minus(Duration.ofDays(effective.doneRetentionDays()));
    return items.deleteDoneOlderThan(userSub, threshold);
  }
}
