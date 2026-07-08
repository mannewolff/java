package org.mwolff.api.kanban.domain;

/**
 * Wird geworfen, wenn ein Token nicht existiert ODER nicht dem aufrufenden User gehoert. Wie in der
 * uebrigen Kanban-Domaene werden beide Faelle als 404 nach aussen gemappt (kein Existenz-Leak).
 */
public class KanbanTokenNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public KanbanTokenNotFoundException(long id) {
    super("KanbanAccessToken " + id + " not found");
  }
}
