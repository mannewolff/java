package org.mwolff.api.kanban.domain;

/**
 * Wird geworfen wenn ein Kanban-Item nicht existiert ODER nicht dem aufrufenden User gehört. Beide
 * Fälle werden bewusst gleich behandelt — kein Existenz-Leak (Pattern analog Dashboard).
 */
public class KanbanItemNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public KanbanItemNotFoundException(long id) {
    super("Kanban item " + id + " not found");
  }
}
