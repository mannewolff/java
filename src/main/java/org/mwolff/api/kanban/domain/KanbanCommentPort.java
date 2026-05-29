package org.mwolff.api.kanban.domain;

import java.util.List;
import java.util.Optional;

/**
 * Persistenz-Port für {@link KanbanComment}s. Implementiert im Infrastructure-Layer via JPA.
 *
 * <p>{@code findById} ist owner-agnostisch. Die Eigentumsprüfung (gehört das Item dem User, gehört
 * der Kommentar dem Autor) erfolgt in den Use-Cases.
 */
public interface KanbanCommentPort {

  /** Alle Kommentare eines Items, neueste zuerst. */
  List<KanbanComment> findByItemNewestFirst(long itemId);

  Optional<KanbanComment> findById(long id);

  KanbanComment save(KanbanComment comment);

  void deleteById(long id);
}
