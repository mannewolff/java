package org.mwolff.api.kanban.web;

import org.mwolff.api.kanban.domain.EpicHasChildrenException;
import org.mwolff.api.kanban.domain.KanbanAttachmentLimitExceededException;
import org.mwolff.api.kanban.domain.KanbanAttachmentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanCommentForbiddenException;
import org.mwolff.api.kanban.domain.KanbanCommentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanTokenNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mapping von Kanban-Domain-Exceptions auf HTTP-Antworten. */
@RestControllerAdvice(basePackages = "org.mwolff.api.kanban.web")
public class KanbanExceptionHandler {

  @ExceptionHandler({
    KanbanItemNotFoundException.class,
    KanbanCommentNotFoundException.class,
    KanbanAttachmentNotFoundException.class,
    KanbanTokenNotFoundException.class
  })
  public ResponseEntity<Void> handleNotFound(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  /**
   * Upload über das Anhang-Limit hinaus (#350). 409 signalisiert einen per Löschen auflösbaren
   * Zustandskonflikt.
   */
  @ExceptionHandler(KanbanAttachmentLimitExceededException.class)
  public ResponseEntity<Void> handleAttachmentLimit(KanbanAttachmentLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  @ExceptionHandler(KanbanCommentForbiddenException.class)
  public ResponseEntity<Void> handleForbidden(KanbanCommentForbiddenException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  /**
   * Versuch, ein Epic zu löschen, auf das noch Items verweisen (#330). 409 signalisiert dem Client
   * einen auflösbaren Konflikt: erst die zugeordneten Items umhängen/löschen, dann das Epic.
   */
  @ExceptionHandler(EpicHasChildrenException.class)
  public ResponseEntity<Void> handleEpicHasChildren(EpicHasChildrenException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  /**
   * Fachliche Regel-Verletzungen aus den Use-Cases (#321): z. B. der Versuch, ein Epic auf dem
   * Board zu verschieben. Ohne dieses Mapping fiele der Fall auf 500 zurück statt auf 400.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  /**
   * Verletzung eines DB-Constraints unter Nebenläufigkeit (#309): kollidierende Anzeige-Nummer
   * (uk_kanban_item_number_per_user) oder aktive Position (uk_kanban_active_position) bei
   * parallelen Create-/Move-Requests. Ein sauberes 409 signalisiert dem Client einen per Retry
   * auflösbaren Konflikt — statt eines intransparenten 500.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Void> handleConflict(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }
}
