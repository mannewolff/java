package org.mwolff.api.kanban.domain;

/**
 * Wird geworfen, wenn versucht wird, ein Epic zu löschen, auf das noch mindestens ein Item verweist
 * ({@code parentId == epic.id}). Referentielle Integrität: solange ein zugeordnetes Item existiert
 * (auch ein archiviertes), bleibt das Epic unlöschbar. Wird im Web-Layer auf {@code 409 Conflict}
 * gemappt (#330).
 */
public class EpicHasChildrenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EpicHasChildrenException(long epicId, long childCount) {
    super("epic " + epicId + " still has " + childCount + " referencing item(s)");
  }
}
