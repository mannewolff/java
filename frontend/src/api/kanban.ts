import { api } from './client';

export type KanbanColumn = 'BACKLOG' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';

export const KANBAN_COLUMNS: readonly KanbanColumn[] = [
  'BACKLOG',
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
  /** Nur in der DONE-Spalte gesetzt — Basis fuer den Cleanup-Countdown. */
  movedToDoneAt?: string;
}

export type KanbanBoard = Record<KanbanColumn, KanbanItem[]>;

export interface KanbanSettings {
  doneRetentionDays: number;
}

const PATH = '/kanban';

export function listKanbanItems(): Promise<KanbanBoard> {
  return api.get<KanbanBoard>(`${PATH}/items`);
}

export function createKanbanItem(
  title: string,
  body: string,
  column?: KanbanColumn,
): Promise<KanbanItem> {
  return api.post<KanbanItem>(`${PATH}/items`, { title, body, column });
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

export function deleteKanbanItem(id: number): Promise<void> {
  return api.del(`${PATH}/items/${id}`);
}

export function getKanbanSettings(): Promise<KanbanSettings> {
  return api.get<KanbanSettings>(`${PATH}/settings`);
}

export function updateKanbanSettings(
  doneRetentionDays: number,
): Promise<KanbanSettings> {
  return api.put<KanbanSettings>(`${PATH}/settings`, { doneRetentionDays });
}
