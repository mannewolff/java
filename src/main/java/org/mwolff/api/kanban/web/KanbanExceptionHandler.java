package org.mwolff.api.kanban.web;

import org.mwolff.api.kanban.domain.KanbanCommentForbiddenException;
import org.mwolff.api.kanban.domain.KanbanCommentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mapping von Kanban-Domain-Exceptions auf HTTP-Antworten. */
@RestControllerAdvice(basePackages = "org.mwolff.api.kanban.web")
public class KanbanExceptionHandler {

  @ExceptionHandler({KanbanItemNotFoundException.class, KanbanCommentNotFoundException.class})
  public ResponseEntity<Void> handleNotFound(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler(KanbanCommentForbiddenException.class)
  public ResponseEntity<Void> handleForbidden(KanbanCommentForbiddenException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }
}
