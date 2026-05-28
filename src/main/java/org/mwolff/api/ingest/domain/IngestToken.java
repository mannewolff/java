package org.mwolff.api.ingest.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Langlebiger Token, mit dem externe Programme (IoT-Sensoren, Skripte) Werte in eine Zeitreihe
 * pushen koennen — ohne JWT, weil JWT zu kurze Lebensdauer hat.
 *
 * <p>Der Plaintext-Token wird nur einmal beim Anlegen ausgeliefert; in der DB liegt nur der Hash
 * ({@code tokenHash}, bcrypt). Widerruf passiert via Soft-Delete (Flag {@link #revoked}).
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param userSub Keycloak-{@code sub} des Eigentuemers
 * @param name freier Label des Tokens, max 100 Zeichen
 * @param tokenHash bcrypt-Hash des Plaintext-Tokens
 * @param createdAt Zeitpunkt der Anlage
 * @param lastUsedAt Zeitpunkt der letzten Nutzung, {@code null} solange ungenutzt
 * @param revoked {@code true} markiert den Token als deaktiviert
 */
public record IngestToken(
    Long id,
    String userSub,
    String name,
    String tokenHash,
    Instant createdAt,
    Instant lastUsedAt,
    boolean revoked) {

  public static final int MAX_NAME_LENGTH = 100;

  public IngestToken {
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
    Objects.requireNonNull(tokenHash, "tokenHash must not be null");
    if (tokenHash.isBlank()) {
      throw new IllegalArgumentException("tokenHash must not be blank");
    }
  }

  /** Erzeugt einen noch nicht persistierten Token. */
  public static IngestToken newInstance(String userSub, String name, String tokenHash) {
    return new IngestToken(null, userSub, name, tokenHash, null, null, false);
  }

  /** Erzeugt eine Kopie mit gesetztem Widerrufs-Flag. */
  public IngestToken withRevoked() {
    return new IngestToken(id, userSub, name, tokenHash, createdAt, lastUsedAt, true);
  }

  /** Erzeugt eine Kopie mit aktualisiertem {@code lastUsedAt}. */
  public IngestToken withLastUsedAt(Instant now) {
    return new IngestToken(id, userSub, name, tokenHash, createdAt, now, revoked);
  }
}
