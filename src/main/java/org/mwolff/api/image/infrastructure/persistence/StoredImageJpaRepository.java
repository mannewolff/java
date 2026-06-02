package org.mwolff.api.image.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StoredImageJpaRepository extends JpaRepository<StoredImageEntity, Long> {

  /** Metadaten-Projektion (ohne Binärdaten) für die Galerie-Paginierung (#198). */
  List<StoredImageMetadataView> findAllByOrderByIdDesc(Pageable pageable);

  /** Ids existierender Bilder mit diesem Hash, aufsteigend (#199) — ohne Binärdaten zu laden. */
  @Query("select s.id from StoredImageEntity s where s.hash = :hash order by s.id asc")
  List<Long> findIdsByHash(@Param("hash") String hash);
}
