package org.mwolff.api.kanban.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Ein Kanban-Item gehört genau einem User und liegt zu jedem Zeitpunkt in genau einer Spalte. Die
 * {@code position} ist 0-basiert und innerhalb einer Spalte lückenlos sortiert.
 *
 * <p>{@code movedToDoneAt} wird nur gesetzt, wenn die aktuelle Spalte {@link KanbanColumn#DONE}
 * ist. Beim Verlassen von DONE wird der Zeitpunkt zurückgesetzt. Das Feld ist Basis für den
 * automatischen Cleanup.
 *
 * <p>Epics (#321): {@code type} unterscheidet normale Items von Epics. Ein Item kann über {@code
 * parentId} genau einem Epic zugeordnet sein; ein Epic selbst darf keinen Parent haben. Epics
 * nehmen nicht am Spalten-Workflow teil — sie erscheinen nicht auf dem Board und werden nicht
 * verschoben (Guard im Move-Use-Case; die Prüfung „parent ist ein EPIC" liegt ebenfalls im
 * Use-Case, da sie andere Items braucht).
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param userSub Keycloak-{@code sub} des Eigentümers
 * @param title pflicht, max 200 Zeichen, nicht leer
 * @param body Markdown-Text, max 10_000 Zeichen, darf leer sein
 * @param column Spalte
 * @param position 0-basierte Sortierreihenfolge innerhalb der Spalte
 * @param createdAt Erstanlage
 * @param updatedAt letzte Änderung
 * @param movedToDoneAt Zeitpunkt des letzten Wechsels nach DONE (null außerhalb DONE)
 * @param archived {@code true} = archiviert (Soft-Delete), wird standardmäßig nicht angezeigt
 * @param number fortlaufende, pro User eindeutige Anzeige-Nummer (#187)
 * @param type {@link KanbanItemType#ITEM} (Board-Karte) oder {@link KanbanItemType#EPIC}
 * @param parentId ID des zugeordneten Epics ({@code null} = keinem Epic zugeordnet)
 */
public record KanbanItem(
    Long id,
    String userSub,
    String title,
    String body,
    KanbanColumn column,
    int position,
    Instant createdAt,
    Instant updatedAt,
    Instant movedToDoneAt,
    boolean archived,
    int number,
    KanbanItemType type,
    Long parentId) {

  /** Maximale Länge des Titels — entspricht dem Schema. */
  public static final int MAX_TITLE_LENGTH = 200;

  /** Maximale Länge des Bodys — entspricht dem Schema (TEXT-Spalte). */
  public static final int MAX_BODY_LENGTH = 10_000;

  public KanbanItem {
    Objects.requireNonNull(userSub, "userSub must not be null");
    if (userSub.isBlank()) {
      throw new IllegalArgumentException("userSub must not be blank");
    }
    Objects.requireNonNull(title, "title must not be null");
    if (title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (title.length() > MAX_TITLE_LENGTH) {
      throw new IllegalArgumentException("title must be at most " + MAX_TITLE_LENGTH + " chars");
    }
    Objects.requireNonNull(body, "body must not be null");
    if (body.length() > MAX_BODY_LENGTH) {
      throw new IllegalArgumentException("body must be at most " + MAX_BODY_LENGTH + " chars");
    }
    Objects.requireNonNull(column, "column must not be null");
    if (position < 0) {
      throw new IllegalArgumentException("position must be >= 0");
    }
    if (column != KanbanColumn.DONE && movedToDoneAt != null) {
      throw new IllegalArgumentException("movedToDoneAt must be null outside DONE");
    }
    if (number < 0) {
      throw new IllegalArgumentException("number must be >= 0");
    }
    Objects.requireNonNull(type, "type must not be null");
    if (type == KanbanItemType.EPIC && parentId != null) {
      throw new IllegalArgumentException("an EPIC must not have a parent");
    }
  }

  /**
   * Komfort-Konstruktor für normale Items ohne Epic-Zuordnung ({@code type=ITEM}, {@code
   * parentId=null}) — hält die vor #321 entstandenen Aufrufstellen stabil.
   */
  public KanbanItem(
      Long id,
      String userSub,
      String title,
      String body,
      KanbanColumn column,
      int position,
      Instant createdAt,
      Instant updatedAt,
      Instant movedToDoneAt,
      boolean archived,
      int number) {
    this(
        id,
        userSub,
        title,
        body,
        column,
        position,
        createdAt,
        updatedAt,
        movedToDoneAt,
        archived,
        number,
        KanbanItemType.ITEM,
        null);
  }

  /**
   * Erzeugt ein noch nicht persistiertes Item ({@code type=ITEM}, kein Epic), am Ende der
   * Zielspalte (position wird Use-Case-seitig gesetzt).
   */
  public static KanbanItem newInstance(
      String userSub, String title, String body, KanbanColumn column, int position, Instant now) {
    return newInstance(userSub, title, body, column, position, now, KanbanItemType.ITEM, null);
  }

  /**
   * Erzeugt ein noch nicht persistiertes Item mit explizitem Typ und optionaler Epic-Zuordnung
   * (#321). Die Existenz-/Typ-Prüfung des Parents liegt im Create-Use-Case.
   */
  public static KanbanItem newInstance(
      String userSub,
      String title,
      String body,
      KanbanColumn column,
      int position,
      Instant now,
      KanbanItemType type,
      Long parentId) {
    final Instant movedToDone = column == KanbanColumn.DONE ? now : null;
    // number = 0: noch nicht vergeben; der Create-Use-Case setzt sie via withNumber (#187).
    return new KanbanItem(
        null,
        userSub,
        title,
        body,
        column,
        position,
        null,
        null,
        movedToDone,
        false,
        0,
        type,
        parentId);
  }

  /** Kopie mit gesetzter Anzeige-Nummer (#187). Alle anderen Felder bleiben unverändert. */
  public KanbanItem withNumber(int newNumber) {
    return new KanbanItem(
        id,
        userSub,
        title,
        body,
        column,
        position,
        createdAt,
        updatedAt,
        movedToDoneAt,
        archived,
        newNumber,
        type,
        parentId);
  }

  /** Kopie mit neuem Titel und neuem Body. Alle anderen Felder bleiben unverändert. */
  public KanbanItem withContent(String newTitle, String newBody) {
    return new KanbanItem(
        id,
        userSub,
        newTitle,
        newBody,
        column,
        position,
        createdAt,
        updatedAt,
        movedToDoneAt,
        archived,
        number,
        type,
        parentId);
  }

  /**
   * Kopie mit neuer Spalte und Position. Setzt {@code movedToDoneAt} korrekt: ans aktuelle "now"
   * beim Eintritt in DONE, auf {@code null} beim Verlassen, sonst unverändert.
   */
  public KanbanItem withColumnAndPosition(KanbanColumn newColumn, int newPosition, Instant now) {
    final boolean enteringDone = newColumn == KanbanColumn.DONE && column != KanbanColumn.DONE;
    final boolean leavingDone = column == KanbanColumn.DONE && newColumn != KanbanColumn.DONE;
    final Instant nextMovedToDone = enteringDone ? now : leavingDone ? null : movedToDoneAt;
    return new KanbanItem(
        id,
        userSub,
        title,
        body,
        newColumn,
        newPosition,
        createdAt,
        updatedAt,
        nextMovedToDone,
        archived,
        number,
        type,
        parentId);
  }

  /** Kopie mit neuer Position (gleiche Spalte). */
  public KanbanItem withPosition(int newPosition) {
    return new KanbanItem(
        id,
        userSub,
        title,
        body,
        column,
        newPosition,
        createdAt,
        updatedAt,
        movedToDoneAt,
        archived,
        number,
        type,
        parentId);
  }
}
