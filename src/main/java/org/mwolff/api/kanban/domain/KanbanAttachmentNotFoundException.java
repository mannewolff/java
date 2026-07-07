package org.mwolff.api.kanban.domain;

/**
 * Wird geworfen, wenn ein Anhang nicht existiert oder nicht zum adressierten Item gehört. Wie bei
 * {@link KanbanItemNotFoundException} bewusst als 404 behandelt — kein Existenz-Leak.
 */
public class KanbanAttachmentNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public KanbanAttachmentNotFoundException(long id) {
    super("Kanban attachment " + id + " not found");
  }
}
