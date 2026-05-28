package org.mwolff.api.kanban.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verschiebt ein Item innerhalb oder zwischen Spalten und re-indexiert die Quell- und
 * Ziel-Spalten-Positionen lückenlos.
 *
 * <p>Alles in <strong>einer</strong> Transaktion — sonst riskieren wir inkonsistente Positionen,
 * wenn Schritt 2 von 3 abbricht.
 */
@Component
public class MoveItemUseCase {

  private final KanbanItemPort items;
  private final Clock clock;

  public MoveItemUseCase(KanbanItemPort items, Clock clock) {
    this.items = items;
    this.clock = clock;
  }

  @Transactional
  public KanbanItem execute(
      String userSub, long itemId, KanbanColumn targetColumn, int targetPosition) {
    final KanbanItem existing =
        items
            .findById(itemId)
            .filter(i -> i.userSub().equals(userSub))
            .orElseThrow(() -> new KanbanItemNotFoundException(itemId));

    final KanbanColumn sourceColumn = existing.column();
    final int sourcePosition = existing.position();

    if (sourceColumn == targetColumn && sourcePosition == targetPosition) {
      // Kein Move nötig — Idempotenz.
      return existing;
    }

    final int clampedTargetPosition;
    if (sourceColumn == targetColumn) {
      clampedTargetPosition =
          reindexWithinSameColumn(userSub, sourceColumn, sourcePosition, targetPosition);
    } else {
      reindexAfterRemoval(userSub, sourceColumn, sourcePosition);
      clampedTargetPosition = reindexBeforeInsertion(userSub, targetColumn, targetPosition);
    }

    final Instant now = Instant.now(clock);
    return items.save(existing.withColumnAndPosition(targetColumn, clampedTargetPosition, now));
  }

  /**
   * Same-Column-Reorder: schiebt die Items zwischen Quell- und Ziel-Position um eins, sodass am
   * Ende die Ziel-Position frei ist. Liefert die (geclampte) Ziel-Position zurück.
   */
  private int reindexWithinSameColumn(
      String userSub, KanbanColumn column, int fromPosition, int toPosition) {
    final List<KanbanItem> column_ = items.findByUserAndColumn(userSub, column);
    final int max = column_.size() - 1; // Größte gültige Position für ein bestehendes Item.
    final int clamped = Math.max(0, Math.min(toPosition, max));
    if (clamped == fromPosition) {
      return clamped;
    }
    if (clamped > fromPosition) {
      // Item wandert nach unten — alles dazwischen rutscht um 1 nach oben.
      for (final KanbanItem other : column_) {
        if (other.position() > fromPosition && other.position() <= clamped) {
          items.updatePosition(other.id(), other.position() - 1);
        }
      }
    } else {
      // Item wandert nach oben — alles dazwischen rutscht um 1 nach unten.
      for (final KanbanItem other : column_) {
        if (other.position() >= clamped && other.position() < fromPosition) {
          items.updatePosition(other.id(), other.position() + 1);
        }
      }
    }
    return clamped;
  }

  /** Quell-Spalte: Items mit position > entfernt um 1 dekrementieren. */
  private void reindexAfterRemoval(String userSub, KanbanColumn column, int removedPosition) {
    final List<KanbanItem> column_ = items.findByUserAndColumn(userSub, column);
    for (final KanbanItem other : column_) {
      if (other.position() > removedPosition) {
        items.updatePosition(other.id(), other.position() - 1);
      }
    }
  }

  /**
   * Ziel-Spalte: Items mit position >= um 1 inkrementieren. Liefert die geclampte Ziel-Position
   * (zwischen 0 und size, inklusive — Insert ans Ende ist erlaubt).
   */
  private int reindexBeforeInsertion(String userSub, KanbanColumn column, int targetPosition) {
    final List<KanbanItem> column_ = items.findByUserAndColumn(userSub, column);
    final int clamped = Math.max(0, Math.min(targetPosition, column_.size()));
    for (final KanbanItem other : column_) {
      if (other.position() >= clamped) {
        items.updatePosition(other.id(), other.position() + 1);
      }
    }
    return clamped;
  }
}
