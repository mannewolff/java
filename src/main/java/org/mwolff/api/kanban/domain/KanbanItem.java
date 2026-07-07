package org.mwolff.api.kanban.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
 * <p>{@code shortcode} (#329) ist ein optionales, frei wählbares Kürzel eines Epics (Label). Nur
 * Epics dürfen ein Kürzel tragen; ohne Kürzel wird im Frontend eins aus den Titel-Initialen
 * abgeleitet.
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
 * @param shortcode optionales Epic-Kürzel ({@code null} = keins; nur an Epics erlaubt)
 * @param dependencies Anzeige-Nummern der Einträge, von denen dieser abhängt (#352). Normalisiert:
 *     dedupliziert, aufsteigend sortiert, ohne Werte ≤ 0; nie {@code null}. Existenz- und
 *     Selbstreferenz-Prüfung liegt im Use-Case.
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
    Long parentId,
    String shortcode,
    List<Integer> dependencies) {

  /** Maximale Länge des Titels — entspricht dem Schema. */
  public static final int MAX_TITLE_LENGTH = 200;

  /** Maximale Länge des Bodys — entspricht dem Schema (TEXT-Spalte). */
  public static final int MAX_BODY_LENGTH = 10_000;

  /** Maximale Länge des Epic-Kürzels — entspricht der Schema-Spalte (#329). */
  public static final int MAX_SHORTCODE_LENGTH = 16;

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
    // Leeres Kürzel als „keins" behandeln, sonst trimmen (Normalisierung).
    shortcode = shortcode == null || shortcode.isBlank() ? null : shortcode.trim();
    if (shortcode != null) {
      if (type != KanbanItemType.EPIC) {
        throw new IllegalArgumentException("only an EPIC may have a shortcode");
      }
      if (shortcode.length() > MAX_SHORTCODE_LENGTH) {
        throw new IllegalArgumentException(
            "shortcode must be at most " + MAX_SHORTCODE_LENGTH + " chars");
      }
    }
    // Abhängigkeiten normalisieren: null → leer, ≤0 verwerfen, dedupliziert, aufsteigend.
    // List.copyOf
    // liefert eine von SpotBugs als unveränderlich erkannte Liste (kein EI_EXPOSE_REP am Accessor).
    dependencies =
        dependencies == null
            ? List.of()
            : List.copyOf(
                dependencies.stream().filter(n -> n != null && n > 0).distinct().sorted().toList());
  }

  /**
   * Convenience-Konstruktor mit Kürzel, aber ohne Abhängigkeiten (#329) — hält alle Aufrufstellen
   * vor #352 stabil (Abhängigkeiten default leer).
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
      int number,
      KanbanItemType type,
      Long parentId,
      String shortcode) {
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
        type,
        parentId,
        shortcode,
        List.of());
  }

  /**
   * Convenience-Konstruktor mit Typ + Epic-Zuordnung ohne Kürzel (#321) — hält die Aufrufstellen
   * aus #321/#322 stabil.
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
      int number,
      KanbanItemType type,
      Long parentId) {
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
        type,
        parentId,
        null,
        List.of());
  }

  /**
   * Komfort-Konstruktor für normale Items ohne Epic-Zuordnung ({@code type=ITEM}, {@code
   * parentId=null}, kein Kürzel) — hält die vor #321 entstandenen Aufrufstellen stabil.
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
        null,
        null);
  }

  /**
   * Erzeugt ein noch nicht persistiertes Item ({@code type=ITEM}, kein Epic), am Ende der
   * Zielspalte (position wird Use-Case-seitig gesetzt).
   */
  public static KanbanItem newInstance(
      String userSub, String title, String body, KanbanColumn column, int position, Instant now) {
    return newInstance(
        userSub, title, body, column, position, now, KanbanItemType.ITEM, null, null);
  }

  /**
   * Erzeugt ein noch nicht persistiertes Item mit explizitem Typ und optionaler Epic-Zuordnung ohne
   * Kürzel (#321/#322).
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
    return newInstance(userSub, title, body, column, position, now, type, parentId, null);
  }

  /**
   * Erzeugt ein noch nicht persistiertes Item mit Typ, optionaler Epic-Zuordnung und optionalem
   * Epic-Kürzel (#329). Die Existenz-/Typ-Prüfung des Parents liegt im Create-Use-Case.
   */
  public static KanbanItem newInstance(
      String userSub,
      String title,
      String body,
      KanbanColumn column,
      int position,
      Instant now,
      KanbanItemType type,
      Long parentId,
      String shortcode) {
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
        parentId,
        shortcode);
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
        parentId,
        shortcode,
        dependencies);
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
        parentId,
        shortcode,
        dependencies);
  }

  /**
   * Kopie mit neuem Titel, Body und Kürzel (#329) — für das Bearbeiten eines Epics. Alle anderen
   * Felder bleiben unverändert.
   */
  public KanbanItem withContent(String newTitle, String newBody, String newShortcode) {
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
        parentId,
        newShortcode,
        dependencies);
  }

  /**
   * Kopie mit neuem Titel, Body, Kürzel und Epic-Zuordnung (#339) — für das Bearbeiten eines Items
   * inklusive nachträglicher Epic-Zuordnung ({@code parentId = null} entfernt die Zuordnung). Die
   * Existenz-/Typ-/Owner-Prüfung des Parents liegt im Update-Use-Case. Alle anderen Felder bleiben
   * unverändert.
   */
  public KanbanItem withContent(
      String newTitle, String newBody, String newShortcode, Long newParentId) {
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
        newParentId,
        newShortcode,
        dependencies);
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
        parentId,
        shortcode,
        dependencies);
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
        parentId,
        shortcode,
        dependencies);
  }

  /** Kopie mit neuen Abhängigkeiten (#352). Alle anderen Felder bleiben unverändert. */
  public KanbanItem withDependencies(List<Integer> newDependencies) {
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
        number,
        type,
        parentId,
        shortcode,
        newDependencies);
  }

  /**
   * Serialisiert die Abhängigkeiten als CSV der Nummern (für die Persistenz), z. B. {@code
   * "12,34"}.
   */
  public static String dependenciesToCsv(List<Integer> dependencies) {
    return dependencies.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  /**
   * Parst die persistierte CSV zurück in eine normalisierte Nummern-Liste. {@code null}/leer →
   * leer.
   */
  public static List<Integer> dependenciesFromCsv(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Integer::valueOf)
        .filter(n -> n > 0)
        .distinct()
        .sorted()
        .toList();
  }
}
