package org.mwolff.api.timeseries.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA-Entity zur {@code time_series_entry}-Tabelle. */
@Entity
@Table(name = "time_series_entry")
class TimeSeriesEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "time_series_id", nullable = false)
  private Long timeSeriesId;

  @Column(name = "timestamp_value", nullable = false)
  private Instant timestamp;

  @Column(name = "numeric_value", nullable = false, precision = 20, scale = 6)
  private BigDecimal value;

  protected TimeSeriesEntryEntity() {
    // JPA
  }

  TimeSeriesEntryEntity(Long timeSeriesId, Instant timestamp, BigDecimal value) {
    this.timeSeriesId = timeSeriesId;
    this.timestamp = timestamp;
    this.value = value;
  }

  Long getId() {
    return id;
  }

  Long getTimeSeriesId() {
    return timeSeriesId;
  }

  Instant getTimestamp() {
    return timestamp;
  }

  BigDecimal getValue() {
    return value;
  }
}
