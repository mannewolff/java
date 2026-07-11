import { api } from './client';

export type WidgetType = 'TEXTBOX' | 'KPI' | 'PLOT' | 'DIVIDER' | 'IMAGE';

/** Wire-Format eines Widgets — passt zu `WidgetDto` im Spring-Backend. */
export interface WidgetDto {
  id?: number;
  type: WidgetType;
  posX: number;
  posY: number;
  width: number;
  height: number;
  /** Opake JSON-Konfiguration als String (Inhalt vom WidgetType abhängig). */
  config: string;
}

export interface DashboardSummary {
  id: number;
  name: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardDetail extends DashboardSummary {
  /** CSS-Farbwert für den Dashboard-Hintergrund; `null` = Theme-Default. */
  backgroundColor?: string | null;
  widgets: WidgetDto[];
}

const PATH = '/dashboards';

export function listDashboards(): Promise<DashboardSummary[]> {
  return api.get<DashboardSummary[]>(PATH);
}

export function getDashboard(id: number): Promise<DashboardDetail> {
  return api.get<DashboardDetail>(`${PATH}/${id}`);
}

export function createDashboard(name: string): Promise<DashboardSummary> {
  return api.post<DashboardSummary>(PATH, { name });
}

export function updateDashboard(id: number, widgets: WidgetDto[]): Promise<DashboardDetail> {
  return api.put<DashboardDetail>(`${PATH}/${id}`, { widgets });
}

export function setDefaultDashboard(id: number): Promise<DashboardSummary> {
  return api.put<DashboardSummary>(`${PATH}/${id}/default`, {});
}

export function renameDashboard(id: number, name: string): Promise<DashboardSummary> {
  return api.put<DashboardSummary>(`${PATH}/${id}/name`, { name });
}

export function setDashboardBackgroundColor(
  id: number,
  backgroundColor: string | null,
): Promise<DashboardSummary> {
  return api.put<DashboardSummary>(`${PATH}/${id}/background`, { backgroundColor });
}

export function deleteDashboard(id: number): Promise<void> {
  return api.del(`${PATH}/${id}`);
}
