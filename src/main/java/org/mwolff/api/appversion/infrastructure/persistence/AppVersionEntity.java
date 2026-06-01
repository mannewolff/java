package org.mwolff.api.appversion.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * JPA-Entity zur {@code app_version}-Tabelle. Es existiert genau eine Zeile mit {@code id = 1}, die
 * die aktuelle Major.Minor-Version haelt.
 */
@Entity
@Table(name = "app_version")
class AppVersionEntity {

  @Id private Long id;

  @Column(nullable = false)
  private int major;

  @Column(nullable = false)
  private int minor;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    final Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  Long getId() {
    return id;
  }

  void setId(final Long id) {
    this.id = id;
  }

  int getMajor() {
    return major;
  }

  void setMajor(final int major) {
    this.major = major;
  }

  int getMinor() {
    return minor;
  }

  void setMinor(final int minor) {
    this.minor = minor;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }
}
