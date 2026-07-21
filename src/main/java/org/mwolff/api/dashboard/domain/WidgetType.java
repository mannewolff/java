package org.mwolff.api.dashboard.domain;

/**
 * Vom UI unterstützte Widget-Typen. Erweiterbar in Folge-Phasen (Phase 3+4 bringen KPI- und
 * Textbox-Renderer).
 */
public enum WidgetType {
  TEXTBOX,
  KPI,
  PLOT,
  /**
   * @deprecated Das Kanban-Modul wurde aus der Toolbox entfernt (Extraktion nach manban); das
   *     Frontend rendert dieses Widget nicht mehr. Der Enum-Wert bleibt bewusst erhalten, weil
   *     {@code WidgetType} in der {@code widgets}-Tabelle als {@code @Enumerated(STRING)}
   *     persistiert wird — ein Entfernen würde das Laden bestehender Dashboards mit einem
   *     gespeicherten {@code KANBAN_LIST}-Widget mit einer Deserialisierungs-Exception brechen.
   *     Nicht für neue Widgets verwenden.
   */
  @Deprecated
  KANBAN_LIST,
  DIVIDER,
  IMAGE
}
