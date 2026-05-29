package org.mwolff.api.kanban.domain;

/**
 * Wird geworfen, wenn ein Kommentar nicht existiert oder nicht zum adressierten Item gehört. Beide
 * Fälle werden bewusst gleich behandelt — kein Existenz-Leak.
 */
public class KanbanCommentNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public KanbanCommentNotFoundException(long id) {
    super("Kanban comment " + id + " not found");
  }
}
