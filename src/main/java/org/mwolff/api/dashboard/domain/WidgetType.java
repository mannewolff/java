package org.mwolff.api.dashboard.domain;

/**
 * Vom UI unterstützte Widget-Typen. Erweiterbar in Folge-Phasen (Phase 3+4 bringen KPI- und
 * Textbox-Renderer).
 */
public enum WidgetType {
  TEXTBOX,
  KPI,
  PLOT,
  KANBAN_LIST,
  DIVIDER
}
