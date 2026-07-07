package org.mwolff.api.kanban.domain;

/**
 * Wird geworfen, wenn ein Item bereits die maximale Anzahl Anhänge hält und ein weiterer Upload
 * versucht wird → 409 Conflict (ein per Löschen auflösbarer Zustandskonflikt).
 */
public class KanbanAttachmentLimitExceededException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public KanbanAttachmentLimitExceededException(long itemId, int limit) {
    super("Item " + itemId + " already has the maximum of " + limit + " attachments");
  }
}
