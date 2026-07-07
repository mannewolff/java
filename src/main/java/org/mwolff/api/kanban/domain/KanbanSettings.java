package org.mwolff.api.kanban.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Settings eines Users für sein Kanban-Board.
 *
 * @param userSub Keycloak-{@code sub} des Eigentümers
 * @param doneRetentionDays Anzahl Tage, nach denen Items in der DONE-Spalte automatisch gelöscht
 *     werden. Range 1..30, Default 5 (siehe {@link #DEFAULT_RETENTION_DAYS}).
 * @param activeFilters In der Listen-Ansicht aktive Filter-Keys (Spalten-Namen + {@link
 *     #ARCHIVED_FILTER}). Nie {@code null}; leere Menge = "nichts anzeigen".
 */
public record KanbanSettings(String userSub, int doneRetentionDays, Set<String> activeFilters) {

  /** Default-Retention falls für den User noch nichts gespeichert ist. */
  public static final int DEFAULT_RETENTION_DAYS = 5;

  /** Minimaler Wert für {@code doneRetentionDays}. */
  public static final int MIN_RETENTION_DAYS = 1;

  /** Maximaler Wert für {@code doneRetentionDays}. */
  public static final int MAX_RETENTION_DAYS = 30;

  /** Pseudo-Filter für archivierte Items (ist kein {@link KanbanColumn}). */
  public static final String ARCHIVED_FILTER = "archived";

  /**
   * Alle erlaubten Filter-Keys in kanonischer Reihenfolge: Spalten (Enum-Reihenfolge), dann Archiv.
   */
  private static final List<String> FILTER_ORDER =
      Stream.concat(
              Arrays.stream(KanbanColumn.values()).map(Enum::name), Stream.of(ARCHIVED_FILTER))
          .toList();

  private static final Set<String> VALID_FILTER_KEYS = Set.copyOf(FILTER_ORDER);

  /** Default-Filter: alle Spalten aktiv, Archiv aus. */
  public static final Set<String> DEFAULT_FILTERS =
      Arrays.stream(KanbanColumn.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());

  public KanbanSettings {
    Objects.requireNonNull(userSub, "userSub must not be null");
    if (userSub.isBlank()) {
      throw new IllegalArgumentException("userSub must not be blank");
    }
    if (doneRetentionDays < MIN_RETENTION_DAYS || doneRetentionDays > MAX_RETENTION_DAYS) {
      throw new IllegalArgumentException(
          "doneRetentionDays must be in " + MIN_RETENTION_DAYS + ".." + MAX_RETENTION_DAYS);
    }
    Objects.requireNonNull(activeFilters, "activeFilters must not be null");
    activeFilters = Set.copyOf(activeFilters);
  }

  /** Bequemer Konstruktor mit Default-Filtern (alle Spalten aktiv). */
  public KanbanSettings(String userSub, int doneRetentionDays) {
    this(userSub, doneRetentionDays, DEFAULT_FILTERS);
  }

  /** Default-Settings für einen User, falls noch nichts in der DB liegt. */
  public static KanbanSettings defaultFor(String userSub) {
    return new KanbanSettings(userSub, DEFAULT_RETENTION_DAYS, DEFAULT_FILTERS);
  }

  /**
   * Filtert eine rohe Filter-Eingabe auf die erlaubten Keys und verwirft Unbekanntes. {@code null}
   * (Feld gar nicht gesendet) fällt auf die Default-Filter zurück; eine leere Eingabe bleibt leer
   * ("nichts anzeigen").
   */
  public static Set<String> sanitizeFilters(Collection<String> raw) {
    if (raw == null) {
      return DEFAULT_FILTERS;
    }
    return raw.stream().filter(VALID_FILTER_KEYS::contains).collect(Collectors.toUnmodifiableSet());
  }

  /** Serialisiert die Filter als CSV in kanonischer Reihenfolge (für die Persistenz). */
  public static String serializeFilters(Set<String> filters) {
    return FILTER_ORDER.stream().filter(filters::contains).collect(Collectors.joining(","));
  }

  /**
   * Parst die persistierte CSV zurück. {@code null} (Legacy-Zeile ohne Spaltenwert) fällt auf die
   * Default-Filter zurück; ein leerer String bleibt leere Menge.
   */
  public static Set<String> parseFilters(String csv) {
    if (csv == null) {
      return DEFAULT_FILTERS;
    }
    if (csv.isBlank()) {
      return Set.of();
    }
    return sanitizeFilters(Arrays.asList(csv.split(",")));
  }

  /** Filter als Liste in kanonischer Reihenfolge (für stabile API-Responses). */
  public static List<String> orderedFilters(Set<String> filters) {
    return FILTER_ORDER.stream().filter(filters::contains).toList();
  }
}
