package org.mwolff.api.image.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StoredImageJpaRepository extends JpaRepository<StoredImageEntity, Long> {

  /** Lädt ein Bild nur, wenn es dem User gehört (#230). */
  Optional<StoredImageEntity> findByIdAndUserSub(long id, String userSub);

  /** Metadaten-Projektion (ohne Binärdaten) für die Galerie-Paginierung des Users (#198, #230). */
  List<StoredImageMetadataView> findByUserSubOrderByIdDesc(String userSub, Pageable pageable);

  /** Anzahl der Bilder des Users (#230). */
  long countByUserSub(String userSub);

  /** Ids eigener Bilder mit diesem Hash, aufsteigend (#199, #230) — ohne Binärdaten zu laden. */
  @Query(
      "select s.id from StoredImageEntity s "
          + "where s.hash = :hash and s.userSub = :userSub order by s.id asc")
  List<Long> findIdsByHashAndUserSub(@Param("hash") String hash, @Param("userSub") String userSub);
}
