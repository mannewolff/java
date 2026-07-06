import { api } from './client';

export type KanbanColumn = 'BACKLOG' | 'READY' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';

/** Typ eines Kanban-Eintrags: normale Board-Karte oder übergeordnetes Epic (#321). */
export type KanbanItemType = 'ITEM' | 'EPIC';

export const KANBAN_COLUMNS: readonly KanbanColumn[] = [
  'BACKLOG',
  'READY',
  'IN_PROGRESS',
  'IN_REVIEW',
  'DONE',
];

export interface KanbanItem {
  id: number;
  title: string;
  body: string;
  column: KanbanColumn;
  position: number;
  createdAt: string;
  updatedAt: string;
  /**
   * Nur in der DONE-Spalte gesetzt — Basis fuer den Cleanup-Countdown. Das Backend serialisiert
   * ausserhalb DONE {@code null} (nicht "fehlend"), daher {@code string | null}.
   */
  movedToDoneAt: string | null;
  /** Soft-Delete-Flag: archivierte Items werden standardmaessig nicht angezeigt. */
  archived: boolean;
  /** Fortlaufende, pro User eindeutige Anzeige-Nummer (#187/#188). */
  number: number;
  /** Typ: normale Karte (ITEM) oder Epic (#321). Epics erscheinen nicht auf dem Board. */
  type: KanbanItemType;
  /** ID des zugeordneten Epics oder {@code null}, wenn keinem Epic zugeordnet (#321). */
  parentId: number | null;
}

export type KanbanBoard = Record<KanbanColumn, KanbanItem[]>;

/** Ein Epic mit berechnetem Fortschritt (#322). */
export interface KanbanEpic {
  id: number;
  number: number;
  title: string;
  body: string;
  type: 'EPIC';
  /** Frei wählbares Kürzel (#329) oder {@code null} — dann wird eins aus dem Titel abgeleitet. */
  shortcode: string | null;
  progress: { done: number; total: number };
}

export interface KanbanSettings {
  doneRetentionDays: number;
}

export interface KanbanComment {
  id: number;
  itemId: number;
  /** Anzeigename des Verfassers (preferred_username). */
  author: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

const PATH = '/kanban';

export function listKanbanItems(includeArchived = false): Promise<KanbanBoard> {
  const qs = includeArchived ? '?includeArchived=true' : '';
  return api.get<KanbanBoard>(`${PATH}/items${qs}`);
}

export function createKanbanItem(
  title: string,
  body: string,
  column?: KanbanColumn,
  type: KanbanItemType = 'ITEM',
  parentId: number | null = null,
  shortcode: string | null = null,
): Promise<KanbanItem> {
  return api.post<KanbanItem>(`${PATH}/items`, {
    title,
    body,
    column,
    type,
    parentId,
    shortcode,
  });
}

/** Liefert die eigenen Epics inkl. Fortschritt (#322). */
export function getKanbanEpics(): Promise<KanbanEpic[]> {
  return api.get<KanbanEpic[]>(`${PATH}/epics`);
}

export function updateKanbanItem(
  id: number,
  title: string,
  body: string,
): Promise<KanbanItem> {
  return api.put<KanbanItem>(`${PATH}/items/${id}`, { title, body });
}

export function moveKanbanItem(
  id: number,
  column: KanbanColumn,
  position: number,
): Promise<KanbanItem> {
  return api.put<KanbanItem>(`${PATH}/items/${id}/move`, { column, position });
}

export function archiveKanbanItem(id: number): Promise<void> {
  return api.del(`${PATH}/items/${id}`);
}

export function forceDeleteKanbanItem(id: number): Promise<void> {
  return api.del(`${PATH}/items/${id}/force`);
}

export function restoreKanbanItem(id: number): Promise<KanbanItem> {
  return api.patch<KanbanItem>(`${PATH}/items/${id}/restore`);
}

export function getKanbanSettings(): Promise<KanbanSettings> {
  return api.get<KanbanSettings>(`${PATH}/settings`);
}

export function updateKanbanSettings(
  doneRetentionDays: number,
): Promise<KanbanSettings> {
  return api.put<KanbanSettings>(`${PATH}/settings`, { doneRetentionDays });
}

export function listKanbanComments(itemId: number): Promise<KanbanComment[]> {
  return api.get<KanbanComment[]>(`${PATH}/items/${itemId}/comments`);
}

export function addKanbanComment(
  itemId: number,
  body: string,
): Promise<KanbanComment> {
  return api.post<KanbanComment>(`${PATH}/items/${itemId}/comments`, { body });
}

export function updateKanbanComment(
  itemId: number,
  commentId: number,
  body: string,
): Promise<KanbanComment> {
  return api.put<KanbanComment>(
    `${PATH}/items/${itemId}/comments/${commentId}`,
    { body },
  );
}

export function deleteKanbanComment(
  itemId: number,
  commentId: number,
): Promise<void> {
  return api.del(`${PATH}/items/${itemId}/comments/${commentId}`);
}
