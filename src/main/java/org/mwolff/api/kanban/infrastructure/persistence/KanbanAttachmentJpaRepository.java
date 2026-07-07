package org.mwolff.api.kanban.infrastructure.persistence;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanAttachmentMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface KanbanAttachmentJpaRepository extends JpaRepository<KanbanAttachmentEntity, Long> {

  /**
   * Metadaten-Projektion — lädt bewusst NICHT die {@code data}-Spalte (Blob), damit die Liste
   * günstig bleibt. Älteste zuerst; {@code id} als deterministischer Tiebreaker.
   */
  @Query(
      "select new org.mwolff.api.kanban.domain.KanbanAttachmentMeta("
          + "a.id, a.itemId, a.filename, a.contentType, a.sizeBytes, a.uploadedBySub, a.createdAt) "
          + "from KanbanAttachmentEntity a where a.itemId = :itemId "
          + "order by a.createdAt asc, a.id asc")
  List<KanbanAttachmentMeta> findMetaByItemId(@Param("itemId") long itemId);

  long countByItemId(long itemId);
}
