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

  /** Aktive Listen-Filter als CSV; {@code null} bei Legacy-Zeilen ohne gespeicherte Filter. */
  @Column(name = "list_filters", length = 255)
  private String listFilters;

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

  String getListFilters() {
    return listFilters;
  }

  void setListFilters(String listFilters) {
    this.listFilters = listFilters;
  }
}
