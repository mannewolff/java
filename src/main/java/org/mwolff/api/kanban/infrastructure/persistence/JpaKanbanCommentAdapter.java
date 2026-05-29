package org.mwolff.api.kanban.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.mwolff.api.kanban.domain.KanbanComment;
import org.mwolff.api.kanban.domain.KanbanCommentPort;
import org.springframework.stereotype.Component;

/** JPA-Implementierung von {@link KanbanCommentPort}. */
@Component
class JpaKanbanCommentAdapter implements KanbanCommentPort {

  private final KanbanCommentJpaRepository repo;

  JpaKanbanCommentAdapter(KanbanCommentJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public List<KanbanComment> findByItemNewestFirst(long itemId) {
    return repo.findByItemIdOrderByCreatedAtDescIdDesc(itemId).stream()
        .map(JpaKanbanCommentAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<KanbanComment> findById(long id) {
    return repo.findById(id).map(JpaKanbanCommentAdapter::toDomain);
  }

  @Override
  public KanbanComment save(KanbanComment comment) {
    final KanbanCommentEntity entity;
    if (comment.id() == null) {
      entity = new KanbanCommentEntity(comment.itemId(), comment.author(), comment.body());
    } else {
      entity =
          repo.findById(comment.id())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Kanban comment " + comment.id() + " disappeared during save"));
      entity.setBody(comment.body());
    }
    return toDomain(repo.save(entity));
  }

  @Override
  public void deleteById(long id) {
    repo.deleteById(id);
  }

  private static KanbanComment toDomain(KanbanCommentEntity entity) {
    return new KanbanComment(
        entity.getId(),
        entity.getItemId(),
        entity.getAuthor(),
        entity.getBody(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
