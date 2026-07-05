package org.mwolff.api.kanban.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Ein freier Kommentar an genau einem {@link KanbanItem}. Die Identität des Verfassers ist der
 * stabile Keycloak-{@code sub} ({@code authorSub}) — er wird beim Bearbeiten/Löschen gegen den
 * aktuellen User geprüft (fremder Kommentar → 403). Der {@code author} ist nur der Anzeigename
 * ({@code preferred_username}) und darf sich ändern, ohne das Eigentum zu berühren.
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param itemId ID des Kanban-Items, an dem der Kommentar hängt
 * @param authorSub stabile Identität des Verfassers (Keycloak-{@code sub})
 * @param author Anzeigename des Verfassers ({@code preferred_username})
 * @param body Kommentartext, nicht leer, max 10_000 Zeichen
 * @param createdAt Erstanlage
 * @param updatedAt letzte Änderung
 */
public record KanbanComment(
    Long id,
    long itemId,
    String authorSub,
    String author,
    String body,
    Instant createdAt,
    Instant updatedAt) {

  /** Maximale Länge des Kommentartexts — entspricht dem Schema (TEXT-Spalte). */
  public static final int MAX_BODY_LENGTH = 10_000;

  public KanbanComment {
    Objects.requireNonNull(authorSub, "authorSub must not be null");
    if (authorSub.isBlank()) {
      throw new IllegalArgumentException("authorSub must not be blank");
    }
    Objects.requireNonNull(author, "author must not be null");
    if (author.isBlank()) {
      throw new IllegalArgumentException("author must not be blank");
    }
    Objects.requireNonNull(body, "body must not be null");
    if (body.isBlank()) {
      throw new IllegalArgumentException("body must not be blank");
    }
    if (body.length() > MAX_BODY_LENGTH) {
      throw new IllegalArgumentException("body must be at most " + MAX_BODY_LENGTH + " chars");
    }
  }

  /** Erzeugt einen noch nicht persistierten Kommentar. */
  public static KanbanComment newInstance(
      long itemId, String authorSub, String author, String body) {
    return new KanbanComment(null, itemId, authorSub, author, body, null, null);
  }

  /** Prüft, ob der übergebene {@code sub} der Verfasser dieses Kommentars ist. */
  public boolean isOwnedBy(String userSub) {
    return authorSub.equals(userSub);
  }

  /** Kopie mit geändertem Body; Autor und Zeitstempel-Logik bleiben unverändert. */
  public KanbanComment withBody(String newBody) {
    return new KanbanComment(id, itemId, authorSub, author, newBody, createdAt, updatedAt);
  }
}
