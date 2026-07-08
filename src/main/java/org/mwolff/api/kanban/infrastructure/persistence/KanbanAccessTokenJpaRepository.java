package org.mwolff.api.kanban.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface KanbanAccessTokenJpaRepository extends JpaRepository<KanbanAccessTokenEntity, Long> {

  List<KanbanAccessTokenEntity> findAllByUserSubOrderByCreatedAtAsc(String userSub);

  Optional<KanbanAccessTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);
}
