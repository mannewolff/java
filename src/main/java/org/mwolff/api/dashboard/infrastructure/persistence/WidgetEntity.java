package org.mwolff.api.dashboard.infrastructure.persistence;

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

import org.mwolff.api.dashboard.domain.WidgetType;

/** JPA-Entity zur {@code widgets}-Tabelle. */
@Entity
@Table(name = "widgets")
class WidgetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "dashboard_id", nullable = false)
  private Long dashboardId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private WidgetType type;

  @Column(name = "pos_x", nullable = false)
  private int posX;

  @Column(name = "pos_y", nullable = false)
  private int posY;

  @Column(nullable = false)
  private int width;

  @Column(nullable = false)
  private int height;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String config;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected WidgetEntity() {
    // JPA
  }

  WidgetEntity(
      Long dashboardId, WidgetType type, int posX, int posY, int width, int height, String config) {
    this.dashboardId = dashboardId;
    this.type = type;
    this.posX = posX;
    this.posY = posY;
    this.width = width;
    this.height = height;
    this.config = config;
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

  Long getDashboardId() {
    return dashboardId;
  }

  WidgetType getType() {
    return type;
  }

  int getPosX() {
    return posX;
  }

  int getPosY() {
    return posY;
  }

  int getWidth() {
    return width;
  }

  int getHeight() {
    return height;
  }

  String getConfig() {
    return config;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }
}
