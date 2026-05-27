package org.mwolff.api.kanban.domain;

import java.util.Objects;

/**
 * Settings eines Users für sein Kanban-Board.
 *
 * @param userSub Keycloak-{@code sub} des Eigentümers
 * @param doneRetentionDays Anzahl Tage, nach denen Items in der DONE-Spalte automatisch gelöscht
 *     werden. Range 1..30, Default 5 (siehe {@link #DEFAULT_RETENTION_DAYS}).
 */
public record KanbanSettings(String userSub, int doneRetentionDays) {

  /** Default-Retention falls für den User noch nichts gespeichert ist. */
  public static final int DEFAULT_RETENTION_DAYS = 5;

  /** Minimaler Wert für {@code doneRetentionDays}. */
  public static final int MIN_RETENTION_DAYS = 1;

  /** Maximaler Wert für {@code doneRetentionDays}. */
  public static final int MAX_RETENTION_DAYS = 30;

  public KanbanSettings {
    Objects.requireNonNull(userSub, "userSub must not be null");
    if (userSub.isBlank()) {
      throw new IllegalArgumentException("userSub must not be blank");
    }
    if (doneRetentionDays < MIN_RETENTION_DAYS || doneRetentionDays > MAX_RETENTION_DAYS) {
      throw new IllegalArgumentException(
          "doneRetentionDays must be in "
              + MIN_RETENTION_DAYS
              + ".."
              + MAX_RETENTION_DAYS);
    }
  }

  /** Default-Settings für einen User, falls noch nichts in der DB liegt. */
  public static KanbanSettings defaultFor(String userSub) {
    return new KanbanSettings(userSub, DEFAULT_RETENTION_DAYS);
  }
}
