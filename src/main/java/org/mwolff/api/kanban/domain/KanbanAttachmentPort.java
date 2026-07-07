package org.mwolff.api.kanban.domain;

import java.util.List;
import java.util.Optional;

/** Persistenz-Port für {@link KanbanAttachment}. */
public interface KanbanAttachmentPort {

  /** Metadaten aller Anhänge eines Items (ohne Blob), älteste zuerst. */
  List<KanbanAttachmentMeta> findMetaByItem(long itemId);

  /** Einzelner Anhang inkl. Binärdaten (für den Download). */
  Optional<KanbanAttachment> findById(long id);

  KanbanAttachment save(KanbanAttachment attachment);

  void deleteById(long id);

  /** Anzahl der Anhänge eines Items — für das Mengen-Limit. */
  long countByItem(long itemId);
}
