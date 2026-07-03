import type { KanbanColumn } from '../../api/kanban';

/**
 * Zentrale Statusfarben-Palette fuer das Kanban-Board, angelehnt an das
 * claude-workflow-kit-Board (kit/board-ui.mjs). Bewusste Ausnahme von "Farben nur ueber
 * das Theme" (CLAUDE-react.md): Kanban-Status ist eine feste, kleine, semantische Menge,
 * kein allgemeines UI-Token — ein zentraler Ort statt verstreuter Hex-Werte je Komponente.
 */
export interface StatusColorSet {
  /** Hintergrund fuer Spalten-Header und Status-Badges. */
  bg: string;
  /** Textfarbe auf {@link bg}. */
  text: string;
  /** Farbe des Status-Punkts im Spalten-Header. */
  dot: string;
}

export const STATUS_COLORS: Record<KanbanColumn, StatusColorSet> = {
  BACKLOG: { bg: '#dfe1e6', text: '#42526e', dot: '#6b7280' },
  READY: { bg: '#deebff', text: '#0747a6', dot: '#0075ca' },
  IN_PROGRESS: { bg: '#fffae6', text: '#7a6000', dot: '#e4b400' },
  IN_REVIEW: { bg: '#ffedeb', text: '#bf2600', dot: '#d93f0b' },
  DONE: { bg: '#e3fcef', text: '#006644', dot: '#0e8a16' },
};

/** Fuer archivierte Items — kein eigener Board-Status, nur in Badges/Listen genutzt. */
export const ARCHIVED_STATUS_COLOR: StatusColorSet = {
  bg: '#f0f0f0',
  text: '#666666',
  dot: '#9e9e9e',
};

/** Neutrale Spaltenflaeche (Kit: `#ebecf0`) — kein Status, daher kein eigener Key. */
export const COLUMN_SURFACE_BG = '#ebecf0';
