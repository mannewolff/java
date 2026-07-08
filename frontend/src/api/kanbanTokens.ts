import { api } from './client';

export interface KanbanTokenSummary {
  id: number;
  name: string;
  createdAt: string;
  lastUsedAt?: string;
  revoked: boolean;
}

export interface CreatedKanbanToken {
  id: number;
  name: string;
  plaintext: string;
  createdAt: string;
}

const PATH = '/kanban-tokens';

export function listKanbanTokens(): Promise<KanbanTokenSummary[]> {
  return api.get<KanbanTokenSummary[]>(PATH);
}

export function createKanbanToken(name: string): Promise<CreatedKanbanToken> {
  return api.post<CreatedKanbanToken>(PATH, { name });
}

export function revokeKanbanToken(id: number): Promise<void> {
  return api.del(`${PATH}/${id}`);
}
