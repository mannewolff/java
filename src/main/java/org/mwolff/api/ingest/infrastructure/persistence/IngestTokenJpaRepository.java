package org.mwolff.api.ingest.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface IngestTokenJpaRepository extends JpaRepository<IngestTokenEntity, Long> {

  List<IngestTokenEntity> findAllByUserSubOrderByCreatedAtAsc(String userSub);

  Optional<IngestTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);
}
