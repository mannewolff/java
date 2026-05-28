package org.mwolff.api.ingest.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.IngestTokenPort;
import org.springframework.stereotype.Component;

/** JPA-Implementierung von {@link IngestTokenPort}. */
@Component
class JpaIngestTokenAdapter implements IngestTokenPort {

  private final IngestTokenJpaRepository repo;

  JpaIngestTokenAdapter(IngestTokenJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public List<IngestToken> findAllByUser(String userSub) {
    return repo.findAllByUserSubOrderByCreatedAtAsc(userSub).stream()
        .map(JpaIngestTokenAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<IngestToken> findById(long id) {
    return repo.findById(id).map(JpaIngestTokenAdapter::toDomain);
  }

  @Override
  public Optional<IngestToken> findActiveByHash(String tokenHash) {
    return repo.findByTokenHashAndRevokedFalse(tokenHash).map(JpaIngestTokenAdapter::toDomain);
  }

  @Override
  public IngestToken save(IngestToken token) {
    final IngestTokenEntity entity;
    if (token.id() == null) {
      entity = new IngestTokenEntity(null, token.userSub(), token.name(), token.tokenHash());
      entity.setLastUsedAt(token.lastUsedAt());
      entity.setRevoked(token.revoked());
    } else {
      entity =
          repo.findById(token.id())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "IngestToken " + token.id() + " disappeared during save"));
      entity.setLastUsedAt(token.lastUsedAt());
      entity.setRevoked(token.revoked());
    }
    return toDomain(repo.save(entity));
  }

  private static IngestToken toDomain(IngestTokenEntity entity) {
    return new IngestToken(
        entity.getId(),
        entity.getUserSub(),
        entity.getName(),
        entity.getTokenHash(),
        entity.getCreatedAt(),
        entity.getLastUsedAt(),
        entity.isRevoked());
  }
}
