package org.mwolff.api.kanban.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface KanbanItemJpaRepository extends JpaRepository<KanbanItemEntity, Long> {

  // Board- und Positionslogik sehen nur normale Items — Epics nehmen nicht am
  // Spalten-Workflow teil und halten keine aktive Position (#321).
  @Query(
      "select i from KanbanItemEntity i where i.userSub = :userSub and i.archived = false "
          + "and i.itemType = org.mwolff.api.kanban.domain.KanbanItemType.ITEM "
          + "order by i.columnName asc, i.positionInColumn asc")
  List<KanbanItemEntity> findActiveByUserSub(@Param("userSub") String userSub);

  @Query(
      "select i from KanbanItemEntity i where i.userSub = :userSub and i.columnName = :column "
          + "and i.archived = false "
          + "and i.itemType = org.mwolff.api.kanban.domain.KanbanItemType.ITEM "
          + "order by i.positionInColumn asc")
  List<KanbanItemEntity> findActiveByUserSubAndColumn(
      @Param("userSub") String userSub, @Param("column") KanbanColumn column);

  @Query(
      "select i from KanbanItemEntity i where i.userSub = :userSub "
          + "and i.itemType = org.mwolff.api.kanban.domain.KanbanItemType.ITEM "
          + "order by i.columnName asc, i.positionInColumn asc")
  List<KanbanItemEntity> findAllByUserSubIncludingArchived(@Param("userSub") String userSub);

  // Epics eines Users (#322), aufsteigend nach Anzeige-Nummer. Epics sind nie archiviert
  // (kein Archivierungs-Pfad), daher kein archived-Filter nötig.
  @Query(
      "select i from KanbanItemEntity i where i.userSub = :userSub "
          + "and i.itemType = org.mwolff.api.kanban.domain.KanbanItemType.EPIC "
          + "order by i.number asc")
  List<KanbanItemEntity> findEpicsByUserSub(@Param("userSub") String userSub);

  // Zählt alle Kinder eines Epics (inkl. archivierter) für den Referenz-Check vor dem Löschen
  // (#330). Archivierte Kinder halten weiterhin eine parentId-Referenz und dürfen nicht verwaisen.
  @Query("select count(i) from KanbanItemEntity i where i.parentId = :epicId")
  long countByParentId(@Param("epicId") long epicId);

  // Max über ALLE Items des Users (auch archivierte), damit neue Nummern nie kollidieren (#187).
  @Query("select max(i.number) from KanbanItemEntity i where i.userSub = :userSub")
  Optional<Integer> findMaxNumberByUserSub(@Param("userSub") String userSub);

  // Auflösung einer Anzeige-Nummer → Item, user-isoliert (#352, für die Abhängigkeits-Validierung).
  @Query("select i from KanbanItemEntity i where i.userSub = :userSub and i.number = :number")
  Optional<KanbanItemEntity> findByUserSubAndNumber(
      @Param("userSub") String userSub, @Param("number") int number);

  // clearAutomatically: nach dem Bulk-Update den Persistence-Context leeren, damit ein
  // anschließendes findById in derselben Transaktion den frischen Stand liest (nicht die
  // veraltete First-Level-Cache-Entity). Sonst sieht der Leser die Änderung nicht.
  @Modifying(clearAutomatically = true)
  @Query("update KanbanItemEntity i set i.positionInColumn = :newPosition where i.id = :id")
  void updatePosition(@Param("id") long id, @Param("newPosition") int newPosition);

  @Modifying(clearAutomatically = true)
  @Query("update KanbanItemEntity i set i.archived = true where i.id = :id")
  void archiveById(@Param("id") long id);

  @Modifying(clearAutomatically = true)
  @Query("update KanbanItemEntity i set i.archived = false where i.id = :id")
  void restoreById(@Param("id") long id);

  // Abgelaufene DONE-Items werden archiviert (Soft-Delete), nicht gelöscht (#327): sie bleiben
  // über den Archiv-Filter der Listenansicht erreichbar. Bereits archivierte bleiben unangetastet.
  @Modifying(clearAutomatically = true)
  @Query(
      "update KanbanItemEntity i set i.archived = true where i.userSub = :userSub "
          + "and i.columnName = org.mwolff.api.kanban.domain.KanbanColumn.DONE "
          + "and i.movedToDoneAt < :threshold "
          + "and i.archived = false")
  int archiveDoneOlderThan(@Param("userSub") String userSub, @Param("threshold") Instant threshold);

  @Query(
      "select distinct i.userSub from KanbanItemEntity i "
          + "where i.columnName = org.mwolff.api.kanban.domain.KanbanColumn.DONE "
          + "and i.archived = false")
  List<String> distinctUsersWithDoneItems();
}
