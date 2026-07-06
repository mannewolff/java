import type { KanbanBoard, KanbanColumn, KanbanItem } from '../../api/kanban';
import { KANBAN_COLUMNS } from '../../api/kanban';

/** Leeres Board fuer alle fuenf Spalten. */
export function emptyBoard(): KanbanBoard {
  return KANBAN_COLUMNS.reduce<KanbanBoard>(
    (acc, col) => ({ ...acc, [col]: [] }),
    {} as KanbanBoard,
  );
}

/**
 * Pure Funktion fuer den Drag&Drop-Move. Liefert die neue Board-Sicht zurueck, in der das
 * Item {@code itemId} in {@code targetColumn} an {@code targetPosition} liegt. Quell- und
 * Ziel-Spalte werden re-indiziert (lückenlos). Ist das Item nicht im Board, ist der Aufruf
 * idempotent — gibt das Eingangs-Board zurueck.
 *
 * Die Funktion ist absichtlich frei von React, dnd-kit und API-Calls, damit sie isoliert
 * testbar ist (Move-Reihenfolge im Frontend ist die wackeligste Stelle).
 */
export function moveItem(
  board: KanbanBoard,
  itemId: number,
  targetColumn: KanbanColumn,
  targetPosition: number,
): KanbanBoard {
  const sourceEntry = findItem(board, itemId);
  if (!sourceEntry) return board;
  if (sourceEntry.item.archived) return board;
  const { item, column: sourceColumn, index: sourceIndex } = sourceEntry;

  if (sourceColumn === targetColumn) {
    const same = board[sourceColumn];
    const max = same.length - 1;
    const clamped = Math.max(0, Math.min(targetPosition, max));
    if (clamped === sourceIndex) return board;
    const without = same.filter((_, i) => i !== sourceIndex);
    const inserted = [...without.slice(0, clamped), item, ...without.slice(clamped)];
    return {
      ...board,
      [sourceColumn]: reindex(inserted, sourceColumn),
    };
  }

  const sourceWithout = board[sourceColumn].filter((_, i) => i !== sourceIndex);
  const targetList = board[targetColumn];
  const clamped = Math.max(0, Math.min(targetPosition, targetList.length));
  const movedItem: KanbanItem = {
    ...item,
    column: targetColumn,
    position: clamped,
    // movedToDoneAt im Frontend "best effort" — Backend ist Quelle der Wahrheit.
    movedToDoneAt:
      targetColumn === 'DONE' ? item.movedToDoneAt ?? new Date().toISOString() : null,
  };
  const targetInserted = [
    ...targetList.slice(0, clamped),
    movedItem,
    ...targetList.slice(clamped),
  ];
  return {
    ...board,
    [sourceColumn]: reindex(sourceWithout, sourceColumn),
    [targetColumn]: reindex(targetInserted, targetColumn),
  };
}

/** Setzt position lückenlos 0..n-1 und korrigiert column-Feld pro Sicherheit. */
function reindex(items: KanbanItem[], column: KanbanColumn): KanbanItem[] {
  return items.map((i, idx) => ({ ...i, column, position: idx }));
}

interface ItemLocation {
  item: KanbanItem;
  column: KanbanColumn;
  index: number;
}

function findItem(board: KanbanBoard, itemId: number): ItemLocation | null {
  for (const col of KANBAN_COLUMNS) {
    const idx = board[col].findIndex((i) => i.id === itemId);
    if (idx !== -1) return { item: board[col][idx], column: col, index: idx };
  }
  return null;
}
