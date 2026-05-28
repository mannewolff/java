import { api } from './client';

export interface IngestTokenSummary {
  id: number;
  name: string;
  createdAt: string;
  lastUsedAt?: string;
  revoked: boolean;
}

export interface CreatedIngestToken {
  id: number;
  name: string;
  plaintext: string;
  createdAt: string;
}

const PATH = '/ingest-tokens';

export function listIngestTokens(): Promise<IngestTokenSummary[]> {
  return api.get<IngestTokenSummary[]>(PATH);
}

export function createIngestToken(name: string): Promise<CreatedIngestToken> {
  return api.post<CreatedIngestToken>(PATH, { name });
}

export function revokeIngestToken(id: number): Promise<void> {
  return api.del(`${PATH}/${id}`);
}
