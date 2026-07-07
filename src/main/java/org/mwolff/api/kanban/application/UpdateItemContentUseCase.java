package org.mwolff.api.kanban.application;

import java.util.List;

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

  /**
   * Aktualisiert Titel, Body, Kürzel und Epic-Zuordnung (#339). Die Parent-Zuordnung wird gegen
   * dieselbe Regel wie beim Anlegen geprüft (existiert, Typ EPIC, Owner → sonst {@link
   * IllegalArgumentException} → 400). {@code parentId = null} entfernt eine bestehende Zuordnung;
   * ein EPIC darf keinen Parent bekommen.
   */
  @Transactional
  public KanbanItem execute(
      String userSub, long itemId, String title, String body, String shortcode, Long parentId) {
    final KanbanItem existing = load(userSub, itemId);
    EpicAssignment.validateParent(items, userSub, existing.type(), parentId);
    return items.save(existing.withContent(title, body, shortcode, parentId));
  }

  /**
   * Wie oben, zusätzlich mit Abhängigkeiten (#352). Jede referenzierte Anzeige-Nummer muss zu einem
   * eigenen Item gehören und darf nicht das Item selbst sein — sonst {@link
   * IllegalArgumentException} → 400.
   */
  @Transactional
  public KanbanItem execute(
      String userSub,
      long itemId,
      String title,
      String body,
      String shortcode,
      Long parentId,
      List<Integer> dependencies) {
    final KanbanItem existing = load(userSub, itemId);
    EpicAssignment.validateParent(items, userSub, existing.type(), parentId);
    final KanbanItem updated =
        existing.withContent(title, body, shortcode, parentId).withDependencies(dependencies);
    DependencyValidation.validate(items, userSub, updated.number(), updated.dependencies());
    return items.save(updated);
  }

  private KanbanItem load(String userSub, long itemId) {
    return items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
  }
}
