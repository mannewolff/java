package org.mwolff.api.kanban.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface KanbanItemJpaRepository extends JpaRepository<KanbanItemEntity, Long> {

  List<KanbanItemEntity> findAllByUserSubOrderByColumnNameAscPositionInColumnAsc(String userSub);

  List<KanbanItemEntity> findAllByUserSubAndColumnNameOrderByPositionInColumnAsc(
      String userSub, KanbanColumn columnName);

  @Modifying
  @Query("update KanbanItemEntity i set i.positionInColumn = :newPosition where i.id = :id")
  void updatePosition(@Param("id") long id, @Param("newPosition") int newPosition);

  @Modifying
  @Query(
      "delete from KanbanItemEntity i where i.userSub = :userSub "
          + "and i.columnName = org.mwolff.api.kanban.domain.KanbanColumn.DONE "
          + "and i.movedToDoneAt < :threshold")
  int deleteDoneOlderThan(@Param("userSub") String userSub, @Param("threshold") Instant threshold);

  @Query(
      "select distinct i.userSub from KanbanItemEntity i "
          + "where i.columnName = org.mwolff.api.kanban.domain.KanbanColumn.DONE")
  List<String> distinctUsersWithDoneItems();
}
