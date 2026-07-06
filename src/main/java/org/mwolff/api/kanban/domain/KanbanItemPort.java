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

  /**
   * Alle Epics eines Users (Typ {@link KanbanItemType#EPIC}), aufsteigend nach Anzeige-Nummer.
   * Epics erscheinen nicht auf dem Board; diese Methode speist die Epic-Ansicht (#322).
   */
  List<KanbanItem> findEpicsByUser(String userSub);

  Optional<KanbanItem> findById(long id);

  /**
   * Zählt alle Items, die dem Epic mit {@code epicId} zugeordnet sind ({@code parentId == epicId}),
   * inklusive archivierter Items. Grundlage des Referenz-Checks vor dem Löschen eines Epics (#330):
   * ein archiviertes Kind hält weiterhin eine Referenz und darf nicht verwaisen.
   */
  long countChildren(long epicId);

  /**
   * Höchste vergebene Anzeige-Nummer (#187) eines Users über ALLE Items (inkl. archivierter), damit
   * neu vergebene Nummern nie mit denen archivierter Items kollidieren (Unique-Constraint). Leer,
   * wenn der User noch keine Items hat.
   */
  Optional<Integer> getMaxNumberForUser(String userSub);

  KanbanItem save(KanbanItem item);

  /**
   * Schreibt eine Position-Aktualisierung für ein vorhandenes Item. Wird vom Move-Use-Case während
   * der Re-Index-Schleife auf benachbarten Items aufgerufen.
   */
  void updatePosition(long id, int newPosition);

  void deleteById(long id);

  /** Setzt {@code archived = true} ohne physisches Löschen. */
  void archiveById(long id);

  /** Setzt {@code archived = false} (Wiederherstellung). */
  void restoreById(long id);

  /**
   * Alle Items des Users inkl. archivierter, sortiert nach (column, position). Für die
   * Archiv-Ansicht — normale Abfragen verwenden {@link #findAllByUser}.
   */
  List<KanbanItem> findAllByUserIncludingArchived(String userSub);

  /**
   * Archiviert alle nicht-archivierten Items eines Users, die in der DONE-Spalte liegen und deren
   * {@code movedToDoneAt} vor dem übergebenen Threshold liegt (#327). Bereits archivierte Items
   * bleiben unangetastet. Verwendet vom Auto-Archivierungs-Scheduler.
   *
   * @return Anzahl archivierter Items, für Logging.
   */
  int archiveDoneOlderThan(String userSub, Instant threshold);

  /**
   * Liefert die distinkten {@code userSub}s, die mindestens ein nicht-archiviertes DONE-Item haben
   * (Cleanup-Iteration).
   */
  List<String> distinctUsersWithDoneItems();
}
