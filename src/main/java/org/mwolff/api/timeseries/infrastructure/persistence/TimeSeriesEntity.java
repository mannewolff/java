package org.mwolff.api.timeseries.infrastructure.persistence;

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

import org.mwolff.api.timeseries.domain.TimeSeriesDataType;

/** JPA-Entity zur {@code time_series}-Tabelle. */
@Entity
@Table(name = "time_series")
class TimeSeriesEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_sub", nullable = false, length = 64)
  private String userSub;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, length = 50)
  private String unit;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_type", nullable = false, length = 20)
  private TimeSeriesDataType dataType;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TimeSeriesEntity() {
    // JPA
  }

  TimeSeriesEntity(
      Long id,
      String userSub,
      String name,
      String description,
      String unit,
      TimeSeriesDataType dataType) {
    this.id = id;
    this.userSub = userSub;
    this.name = name;
    this.description = description;
    this.unit = unit;
    this.dataType = dataType;
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

  String getDescription() {
    return description;
  }

  void setDescription(String description) {
    this.description = description;
  }

  String getUnit() {
    return unit;
  }

  void setUnit(String unit) {
    this.unit = unit;
  }

  TimeSeriesDataType getDataType() {
    return dataType;
  }

  void setDataType(TimeSeriesDataType dataType) {
    this.dataType = dataType;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }
}
