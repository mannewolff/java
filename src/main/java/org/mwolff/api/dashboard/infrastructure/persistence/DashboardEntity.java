package org.mwolff.api.dashboard.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** JPA-Entity zur {@code dashboards}-Tabelle. */
@Entity
@Table(name = "dashboards")
class DashboardEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_sub", nullable = false, length = 255)
  private String userSub;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DashboardEntity() {
    // JPA
  }

  DashboardEntity(Long id, String userSub, String name, boolean isDefault) {
    this.id = id;
    this.userSub = userSub;
    this.name = name;
    this.isDefault = isDefault;
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

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  boolean isDefault() {
    return isDefault;
  }

  void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }
}
