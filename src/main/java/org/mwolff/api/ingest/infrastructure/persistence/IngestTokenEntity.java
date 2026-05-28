package org.mwolff.api.ingest.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** JPA-Entity zur {@code ingest_token}-Tabelle. */
@Entity
@Table(name = "ingest_token")
class IngestTokenEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_sub", nullable = false, length = 64)
  private String userSub;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "token_hash", nullable = false, length = 100, unique = true)
  private String tokenHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(nullable = false)
  private boolean revoked;

  protected IngestTokenEntity() {
    // JPA
  }

  IngestTokenEntity(Long id, String userSub, String name, String tokenHash) {
    this.id = id;
    this.userSub = userSub;
    this.name = name;
    this.tokenHash = tokenHash;
    this.revoked = false;
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

  String getUserSub() {
    return userSub;
  }

  String getName() {
    return name;
  }

  String getTokenHash() {
    return tokenHash;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getLastUsedAt() {
    return lastUsedAt;
  }

  void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  boolean isRevoked() {
    return revoked;
  }

  void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }
}
