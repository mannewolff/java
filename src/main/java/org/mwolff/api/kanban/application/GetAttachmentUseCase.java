package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.domain.KanbanAttachmentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanAttachmentPort;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert einen Anhang inkl. Binärdaten für den Download. Fremdes/unbekanntes Item → 404;
 * unbekannter Anhang oder Anhang eines anderen Items → 404.
 */
@Component
public class GetAttachmentUseCase {

  private final KanbanItemPort items;
  private final KanbanAttachmentPort attachments;

  public GetAttachmentUseCase(KanbanItemPort items, KanbanAttachmentPort attachments) {
    this.items = items;
    this.attachments = attachments;
  }

  @Transactional(readOnly = true)
  public KanbanAttachment execute(String userSub, long itemId, long attachmentId) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    return attachments
        .findById(attachmentId)
        .filter(a -> a.itemId() == itemId)
        .orElseThrow(() -> new KanbanAttachmentNotFoundException(attachmentId));
  }
}
