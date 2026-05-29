package org.mwolff.api.kanban.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** JPA-Entity zur {@code kanban_comment}-Tabelle. */
@Entity
@Table(name = "kanban_comment")
class KanbanCommentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "item_id", nullable = false)
  private long itemId;

  @Column(nullable = false, length = 255)
  private String author;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected KanbanCommentEntity() {
    // JPA
  }

  KanbanCommentEntity(long itemId, String author, String body) {
    this.itemId = itemId;
    this.author = author;
    this.body = body;
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

  long getItemId() {
    return itemId;
  }

  String getAuthor() {
    return author;
  }

  String getBody() {
    return body;
  }

  void setBody(String body) {
    this.body = body;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }
}
