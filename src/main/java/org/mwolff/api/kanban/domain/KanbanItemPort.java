package org.mwolff.api.kanban.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistenz-Port für {@link KanbanItem}s. Implementiert im Infrastructure-Layer via JPA.
 *
 * <p>Methoden mit {@code userSub}-Parameter filtern serverseitig nach Eigentum. {@code findById}
 * ist owner-agnostisch und muss vom Use-Case mit einer expliziten Eigentumsprüfung umrahmt werden
 * (Pattern analog Dashboard).
 */
public interface KanbanItemPort {

  /** Alle Items des Users, in fester Reihenfolge: (column, position) aufsteigend. */
  List<KanbanItem> findAllByUser(String userSub);

  /** Alle Items einer Spalte für einen User, aufsteigend nach position. */
  List<KanbanItem> findByUserAndColumn(String userSub, KanbanColumn column);

  Optional<KanbanItem> findById(long id);

  KanbanItem save(KanbanItem item);

  /**
   * Schreibt eine Position-Aktualisierung für ein vorhandenes Item. Wird vom Move-Use-Case während
   * der Re-Index-Schleife auf benachbarten Items aufgerufen.
   */
  void updatePosition(long id, int newPosition);

  void deleteById(long id);

  /**
   * Löscht alle Items eines Users, die in der DONE-Spalte liegen und deren {@code movedToDoneAt}
   * vor dem übergebenen Threshold liegt. Verwendet vom Auto-Cleanup-Scheduler.
   *
   * @return Anzahl gelöschter Items, für Logging.
   */
  int deleteDoneOlderThan(String userSub, Instant threshold);

  /**
   * Liefert die distinkten {@code userSub}s, die mindestens ein DONE-Item haben
   * (Cleanup-Iteration).
   */
  List<String> distinctUsersWithDoneItems();
}
