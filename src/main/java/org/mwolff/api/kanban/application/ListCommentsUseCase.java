package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanComment;
import org.mwolff.api.kanban.domain.KanbanCommentPort;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Listet die Kommentare eines eigenen Items, neueste zuerst. Fremdes/unbekanntes Item → 404. */
@Component
public class ListCommentsUseCase {

  private final KanbanItemPort items;
  private final KanbanCommentPort comments;

  public ListCommentsUseCase(KanbanItemPort items, KanbanCommentPort comments) {
    this.items = items;
    this.comments = comments;
  }

  @Transactional(readOnly = true)
  public List<KanbanComment> execute(String userSub, long itemId) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    return comments.findByItemNewestFirst(itemId);
  }
}
