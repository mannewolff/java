package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanComment;
import org.mwolff.api.kanban.domain.KanbanCommentPort;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fügt einem eigenen Item einen Kommentar hinzu. Fremdes/unbekanntes Item → 404. */
@Component
public class AddCommentUseCase {

  private final KanbanItemPort items;
  private final KanbanCommentPort comments;

  public AddCommentUseCase(KanbanItemPort items, KanbanCommentPort comments) {
    this.items = items;
    this.comments = comments;
  }

  @Transactional
  public KanbanComment execute(String userSub, String author, long itemId, String body) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    return comments.save(KanbanComment.newInstance(itemId, author, body));
  }
}
