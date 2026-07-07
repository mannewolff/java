package org.mwolff.api.kanban.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItemType;

/** JPA-Entity zur {@code kanban_item}-Tabelle. */
@Entity
@Table(name = "kanban_item")
class KanbanItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_sub", nullable = false, length = 64)
  private String userSub;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  // Feldname itemType statt type: vermeidet Kollision mit der HQL-Funktion TYPE() in Queries.
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private KanbanItemType itemType;

  @Column(name = "parent_id")
  private Long parentId;

  @Column(name = "shortcode", length = 16)
  private String shortcode;

  /** Abhängigkeiten als CSV von Anzeige-Nummern (#352); {@code null} = keine. */
  @Column(name = "dependencies", length = 255)
  private String dependencies;

  @Enumerated(EnumType.STRING)
  @Column(name = "column_name", nullable = false, length = 20)
  private KanbanColumn columnName;

  @Column(name = "position_in_column", nullable = false)
  private int positionInColumn;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "moved_to_done_at")
  private Instant movedToDoneAt;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "number", nullable = false)
  private int number;

  protected KanbanItemEntity() {
    // JPA
  }

  KanbanItemEntity(
      String userSub,
      String title,
      String body,
      KanbanItemType itemType,
      Long parentId,
      String shortcode,
      KanbanColumn columnName,
      int positionInColumn,
      Instant movedToDoneAt) {
    this.userSub = userSub;
    this.title = title;
    this.body = body;
    this.itemType = itemType;
    this.parentId = parentId;
    this.shortcode = shortcode;
    this.columnName = columnName;
    this.positionInColumn = positionInColumn;
    this.movedToDoneAt = movedToDoneAt;
    this.archived = false;
  }

  @PrePersist
  void onCreate() {
    final Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }

  Long getId() {
    return id;
  }

  String getUserSub() {
    return userSub;
  }

  String getTitle() {
    return title;
  }

  void setTitle(String title) {
    this.title = title;
  }

  String getBody() {
    return body;
  }

  void setBody(String body) {
    this.body = body;
  }

  KanbanItemType getItemType() {
    return itemType;
  }

  Long getParentId() {
    return parentId;
  }

  void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  String getShortcode() {
    return shortcode;
  }

  void setShortcode(String shortcode) {
    this.shortcode = shortcode;
  }

  String getDependencies() {
    return dependencies;
  }

  void setDependencies(String dependencies) {
    this.dependencies = dependencies;
  }

  KanbanColumn getColumnName() {
    return columnName;
  }

  void setColumnName(KanbanColumn columnName) {
    this.columnName = columnName;
  }

  int getPositionInColumn() {
    return positionInColumn;
  }

  void setPositionInColumn(int positionInColumn) {
    this.positionInColumn = positionInColumn;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  Instant getMovedToDoneAt() {
    return movedToDoneAt;
  }

  void setMovedToDoneAt(Instant movedToDoneAt) {
    this.movedToDoneAt = movedToDoneAt;
  }

  boolean isArchived() {
    return archived;
  }

  void setArchived(boolean archived) {
    this.archived = archived;
  }

  int getNumber() {
    return number;
  }

  void setNumber(int number) {
    this.number = number;
  }
}
