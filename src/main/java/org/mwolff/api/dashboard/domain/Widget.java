package org.mwolff.api.dashboard.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

/**
 * Einzelnes Widget eines Dashboards.
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param dashboardId ID des zugehörigen Dashboards
 * @param type Typ des Widgets, bestimmt das Frontend-Rendering und die Config-Bedeutung
 * @param position Position und Größe im Grid-Layout
 * @param config opaker JSON-Konfigurationsstring; Inhalt vom {@link WidgetType} abhängig
 * @param createdAt Zeitpunkt der Erstanlage
 * @param updatedAt Zeitpunkt der letzten Änderung
 */
public record Widget(
    Long id,
    Long dashboardId,
    WidgetType type,
    WidgetPosition position,
    String config,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Maximale Bytegröße der {@code config} — entspricht der Kapazität der {@code TEXT}-Spalte
   * (65_535 Bytes). Die Prüfung erfolgt in UTF-8-Bytes (nicht Zeichen), damit auch mehrbyteige
   * Inhalte die Spalte nie überlaufen (sonst {@code DataIntegrityViolationException} → HTTP 500).
   */
  public static final int MAX_CONFIG_BYTES = 65_535;

  public Widget {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(position, "position must not be null");
    Objects.requireNonNull(config, "config must not be null");
    if (config.getBytes(StandardCharsets.UTF_8).length > MAX_CONFIG_BYTES) {
      throw new IllegalArgumentException(
          "config must be at most " + MAX_CONFIG_BYTES + " bytes (UTF-8)");
    }
  }

  /** Erzeugt ein noch nicht persistiertes Widget (id und dashboardId optional). */
  public static Widget newInstance(
      Long dashboardId, WidgetType type, WidgetPosition position, String config) {
    return new Widget(null, dashboardId, type, position, config, null, null);
  }
}
