package org.mwolff.api.kanban.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.mwolff.api.kanban.domain.KanbanAccessTokenPort;
import org.springframework.stereotype.Component;

/** JPA-Implementierung von {@link KanbanAccessTokenPort}. */
@Component
class JpaKanbanAccessTokenAdapter implements KanbanAccessTokenPort {

  private final KanbanAccessTokenJpaRepository repo;

  JpaKanbanAccessTokenAdapter(KanbanAccessTokenJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public List<KanbanAccessToken> findAllByUser(String userSub) {
    return repo.findAllByUserSubOrderByCreatedAtAsc(userSub).stream()
        .map(JpaKanbanAccessTokenAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<KanbanAccessToken> findById(long id) {
    return repo.findById(id).map(JpaKanbanAccessTokenAdapter::toDomain);
  }

  @Override
  public Optional<KanbanAccessToken> findActiveByHash(String tokenHash) {
    return repo.findByTokenHashAndRevokedFalse(tokenHash)
        .map(JpaKanbanAccessTokenAdapter::toDomain);
  }

  @Override
  public KanbanAccessToken save(KanbanAccessToken token) {
    final KanbanAccessTokenEntity entity;
    if (token.id() == null) {
      entity =
          new KanbanAccessTokenEntity(
              null, token.userSub(), token.displayName(), token.name(), token.tokenHash());
      entity.setLastUsedAt(token.lastUsedAt());
      entity.setRevoked(token.revoked());
    } else {
      entity =
          repo.findById(token.id())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "KanbanAccessToken " + token.id() + " disappeared during save"));
      entity.setLastUsedAt(token.lastUsedAt());
      entity.setRevoked(token.revoked());
    }
    return toDomain(repo.save(entity));
  }

  private static KanbanAccessToken toDomain(KanbanAccessTokenEntity entity) {
    return new KanbanAccessToken(
        entity.getId(),
        entity.getUserSub(),
        entity.getDisplayName(),
        entity.getName(),
        entity.getTokenHash(),
        entity.getCreatedAt(),
        entity.getLastUsedAt(),
        entity.isRevoked());
  }
}
