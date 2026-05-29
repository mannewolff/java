package org.mwolff.api.kanban.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface KanbanCommentJpaRepository extends JpaRepository<KanbanCommentEntity, Long> {

  /** Neueste zuerst; {@code id} als deterministischer Tiebreaker bei gleichem Sekunden-Stempel. */
  List<KanbanCommentEntity> findByItemIdOrderByCreatedAtDescIdDesc(long itemId);
}
