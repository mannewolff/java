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
 * @param backgroundColor optionaler CSS-Farbwert für den Hintergrund; {@code null} = Theme-Default
 * @param createdAt Zeitpunkt der Erstanlage
 * @param updatedAt Zeitpunkt der letzten Änderung
 */
public record Dashboard(
    Long id,
    String userSub,
    String name,
    boolean isDefault,
    String backgroundColor,
    Instant createdAt,
    Instant updatedAt) {

  /** Maximale Länge des Namensfelds — entspricht dem Schema. */
  public static final int MAX_NAME_LENGTH = 100;

  /** Maximale Länge des Hintergrundfarb-Felds — entspricht dem Schema. */
  public static final int MAX_BACKGROUND_COLOR_LENGTH = 64;

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
    // Leerwert == kein Override: auf null normalisieren, damit das Frontend "" senden darf.
    if (backgroundColor != null && backgroundColor.isBlank()) {
      backgroundColor = null;
    }
    if (backgroundColor != null && backgroundColor.length() > MAX_BACKGROUND_COLOR_LENGTH) {
      throw new IllegalArgumentException(
          "backgroundColor must be at most " + MAX_BACKGROUND_COLOR_LENGTH + " chars");
    }
  }

  /** Erzeugt ein noch nicht persistiertes Dashboard. */
  public static Dashboard newInstance(String userSub, String name, boolean isDefault) {
    return new Dashboard(null, userSub, name, isDefault, null, null, null);
  }

  /** Erzeugt eine Kopie mit aktualisiertem {@code isDefault}-Flag. */
  public Dashboard withDefault(boolean newDefault) {
    return new Dashboard(id, userSub, name, newDefault, backgroundColor, createdAt, updatedAt);
  }

  /** Erzeugt eine Kopie mit aktualisiertem {@code name}. Validierung erfolgt im Konstruktor. */
  public Dashboard withName(String newName) {
    return new Dashboard(id, userSub, newName, isDefault, backgroundColor, createdAt, updatedAt);
  }

  /**
   * Erzeugt eine Kopie mit aktualisierter {@code backgroundColor}. Leerwert wird zu {@code null}.
   */
  public Dashboard withBackgroundColor(String newBackgroundColor) {
    return new Dashboard(id, userSub, name, isDefault, newBackgroundColor, createdAt, updatedAt);
  }
}
