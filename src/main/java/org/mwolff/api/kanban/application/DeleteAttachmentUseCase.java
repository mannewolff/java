package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanAttachmentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanAttachmentPort;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Löscht einen Anhang eines eigenen Items. Fremdes/unbekanntes Item → 404; unbekannter Anhang oder
 * Anhang eines anderen Items → 404.
 */
@Component
public class DeleteAttachmentUseCase {

  private final KanbanItemPort items;
  private final KanbanAttachmentPort attachments;

  public DeleteAttachmentUseCase(KanbanItemPort items, KanbanAttachmentPort attachments) {
    this.items = items;
    this.attachments = attachments;
  }

  @Transactional
  public void execute(String userSub, long itemId, long attachmentId) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    attachments
        .findById(attachmentId)
        .filter(a -> a.itemId() == itemId)
        .orElseThrow(() -> new KanbanAttachmentNotFoundException(attachmentId));
    attachments.deleteById(attachmentId);
  }
}
