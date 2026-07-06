package org.mwolff.api.kanban.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanSettings;
import org.mwolff.api.kanban.domain.KanbanSettingsPort;
import org.springframework.stereotype.Component;

/** JPA-Implementierung von {@link KanbanItemPort} und {@link KanbanSettingsPort}. */
@Component
class JpaKanbanAdapter implements KanbanItemPort, KanbanSettingsPort {

  private final KanbanItemJpaRepository itemRepo;
  private final KanbanSettingsJpaRepository settingsRepo;

  JpaKanbanAdapter(KanbanItemJpaRepository itemRepo, KanbanSettingsJpaRepository settingsRepo) {
    this.itemRepo = itemRepo;
    this.settingsRepo = settingsRepo;
  }

  // ----- KanbanItemPort ----------------------------------------------------

  @Override
  public List<KanbanItem> findAllByUser(String userSub) {
    return itemRepo.findActiveByUserSub(userSub).stream().map(JpaKanbanAdapter::toDomain).toList();
  }

  @Override
  public List<KanbanItem> findByUserAndColumn(String userSub, KanbanColumn column) {
    return itemRepo.findActiveByUserSubAndColumn(userSub, column).stream()
        .map(JpaKanbanAdapter::toDomain)
        .toList();
  }

  @Override
  public List<KanbanItem> findAllByUserIncludingArchived(String userSub) {
    return itemRepo.findAllByUserSubIncludingArchived(userSub).stream()
        .map(JpaKanbanAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<KanbanItem> findById(long id) {
    return itemRepo.findById(id).map(JpaKanbanAdapter::toDomain);
  }

  @Override
  public KanbanItem save(KanbanItem item) {
    final KanbanItemEntity entity;
    if (item.id() == null) {
      entity =
          new KanbanItemEntity(
              item.userSub(),
              item.title(),
              item.body(),
              item.type(),
              item.parentId(),
              item.column(),
              item.position(),
              item.movedToDoneAt());
      // Anzeige-Nummer (#187) wird vom Use-Case berechnet und nur beim Neuanlegen geschrieben.
      entity.setNumber(item.number());
    } else {
      entity =
          itemRepo
              .findById(item.id())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Kanban item " + item.id() + " disappeared during save"));
      entity.setTitle(item.title());
      entity.setBody(item.body());
      entity.setColumnName(item.column());
      entity.setPositionInColumn(item.position());
      entity.setMovedToDoneAt(item.movedToDoneAt());
      entity.setArchived(item.archived());
      // type bleibt nach dem Anlegen fix; die Epic-Zuordnung darf sich ändern (#321).
      entity.setParentId(item.parentId());
    }
    return toDomain(itemRepo.save(entity));
  }

  @Override
  public Optional<Integer> getMaxNumberForUser(String userSub) {
    return itemRepo.findMaxNumberByUserSub(userSub);
  }

  @Override
  public void updatePosition(long id, int newPosition) {
    itemRepo.updatePosition(id, newPosition);
  }

  @Override
  public void deleteById(long id) {
    itemRepo.deleteById(id);
  }

  @Override
  public void archiveById(long id) {
    itemRepo.archiveById(id);
  }

  @Override
  public void restoreById(long id) {
    itemRepo.restoreById(id);
  }

  @Override
  public int deleteDoneOlderThan(String userSub, Instant threshold) {
    return itemRepo.deleteDoneOlderThan(userSub, threshold);
  }

  @Override
  public List<String> distinctUsersWithDoneItems() {
    return itemRepo.distinctUsersWithDoneItems();
  }

  // ----- KanbanSettingsPort ------------------------------------------------

  @Override
  public Optional<KanbanSettings> findByUser(String userSub) {
    return settingsRepo.findById(userSub).map(JpaKanbanAdapter::toDomain);
  }

  @Override
  public KanbanSettings save(KanbanSettings settings) {
    final KanbanSettingsEntity entity =
        settingsRepo
            .findById(settings.userSub())
            .orElseGet(
                () -> new KanbanSettingsEntity(settings.userSub(), settings.doneRetentionDays()));
    entity.setDoneRetentionDays(settings.doneRetentionDays());
    return toDomain(settingsRepo.save(entity));
  }

  // ----- Mapping -----------------------------------------------------------

  private static KanbanItem toDomain(KanbanItemEntity entity) {
    return new KanbanItem(
        entity.getId(),
        entity.getUserSub(),
        entity.getTitle(),
        entity.getBody(),
        entity.getColumnName(),
        entity.getPositionInColumn(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getMovedToDoneAt(),
        entity.isArchived(),
        entity.getNumber(),
        entity.getItemType(),
        entity.getParentId());
  }

  private static KanbanSettings toDomain(KanbanSettingsEntity entity) {
    return new KanbanSettings(entity.getUserSub(), entity.getDoneRetentionDays());
  }
}
