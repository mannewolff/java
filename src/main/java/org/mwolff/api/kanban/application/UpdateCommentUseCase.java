package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanComment;
import org.mwolff.api.kanban.domain.KanbanCommentForbiddenException;
import org.mwolff.api.kanban.domain.KanbanCommentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanCommentPort;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bearbeitet einen eigenen Kommentar. Fremdes/unbekanntes Item → 404, unbekannter Kommentar → 404,
 * fremder Autor → 403.
 */
@Component
public class UpdateCommentUseCase {

  private final KanbanItemPort items;
  private final KanbanCommentPort comments;

  public UpdateCommentUseCase(KanbanItemPort items, KanbanCommentPort comments) {
    this.items = items;
    this.comments = comments;
  }

  @Transactional
  public KanbanComment execute(
      String userSub, String author, long itemId, long commentId, String body) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    final KanbanComment existing =
        comments
            .findById(commentId)
            .filter(c -> c.itemId() == itemId)
            .orElseThrow(() -> new KanbanCommentNotFoundException(commentId));
    if (!existing.author().equals(author)) {
      throw new KanbanCommentForbiddenException(commentId);
    }
    return comments.save(existing.withBody(body));
  }
}
