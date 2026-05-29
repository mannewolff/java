package org.mwolff.api.kanban.domain;

/**
 * Wird geworfen, wenn ein User einen Kommentar bearbeiten oder löschen will, dessen Autor er nicht
 * ist. Das Item gehört ihm (sonst {@link KanbanItemNotFoundException}), aber der Kommentar stammt
 * von jemand anderem → 403 Forbidden.
 */
public class KanbanCommentForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public KanbanCommentForbiddenException(long id) {
    super("Not the author of kanban comment " + id);
  }
}
