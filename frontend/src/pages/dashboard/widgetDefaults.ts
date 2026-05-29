import type { WidgetDto, WidgetType } from '../../api/dashboard';

/** Grid-Konstanten — bewusst exportiert, damit Tests und künftige Widget-Typen sie teilen. */
export const GRID_COLS = 12;
export const GRID_ROW_HEIGHT = 40;
/** react-grid-layout-Default-Margin zwischen Items, hier nicht überschrieben. */
export const GRID_MARGIN = 10;
export const DESKTOP_MIN_WIDTH = 1024;
export const AUTO_SAVE_DEBOUNCE_MS = 500;

/** Wieviele Grid-Rows muss ein Widget hoch sein, um `pxHeight` Pixel aufzunehmen. */
export function pxToRows(pxHeight: number): number {
  // Höhe eines N-Row-Items = N * GRID_ROW_HEIGHT + (N - 1) * GRID_MARGIN.
  // Gesucht das kleinste N mit N * row + (N - 1) * margin >= px:
  //   N >= (px + margin) / (row + margin)
  return Math.max(1, Math.ceil((pxHeight + GRID_MARGIN) / (GRID_ROW_HEIGHT + GRID_MARGIN)));
}

/**
 * Default-Größen pro Widget-Typ. Die Werte sind Grid-Einheiten — 12 Spalten total, also
 * fühlt sich KPI 2×2 wie ein Sechstel der Breite und Textbox 4×3 wie ein Drittel an.
 */
export const WIDGET_DEFAULTS: Record<WidgetType, { width: number; height: number }> = {
  KPI: { width: 2, height: 2 },
  TEXTBOX: { width: 4, height: 3 },
  PLOT: { width: 6, height: 4 },
  KANBAN_LIST: { width: 3, height: 4 },
  DIVIDER: { width: 6, height: 1 },
};

/** Initial-Config je Widget-Typ — passend zu den Widget-Komponenten in widgets/. */
export const WIDGET_INITIAL_CONFIG: Record<WidgetType, string> = {
  KPI: JSON.stringify({
    style: 'gauge',
    value: 50,
    label: 'Neue Kennzahl',
    min: 0,
    max: 100,
    lowEnd: 33,
    mediumEnd: 66,
    rangeLabel: '',
  }),
  TEXTBOX: JSON.stringify({ markdown: '# Neue Textbox\n\nText hier eingeben.' }),
  PLOT: JSON.stringify({
    timeSeriesId: null,
    defaultGranularity: null,
    overlays: [],
  }),
  KANBAN_LIST: JSON.stringify({ column: 'BACKLOG', limit: 5 }),
  DIVIDER: JSON.stringify({ orientation: 'horizontal', color: '', thickness: 2 }),
};

/** Erzeugt ein neues Widget mit Default-Größe und -Config an Position (0, 0). */
export function newWidget(type: WidgetType): WidgetDto {
  const { width, height } = WIDGET_DEFAULTS[type];
  return {
    type,
    posX: 0,
    posY: 0,
    width,
    height,
    config: WIDGET_INITIAL_CONFIG[type],
  };
}
