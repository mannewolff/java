package org.mwolff.api.kanban.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA-Entity zur {@code kanban_settings}-Tabelle. {@code userSub} ist Primärschlüssel. */
@Entity
@Table(name = "kanban_settings")
class KanbanSettingsEntity {

  @Id
  @Column(name = "user_sub", nullable = false, length = 64)
  private String userSub;

  @Column(name = "done_retention_days", nullable = false)
  private int doneRetentionDays;

  protected KanbanSettingsEntity() {
    // JPA
  }

  KanbanSettingsEntity(String userSub, int doneRetentionDays) {
    this.userSub = userSub;
    this.doneRetentionDays = doneRetentionDays;
  }

  String getUserSub() {
    return userSub;
  }

  int getDoneRetentionDays() {
    return doneRetentionDays;
  }

  void setDoneRetentionDays(int doneRetentionDays) {
    this.doneRetentionDays = doneRetentionDays;
  }
}
