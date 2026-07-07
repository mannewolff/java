package org.mwolff.api.kanban.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** JPA-Entity zur {@code kanban_attachment}-Tabelle (#349). Nur via Testcontainers-IT sinnvoll. */
@Entity
@Table(name = "kanban_attachment")
class KanbanAttachmentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "item_id", nullable = false)
  private long itemId;

  @Column(name = "filename", nullable = false, length = 255)
  private String filename;

  @Column(name = "content_type", nullable = false, length = 128)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private int sizeBytes;

  // LONGVARBINARY passt zur LONGBLOB-Spalte. Ohne expliziten JdbcTypeCode mappt Hibernate 6
  // byte[] auf MariaDB auf einen kleineren BLOB-Typ → Schema-Validierung schlägt fehl.
  @JdbcTypeCode(SqlTypes.LONGVARBINARY)
  @Column(name = "data", nullable = false, columnDefinition = "LONGBLOB")
  private byte[] data;

  @Column(name = "sha256_hash", length = 64)
  private String hash;

  @Column(name = "uploaded_by_sub", nullable = false, length = 64)
  private String uploadedBySub;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected KanbanAttachmentEntity() {
    // JPA
  }

  KanbanAttachmentEntity(
      long itemId,
      String filename,
      String contentType,
      int sizeBytes,
      byte[] data,
      String hash,
      String uploadedBySub) {
    this.itemId = itemId;
    this.filename = filename;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.data = data;
    this.hash = hash;
    this.uploadedBySub = uploadedBySub;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  Long getId() {
    return id;
  }

  long getItemId() {
    return itemId;
  }

  String getFilename() {
    return filename;
  }

  String getContentType() {
    return contentType;
  }

  int getSizeBytes() {
    return sizeBytes;
  }

  byte[] getData() {
    return data;
  }

  String getHash() {
    return hash;
  }

  String getUploadedBySub() {
    return uploadedBySub;
  }

  Instant getCreatedAt() {
    return createdAt;
  }
}
