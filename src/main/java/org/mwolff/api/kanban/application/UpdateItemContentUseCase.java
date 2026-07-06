package org.mwolff.api.kanban.application;

import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Aktualisiert Title und Body (und für Epics das Kürzel) eines Items. Foreign-Item → 404. */
@Component
public class UpdateItemContentUseCase {

  private final KanbanItemPort items;

  public UpdateItemContentUseCase(KanbanItemPort items) {
    this.items = items;
  }

  /** Aktualisiert Titel und Body; ein evtl. vorhandenes Epic-Kürzel bleibt unverändert. */
  @Transactional
  public KanbanItem execute(String userSub, long itemId, String title, String body) {
    return items.save(load(userSub, itemId).withContent(title, body));
  }

  /**
   * Aktualisiert Titel, Body und Kürzel (#330). Ein Kürzel an einem Nicht-Epic wirft im
   * Domänenmodell {@link IllegalArgumentException} → 400.
   */
  @Transactional
  public KanbanItem execute(
      String userSub, long itemId, String title, String body, String shortcode) {
    return items.save(load(userSub, itemId).withContent(title, body, shortcode));
  }

  private KanbanItem load(String userSub, long itemId) {
    return items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
  }
}
