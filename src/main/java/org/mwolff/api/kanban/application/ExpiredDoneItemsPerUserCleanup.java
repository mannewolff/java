package org.mwolff.api.kanban.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanSettings;
import org.mwolff.api.kanban.domain.KanbanSettingsPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Archiviert die abgelaufenen DONE-Items genau eines Users in einer eigenen Transaktion (#327 —
 * Soft-Delete statt Löschen).
 *
 * <p>Bewusst als eigene Bean ausgelagert: {@link CleanupExpiredDoneItemsUseCase#execute()} ruft
 * {@link #archiveForUser(String)} über die injizierte Bean-Referenz auf, damit der Spring-Proxy und
 * damit {@link Transactional @Transactional} greifen. Ein früherer Selbstaufruf über {@code this.}
 * umging den Proxy — die {@code @Modifying}-Query lief dann ohne aktive Transaktion und warf im
 * Scheduler-Pfad {@code TransactionRequiredException} (#305).
 */
@Component
public class ExpiredDoneItemsPerUserCleanup {

  private final KanbanItemPort items;
  private final KanbanSettingsPort settings;
  private final Clock clock;

  public ExpiredDoneItemsPerUserCleanup(
      KanbanItemPort items, KanbanSettingsPort settings, Clock clock) {
    this.items = items;
    this.settings = settings;
    this.clock = clock;
  }

  /** Archiviert die abgelaufenen DONE-Items des Users und liefert deren Anzahl. */
  @Transactional
  public int archiveForUser(String userSub) {
    final KanbanSettings effective =
        settings.findByUser(userSub).orElseGet(() -> KanbanSettings.defaultFor(userSub));
    final Instant threshold =
        Instant.now(clock).minus(Duration.ofDays(effective.doneRetentionDays()));
    return items.archiveDoneOlderThan(userSub, threshold);
  }
}
