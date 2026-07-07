package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanItemPort;

/**
 * Prüft die Abhängigkeits-Nummern eines Items (#352): jede referenzierte Anzeige-Nummer muss zu
 * einem eigenen Item des Users gehören, und ein Item darf nicht von sich selbst abhängen. Verstöße
 * → {@link IllegalArgumentException} (vom {@code KanbanExceptionHandler} auf 400 gemappt). Die
 * Liste ist bereits normalisiert (dedupliziert, sortiert, ohne Werte ≤ 0).
 */
final class DependencyValidation {

  private DependencyValidation() {}

  static void validate(
      KanbanItemPort items, String userSub, int ownNumber, List<Integer> dependencies) {
    for (final int dependency : dependencies) {
      if (dependency == ownNumber) {
        throw new IllegalArgumentException(
            "an item must not depend on itself (#" + dependency + ")");
      }
      items
          .findByUserAndNumber(userSub, dependency)
          .orElseThrow(
              () -> new IllegalArgumentException("dependency #" + dependency + " does not exist"));
    }
  }
}
