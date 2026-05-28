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

export interface UpdateTimeSeriesPayload extends CreateTimeSeriesPayload {}

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
): Promise<AggregateBucket[]> {
  const search = new URLSearchParams();
  search.set('granularity', granularity);
  if (from) search.set('from', from);
  if (to) search.set('to', to);
  return api.get<AggregateBucket[]>(`${PATH}/${id}/aggregate?${search.toString()}`);
}
