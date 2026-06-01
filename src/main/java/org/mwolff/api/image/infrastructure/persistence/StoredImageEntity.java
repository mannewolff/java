package org.mwolff.api.image.infrastructure.persistence;

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

/** JPA-Entity für {@code stored_image} (#181). Nur via Testcontainers-IT sinnvoll testbar. */
@Entity
@Table(name = "stored_image")
class StoredImageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "content_type", nullable = false, length = 64)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private int sizeBytes;

  // LONGVARBINARY passt zur LONGBLOB-Spalte (V15). Ohne expliziten JdbcTypeCode mappt Hibernate 6
  // byte[]/@Lob auf MariaDB auf einen kleineren BLOB-Typ → Schema-Validierung schlägt fehl.
  @JdbcTypeCode(SqlTypes.LONGVARBINARY)
  @Column(name = "data", nullable = false, columnDefinition = "LONGBLOB")
  private byte[] data;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  Long getId() {
    return id;
  }

  void setId(final Long id) {
    this.id = id;
  }

  String getContentType() {
    return contentType;
  }

  void setContentType(final String contentType) {
    this.contentType = contentType;
  }

  int getSizeBytes() {
    return sizeBytes;
  }

  void setSizeBytes(final int sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  byte[] getData() {
    return data;
  }

  void setData(final byte[] data) {
    this.data = data;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(final Instant createdAt) {
    this.createdAt = createdAt;
  }
}
