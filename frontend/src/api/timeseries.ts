import { api } from './client';

export type TimeSeriesDataType = 'DECIMAL' | 'INTEGER';

export interface TimeSeriesSummary {
  id: number;
  name: string;
  description?: string;
  unit: string;
  dataType: TimeSeriesDataType;
  entryCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface TimeSeriesEntry {
  id: number;
  timestamp: string;
  value: number;
}

export interface CreateTimeSeriesPayload {
  name: string;
  description?: string;
  unit: string;
  dataType: TimeSeriesDataType;
}

export type UpdateTimeSeriesPayload = CreateTimeSeriesPayload;

export interface ListEntriesParams {
  from?: string;
  to?: string;
  limit?: number;
}

const PATH = '/timeseries';

export function listTimeSeries(): Promise<TimeSeriesSummary[]> {
  return api.get<TimeSeriesSummary[]>(PATH);
}

export function getTimeSeries(id: number): Promise<TimeSeriesSummary> {
  return api.get<TimeSeriesSummary>(`${PATH}/${id}`);
}

export function createTimeSeries(
  payload: CreateTimeSeriesPayload,
): Promise<TimeSeriesSummary> {
  return api.post<TimeSeriesSummary>(PATH, payload);
}

export function updateTimeSeries(
  id: number,
  payload: UpdateTimeSeriesPayload,
): Promise<TimeSeriesSummary> {
  return api.put<TimeSeriesSummary>(`${PATH}/${id}`, payload);
}

export function deleteTimeSeries(id: number): Promise<void> {
  return api.del(`${PATH}/${id}`);
}

export function listEntries(
  id: number,
  params: ListEntriesParams = {},
): Promise<TimeSeriesEntry[]> {
  const search = new URLSearchParams();
  if (params.from) search.set('from', params.from);
  if (params.to) search.set('to', params.to);
  if (params.limit != null) search.set('limit', String(params.limit));
  const query = search.toString();
  const suffix = query.length > 0 ? `?${query}` : '';
  return api.get<TimeSeriesEntry[]>(`${PATH}/${id}/entries${suffix}`);
}

export function addEntry(
  id: number,
  timestamp: string,
  value: number,
): Promise<TimeSeriesEntry> {
  return api.post<TimeSeriesEntry>(`${PATH}/${id}/entries`, { timestamp, value });
}

export function getLatestEntry(id: number): Promise<TimeSeriesEntry> {
  return api.get<TimeSeriesEntry>(`${PATH}/${id}/latest`);
}

export interface BulkImportResult {
  inserted: number;
  errors: { line: number; reason: string }[];
}

/**
 * Custom-Request, weil der Endpoint text/csv erwartet (nicht JSON wie sonst).
 * Liefert sowohl im Erfolgs- als auch im 400-Validierungsfall ein BulkImportResult —
 * der Aufrufer prueft die errors-Liste.
 */
export async function bulkImportCsv(
  id: number,
  csv: string,
): Promise<BulkImportResult> {
  const { getAccessToken, notifyAuthExpired } = await import('../auth/tokenBridge');
  const token = getAccessToken();
  const response = await fetch(`/api${PATH}/${id}/entries/bulk`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'text/csv',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: csv,
  });
  if (response.status === 401) {
    notifyAuthExpired();
  }
  if (response.status === 404) {
    return { inserted: 0, errors: [{ line: 0, reason: 'Zeitreihe nicht gefunden' }] };
  }
  return (await response.json()) as BulkImportResult;
}

export type Granularity = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface AggregateBucket {
  bucketStart: string;
  count: number;
  min: number;
  max: number;
  avg: number;
  last: number;
}

export function aggregateTimeSeries(
  id: number,
  granularity: Granularity,
  from?: string,
  to?: string,
  limit?: number,
): Promise<AggregateBucket[]> {
  const search = new URLSearchParams();
  search.set('granularity', granularity);
  if (from) search.set('from', from);
  if (to) search.set('to', to);
  if (limit != null) search.set('limit', String(limit));
  return api.get<AggregateBucket[]>(`${PATH}/${id}/aggregate?${search.toString()}`);
}
