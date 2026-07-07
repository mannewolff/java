package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanAttachmentMeta;
import org.mwolff.api.kanban.domain.KanbanAttachmentPort;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Listet die Anhang-Metadaten eines eigenen Items. Fremdes/unbekanntes Item → 404. */
@Component
public class ListAttachmentsUseCase {

  private final KanbanItemPort items;
  private final KanbanAttachmentPort attachments;

  public ListAttachmentsUseCase(KanbanItemPort items, KanbanAttachmentPort attachments) {
    this.items = items;
    this.attachments = attachments;
  }

  @Transactional(readOnly = true)
  public List<KanbanAttachmentMeta> execute(String userSub, long itemId) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
    return attachments.findMetaByItem(itemId);
  }
}
