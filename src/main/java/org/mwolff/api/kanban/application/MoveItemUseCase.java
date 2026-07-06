package org.mwolff.api.kanban.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanItemType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verschiebt ein Item innerhalb oder zwischen Spalten und hält die Positionen der betroffenen
 * Spalten lückenlos und eindeutig.
 *
 * <p>Alles in <strong>einer</strong> Transaktion — sonst riskieren wir inkonsistente Positionen,
 * wenn ein Zwischenschritt abbricht.
 *
 * <p>Reindex-Reihenfolge (#309): Der Unique-Constraint {@code uk_kanban_active_position} prüft
 * jedes einzelne Positions-Update sofort (MariaDB kennt keine deferred constraints). Daher darf nie
 * ein Zwischenschritt zwei aktive Items derselben Spalte auf dieselbe Position setzen. Das
 * erreichen wir, indem wir das bewegte Item zuerst aus dem aktiven Positionsraum nehmen bzw. die
 * Nachbarn in kollisionsfreier Richtung verschieben (auf-/absteigend je nach Bewegungsrichtung).
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
    // Epics nehmen nicht am Spalten-Workflow teil (#321) — ein Move würde die
    // Positions-Invarianten der Zielspalte unterlaufen (Epics halten keine aktive Position).
    if (existing.type() == KanbanItemType.EPIC) {
      throw new IllegalArgumentException("epics cannot be moved on the board");
    }

    final KanbanColumn sourceColumn = existing.column();
    final int sourcePosition = existing.position();
    final Instant now = Instant.now(clock);

    if (sourceColumn == targetColumn) {
      final int clamped = reorderWithinColumn(userSub, existing, targetPosition);
      if (clamped == sourcePosition) {
        // Auch nach dem Clamping keine effektive Positionsänderung — Idempotenz.
        return existing;
      }
      return items.save(existing.withColumnAndPosition(targetColumn, clamped, now));
    }

    final int clamped = shiftTargetForInsertion(userSub, targetColumn, targetPosition);
    // Item in die Zielspalte setzen; die Zielposition ist jetzt frei, die Quellspalte behält
    // vorübergehend eine Lücke.
    final KanbanItem saved = items.save(existing.withColumnAndPosition(targetColumn, clamped, now));
    // Lücke in der Quellspalte schließen — das Item hat die Quelle bereits verlassen.
    closeGap(userSub, sourceColumn, sourcePosition);
    return saved;
  }

  /**
   * Same-Column-Reorder. Nimmt das bewegte Item zunächst ans (freie) Spaltenende, schiebt die
   * betroffenen Nachbarn in kollisionsfreier Richtung um eins und liefert die geclampte
   * Zielposition zurück (der Aufrufer platziert das Item final dorthin).
   */
  private int reorderWithinColumn(String userSub, KanbanItem moved, int targetPosition) {
    final List<KanbanItem> column = items.findByUserAndColumn(userSub, moved.column());
    final int from = moved.position();
    final int clamped = Math.max(0, Math.min(targetPosition, column.size() - 1));
    if (clamped == from) {
      return from;
    }
    // Bewegtes Item temporär ans freie Spaltenende (Position = size), damit seine bisherige
    // Position frei wird und die folgenden Einzel-Updates nie kollidieren.
    items.updatePosition(moved.id(), column.size());
    if (clamped > from) {
      // Bewegung nach hinten: Nachbarn im Intervall (from, clamped] um -1, aufsteigend.
      for (final KanbanItem other : column) {
        final int pos = other.position();
        if (!moved.id().equals(other.id()) && pos > from && pos <= clamped) {
          items.updatePosition(other.id(), pos - 1);
        }
      }
    } else {
      // Bewegung nach vorn: Nachbarn im Intervall [clamped, from) um +1, absteigend.
      for (int idx = column.size() - 1; idx >= 0; idx--) {
        final KanbanItem other = column.get(idx);
        final int pos = other.position();
        if (!moved.id().equals(other.id()) && pos >= clamped && pos < from) {
          items.updatePosition(other.id(), pos + 1);
        }
      }
    }
    return clamped;
  }

  /**
   * Zielspalte für ein Cross-Column-Insert vorbereiten: Items ab der (geclampten) Zielposition um
   * eins nach hinten, absteigend — so bleibt jeder Zwischenschritt kollisionsfrei. Liefert die
   * geclampte Zielposition (0..size, Insert ans Ende erlaubt).
   */
  private int shiftTargetForInsertion(String userSub, KanbanColumn column, int targetPosition) {
    final List<KanbanItem> target = items.findByUserAndColumn(userSub, column);
    final int clamped = Math.max(0, Math.min(targetPosition, target.size()));
    for (int idx = target.size() - 1; idx >= 0; idx--) {
      final KanbanItem other = target.get(idx);
      if (other.position() >= clamped) {
        items.updatePosition(other.id(), other.position() + 1);
      }
    }
    return clamped;
  }

  /**
   * Quellspalte nach dem Entfernen kompaktieren: Items mit position > entfernt um -1, aufsteigend.
   */
  private void closeGap(String userSub, KanbanColumn column, int removedPosition) {
    for (final KanbanItem other : items.findByUserAndColumn(userSub, column)) {
      if (other.position() > removedPosition) {
        items.updatePosition(other.id(), other.position() - 1);
      }
    }
  }
}
