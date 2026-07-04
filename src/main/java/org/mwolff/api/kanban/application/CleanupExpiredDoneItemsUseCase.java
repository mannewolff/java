package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;

/**
 * Iteriert alle User mit mindestens einem DONE-Item und löscht deren abgelaufene Items. Die
 * eigentliche Löschung pro User erledigt {@link ExpiredDoneItemsPerUserCleanup#deleteForUser(String)}
 * — als eigene Bean, damit pro User eine eigene, vom Proxy verwaltete Transaktion greift (#305).
 * Das vermeidet eine große Lock-Geschichte über alle User und stellt sicher, dass die
 * {@code @Transactional}-Grenze auch im Scheduler-Pfad wirkt.
 */
@Component
public class CleanupExpiredDoneItemsUseCase {

  private final KanbanItemPort items;
  private final ExpiredDoneItemsPerUserCleanup perUserCleanup;

  public CleanupExpiredDoneItemsUseCase(
      KanbanItemPort items, ExpiredDoneItemsPerUserCleanup perUserCleanup) {
    this.items = items;
    this.perUserCleanup = perUserCleanup;
  }

  /** Liefert die Summe aller gelöschten Items über alle User — für Logging. */
  public int execute() {
    final List<String> users = items.distinctUsersWithDoneItems();
    int total = 0;
    for (final String userSub : users) {
      total += perUserCleanup.deleteForUser(userSub);
    }
    return total;
  }
}
