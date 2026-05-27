package org.mwolff.api.dashboard.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Dashboard eines Users. Ein User kann mehrere Dashboards haben — höchstens eines davon ist {@code
 * isDefault}.
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param userSub Keycloak-{@code sub} des Eigentümers
 * @param name max 100 Zeichen, nicht leer
 * @param isDefault genau ein Default pro User wird auf Application-Ebene erzwungen
 * @param createdAt Zeitpunkt der Erstanlage
 * @param updatedAt Zeitpunkt der letzten Änderung
 */
public record Dashboard(
    Long id, String userSub, String name, boolean isDefault, Instant createdAt, Instant updatedAt) {

  /** Maximale Länge des Namensfelds — entspricht dem Schema. */
  public static final int MAX_NAME_LENGTH = 100;

  public Dashboard {
    Objects.requireNonNull(userSub, "userSub must not be null");
    if (userSub.isBlank()) {
      throw new IllegalArgumentException("userSub must not be blank");
    }
    Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("name must be at most " + MAX_NAME_LENGTH + " chars");
    }
  }

  /** Erzeugt ein noch nicht persistiertes Dashboard. */
  public static Dashboard newInstance(String userSub, String name, boolean isDefault) {
    return new Dashboard(null, userSub, name, isDefault, null, null);
  }

  /** Erzeugt eine Kopie mit aktualisiertem {@code isDefault}-Flag. */
  public Dashboard withDefault(boolean newDefault) {
    return new Dashboard(id, userSub, name, newDefault, createdAt, updatedAt);
  }

  /** Erzeugt eine Kopie mit aktualisiertem {@code name}. Validierung erfolgt im Konstruktor. */
  public Dashboard withName(String newName) {
    return new Dashboard(id, userSub, newName, isDefault, createdAt, updatedAt);
  }
}
