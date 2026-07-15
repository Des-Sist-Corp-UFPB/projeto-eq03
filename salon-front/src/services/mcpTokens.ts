import api from './api';

export interface McpTokenData {
  id: number;
  name: string;
  createdBy: string;
  createdAt: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
  revoked: boolean;
}

export interface McpTokenGenerated {
  token: McpTokenData;
  rawValue: string;
}

export const mcpTokensService = {
  list: async (): Promise<McpTokenData[]> => {
    const response = await api.get<McpTokenData[]>('/sysadmin/ai-config/tokens');
    return response.data;
  },

  generate: async (name: string, expiresInDays: number | null): Promise<McpTokenGenerated> => {
    const response = await api.post<McpTokenGenerated>('/sysadmin/ai-config/tokens', {
      name,
      expiresInDays,
    });
    return response.data;
  },

  revoke: async (id: number): Promise<void> => {
    await api.delete(`/sysadmin/ai-config/tokens/${id}`);
  },
};
