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

  // Max über ALLE Items des Users (auch archivierte), damit neue Nummern nie kollidieren (#187).
  @Query("select max(i.number) from KanbanItemEntity i where i.userSub = :userSub")
  Optional<Integer> findMaxNumberByUserSub(@Param("userSub") String userSub);

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

  @Modifying(clearAutomatically = true)
  @Query(
      "delete from KanbanItemEntity i where i.userSub = :userSub "
          + "and i.columnName = org.mwolff.api.kanban.domain.KanbanColumn.DONE "
          + "and i.movedToDoneAt < :threshold "
          + "and i.archived = false")
  int deleteDoneOlderThan(@Param("userSub") String userSub, @Param("threshold") Instant threshold);

  @Query(
      "select distinct i.userSub from KanbanItemEntity i "
          + "where i.columnName = org.mwolff.api.kanban.domain.KanbanColumn.DONE "
          + "and i.archived = false")
  List<String> distinctUsersWithDoneItems();
}
