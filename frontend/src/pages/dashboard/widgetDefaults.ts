import type { WidgetDto, WidgetType } from '../../api/dashboard';

/** Grid-Konstanten — bewusst exportiert, damit Tests und künftige Widget-Typen sie teilen. */
export const GRID_COLS = 12;
export const GRID_ROW_HEIGHT = 40;
export const DESKTOP_MIN_WIDTH = 1024;
export const AUTO_SAVE_DEBOUNCE_MS = 500;

/**
 * Default-Größen pro Widget-Typ. Die Werte sind Grid-Einheiten — 12 Spalten total, also
 * fühlt sich KPI 2×2 wie ein Sechstel der Breite und Textbox 4×3 wie ein Drittel an.
 */
export const WIDGET_DEFAULTS: Record<WidgetType, { width: number; height: number }> = {
  KPI: { width: 2, height: 2 },
  TEXTBOX: { width: 4, height: 3 },
};

/** Initial-Config je Widget-Typ — passend zu den Widget-Komponenten in widgets/. */
export const WIDGET_INITIAL_CONFIG: Record<WidgetType, string> = {
  KPI: JSON.stringify({ value: 0, label: 'Neue Kennzahl', color: 'neutral' }),
  TEXTBOX: JSON.stringify({ markdown: '# Neue Textbox\n\nText hier eingeben.' }),
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
