package org.mwolff.api.kanban.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.domain.KanbanAttachmentMeta;
import org.mwolff.api.kanban.domain.KanbanAttachmentPort;
import org.springframework.stereotype.Component;

/** JPA-Implementierung von {@link KanbanAttachmentPort}. */
@Component
class JpaKanbanAttachmentAdapter implements KanbanAttachmentPort {

  private final KanbanAttachmentJpaRepository repo;

  JpaKanbanAttachmentAdapter(KanbanAttachmentJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public List<KanbanAttachmentMeta> findMetaByItem(long itemId) {
    return repo.findMetaByItemId(itemId);
  }

  @Override
  public Optional<KanbanAttachment> findById(long id) {
    return repo.findById(id).map(JpaKanbanAttachmentAdapter::toDomain);
  }

  @Override
  public KanbanAttachment save(KanbanAttachment attachment) {
    // Anhänge sind unveränderlich — es gibt nur Insert (id == null) und kein Update.
    final KanbanAttachmentEntity entity =
        new KanbanAttachmentEntity(
            attachment.itemId(),
            attachment.filename(),
            attachment.contentType(),
            (int) attachment.sizeBytes(),
            attachment.data(),
            attachment.hash(),
            attachment.uploadedBySub());
    return toDomain(repo.save(entity));
  }

  @Override
  public void deleteById(long id) {
    repo.deleteById(id);
  }

  @Override
  public long countByItem(long itemId) {
    return repo.countByItemId(itemId);
  }

  private static KanbanAttachment toDomain(KanbanAttachmentEntity entity) {
    return new KanbanAttachment(
        entity.getId(),
        entity.getItemId(),
        entity.getFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getData(),
        entity.getHash(),
        entity.getUploadedBySub(),
        entity.getCreatedAt());
  }
}
