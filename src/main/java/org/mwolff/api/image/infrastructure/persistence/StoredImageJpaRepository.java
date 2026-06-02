package org.mwolff.api.image.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface StoredImageJpaRepository extends JpaRepository<StoredImageEntity, Long> {

  /** Metadaten-Projektion (ohne Binärdaten) für die Galerie-Paginierung (#198). */
  List<StoredImageMetadataView> findAllByOrderByIdDesc(Pageable pageable);
}
