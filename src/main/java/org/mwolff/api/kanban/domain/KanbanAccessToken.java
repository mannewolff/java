package org.mwolff.api.kanban.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Langlebiger, widerrufbarer Token, mit dem der 9-Schritt-Workflow (Board-Adapter, {@code tbx}-CLI)
 * die Kanban-API ohne Keycloak-Login anspricht — analog zum Ingest-Token der Zeitreihen. Bewusste
 * Entscheidung: PAT statt Keycloak-Device-Flow, damit {@code offline_access} nicht reaktiviert
 * werden muss (#362/#363).
 *
 * <p>Der Plaintext-Token wird nur einmal beim Anlegen ausgeliefert; in der DB liegt nur der
 * SHA-256-Hash ({@code tokenHash}). Widerruf passiert via Soft-Delete (Flag {@link #revoked}).
 *
 * <p>Zusaetzlich zum Ingest-Token traegt der Kanban-Token einen {@code displayName}: Kommentare,
 * die ueber diesen Token angelegt werden, brauchen einen Autor-Namen (der Filter setzt daraus den
 * {@code preferred_username}-Claim).
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param userSub Keycloak-{@code sub} des Eigentuemers
 * @param displayName Anzeigename des Eigentuemers (Autor fuer token-erzeugte Kommentare)
 * @param name freier Label des Tokens, max 100 Zeichen
 * @param tokenHash SHA-256-Hash des Plaintext-Tokens
 * @param createdAt Zeitpunkt der Anlage
 * @param lastUsedAt Zeitpunkt der letzten Nutzung, {@code null} solange ungenutzt
 * @param revoked {@code true} markiert den Token als deaktiviert
 */
public record KanbanAccessToken(
    Long id,
    String userSub,
    String displayName,
    String name,
    String tokenHash,
    Instant createdAt,
    Instant lastUsedAt,
    boolean revoked) {

  public static final int MAX_NAME_LENGTH = 100;
  public static final int MAX_DISPLAY_NAME_LENGTH = 255;

  public KanbanAccessToken {
    Objects.requireNonNull(userSub, "userSub must not be null");
    if (userSub.isBlank()) {
      throw new IllegalArgumentException("userSub must not be blank");
    }
    Objects.requireNonNull(displayName, "displayName must not be null");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "displayName must be at most " + MAX_DISPLAY_NAME_LENGTH + " chars");
    }
    Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("name must be at most " + MAX_NAME_LENGTH + " chars");
    }
    Objects.requireNonNull(tokenHash, "tokenHash must not be null");
    if (tokenHash.isBlank()) {
      throw new IllegalArgumentException("tokenHash must not be blank");
    }
  }

  /** Erzeugt einen noch nicht persistierten Token. */
  public static KanbanAccessToken newInstance(
      String userSub, String displayName, String name, String tokenHash) {
    return new KanbanAccessToken(null, userSub, displayName, name, tokenHash, null, null, false);
  }

  /** Erzeugt eine Kopie mit gesetztem Widerrufs-Flag. */
  public KanbanAccessToken withRevoked() {
    return new KanbanAccessToken(
        id, userSub, displayName, name, tokenHash, createdAt, lastUsedAt, true);
  }

  /** Erzeugt eine Kopie mit aktualisiertem {@code lastUsedAt}. */
  public KanbanAccessToken withLastUsedAt(Instant now) {
    return new KanbanAccessToken(
        id, userSub, displayName, name, tokenHash, createdAt, now, revoked);
  }
}
