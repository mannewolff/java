package org.mwolff.api.kanban.domain;

/**
 * Typ eines Kanban-Eintrags (#321). {@link #ITEM} ist die normale Board-Karte. {@link #EPIC} ist
 * ein übergeordneter Container für Items: Epics nehmen nicht am Spalten-Workflow teil (keine
 * Board-Karte, keine aktive Position) und dürfen selbst keinem Epic zugeordnet sein.
 */
public enum KanbanItemType {
  ITEM,
  EPIC
}
