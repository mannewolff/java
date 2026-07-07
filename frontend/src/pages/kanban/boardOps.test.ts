import { describe, expect, it } from 'vitest';
import { emptyBoard, moveItem, unmetBacklogDependencies } from './boardOps';
import type { KanbanBoard, KanbanColumn, KanbanItem } from '../../api/kanban';

function item(id: number, column: KanbanColumn, position: number, archived = false): KanbanItem {
  return {
    id,
    title: `T-${id}`,
    body: '',
    column,
    position,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    movedToDoneAt: column === 'DONE' ? '2026-01-01T00:00:00Z' : null,
    archived,
    number: id,
    type: 'ITEM' as const,
    parentId: null,
    dependencies: [],
  };
}

function boardOf(input: Partial<Record<KanbanColumn, KanbanItem[]>>): KanbanBoard {
  return { ...emptyBoard(), ...input };
}

describe('emptyBoard', () => {
  it('liefert leere Spalten für alle fünf Slots', () => {
    const b = emptyBoard();
    expect(b.BACKLOG).toEqual([]);
    expect(b.READY).toEqual([]);
    expect(b.IN_PROGRESS).toEqual([]);
    expect(b.IN_REVIEW).toEqual([]);
    expect(b.DONE).toEqual([]);
  });
});

describe('moveItem — same column', () => {
  it('verschiebt nach unten und re-indiziert', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0), item(2, 'BACKLOG', 1), item(3, 'BACKLOG', 2)],
    });
    const next = moveItem(b, 1, 'BACKLOG', 2);
    expect(next.BACKLOG.map((i) => i.id)).toEqual([2, 3, 1]);
    expect(next.BACKLOG.map((i) => i.position)).toEqual([0, 1, 2]);
  });

  it('verschiebt nach oben und re-indiziert', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0), item(2, 'BACKLOG', 1), item(3, 'BACKLOG', 2)],
    });
    const next = moveItem(b, 3, 'BACKLOG', 0);
    expect(next.BACKLOG.map((i) => i.id)).toEqual([3, 1, 2]);
  });

  it('gibt das Eingangs-Board zurück, wenn dieselbe Position', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0), item(2, 'BACKLOG', 1)],
    });
    const next = moveItem(b, 1, 'BACKLOG', 0);
    expect(next).toBe(b);
  });

  it('clampt zu hohe Zielposition auf den letzten Slot', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0), item(2, 'BACKLOG', 1), item(3, 'BACKLOG', 2)],
    });
    const next = moveItem(b, 1, 'BACKLOG', 99);
    expect(next.BACKLOG.map((i) => i.id)).toEqual([2, 3, 1]);
  });
});

describe('moveItem — cross column', () => {
  it('schliesst Quell-Spalten-Lücke und öffnet Ziel-Spalten-Lücke', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0), item(2, 'BACKLOG', 1)],
      IN_PROGRESS: [item(10, 'IN_PROGRESS', 0)],
    });
    const next = moveItem(b, 1, 'IN_PROGRESS', 0);
    expect(next.BACKLOG.map((i) => i.id)).toEqual([2]);
    expect(next.BACKLOG[0].position).toBe(0);
    expect(next.IN_PROGRESS.map((i) => i.id)).toEqual([1, 10]);
    expect(next.IN_PROGRESS.map((i) => i.position)).toEqual([0, 1]);
  });

  it('setzt movedToDoneAt beim Eintritt in DONE (Frontend-Best-Effort)', () => {
    const b = boardOf({ BACKLOG: [item(1, 'BACKLOG', 0)] });
    const next = moveItem(b, 1, 'DONE', 0);
    expect(next.DONE[0].movedToDoneAt).toBeDefined();
  });

  it('löscht movedToDoneAt beim Verlassen von DONE', () => {
    const b = boardOf({ DONE: [item(1, 'DONE', 0)] });
    const next = moveItem(b, 1, 'BACKLOG', 0);
    expect(next.BACKLOG[0].movedToDoneAt).toBeNull();
  });

  it('gibt das Eingangs-Board zurück, wenn das Item nicht existiert', () => {
    const b = boardOf({ BACKLOG: [item(1, 'BACKLOG', 0)] });
    const next = moveItem(b, 999, 'IN_PROGRESS', 0);
    expect(next).toBe(b);
  });

  it('clampt Zielposition jenseits der Länge ans Ende', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0)],
      IN_PROGRESS: [item(10, 'IN_PROGRESS', 0)],
    });
    const next = moveItem(b, 1, 'IN_PROGRESS', 99);
    expect(next.IN_PROGRESS.map((i) => i.id)).toEqual([10, 1]);
  });

  it('verschiebt von BACKLOG nach READY', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0)],
      READY: [item(10, 'READY', 0)],
    });
    const next = moveItem(b, 1, 'READY', 0);
    expect(next.BACKLOG).toEqual([]);
    expect(next.READY.map((i) => i.id)).toEqual([1, 10]);
  });

  it('archiviertes Item wird nicht bewegt — Board bleibt identisch', () => {
    const b = boardOf({
      BACKLOG: [item(1, 'BACKLOG', 0, true)],
    });
    const next = moveItem(b, 1, 'IN_PROGRESS', 0);
    expect(next).toBe(b);
  });
});

describe('unmetBacklogDependencies', () => {
  it('meldet eine Abhängigkeit, die noch im Backlog liegt', () => {
    const dep = { ...item(12, 'BACKLOG', 0), title: 'Vorarbeit' };
    const target = { ...item(2, 'IN_PROGRESS', 0), dependencies: [12] };
    const board = boardOf({ BACKLOG: [dep], IN_PROGRESS: [target] });

    expect(unmetBacklogDependencies(board, 2)).toEqual([{ number: 12, title: 'Vorarbeit' }]);
  });

  it('meldet nichts, wenn die Abhängigkeit den Backlog verlassen hat', () => {
    const dep = item(12, 'READY', 0);
    const target = { ...item(2, 'IN_PROGRESS', 0), dependencies: [12] };
    const board = boardOf({ READY: [dep], IN_PROGRESS: [target] });

    expect(unmetBacklogDependencies(board, 2)).toEqual([]);
  });

  it('ignoriert nicht (mehr) existierende Abhängigkeits-Nummern', () => {
    const target = { ...item(2, 'IN_PROGRESS', 0), dependencies: [999] };
    const board = boardOf({ IN_PROGRESS: [target] });

    expect(unmetBacklogDependencies(board, 2)).toEqual([]);
  });

  it('liefert leer, wenn das Item nicht im Board ist', () => {
    expect(unmetBacklogDependencies(emptyBoard(), 42)).toEqual([]);
  });

  it('gibt nur die Backlog-Abhängigkeiten einer gemischten Liste zurück', () => {
    const inBacklog = { ...item(1, 'BACKLOG', 0), title: 'A' };
    const inReady = item(3, 'READY', 0);
    const target = { ...item(9, 'IN_PROGRESS', 0), dependencies: [1, 3] };
    const board = boardOf({ BACKLOG: [inBacklog], READY: [inReady], IN_PROGRESS: [target] });

    expect(unmetBacklogDependencies(board, 9)).toEqual([{ number: 1, title: 'A' }]);
  });
});
