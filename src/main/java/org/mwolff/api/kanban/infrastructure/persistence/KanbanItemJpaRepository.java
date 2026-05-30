package org.mwolff.api.kanban.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface KanbanItemJpaRepository extends JpaRepository<KanbanItemEntity, Long> {

  @Query(
      "select i from KanbanItemEntity i where i.userSub = :userSub and i.archived = false "
          + "order by i.columnName asc, i.positionInColumn asc")
  List<KanbanItemEntity> findActiveByUserSub(@Param("userSub") String userSub);

  @Query(
      "select i from KanbanItemEntity i where i.userSub = :userSub and i.columnName = :column "
          + "and i.archived = false order by i.positionInColumn asc")
  List<KanbanItemEntity> findActiveByUserSubAndColumn(
      @Param("userSub") String userSub, @Param("column") KanbanColumn column);

  @Query(
      "select i from KanbanItemEntity i where i.userSub = :userSub "
          + "order by i.columnName asc, i.positionInColumn asc")
  List<KanbanItemEntity> findAllByUserSubIncludingArchived(@Param("userSub") String userSub);

  @Modifying
  @Query("update KanbanItemEntity i set i.positionInColumn = :newPosition where i.id = :id")
  void updatePosition(@Param("id") long id, @Param("newPosition") int newPosition);

  @Modifying
  @Query("update KanbanItemEntity i set i.archived = true where i.id = :id")
  void archiveById(@Param("id") long id);

  @Modifying
  @Query("update KanbanItemEntity i set i.archived = false where i.id = :id")
  void restoreById(@Param("id") long id);

  @Modifying
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
