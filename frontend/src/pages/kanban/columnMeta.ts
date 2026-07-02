import type { KanbanColumn } from '../../api/kanban';

/**
 * Anzeige-Labels je Kanban-Spalte. Gemeinsame Quelle fuer Listenansicht (#282) und Detail-Modal,
 * damit Spalten-Labels nicht ein weiteres Mal dupliziert werden.
 */
export const COLUMN_LABELS: Record<KanbanColumn, string> = {
  BACKLOG: 'Backlog',
  READY: 'Ready',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
};
