import api from './api';

export interface AiConfigData {
  baseUrl: string;
  model: string;
  apiKeyMasked: string | null;
  apiKeyConfigured: boolean;
  temperature: number;
  maxTokens: number;
  enabled: boolean;
  dailyCallBudget: number;
  updatedBy: string | null;
  updatedAt: string | null;
}

export interface AiConfigUpdatePayload {
  baseUrl: string;
  model: string;
  apiKey?: string | null;
  temperature: number;
  maxTokens: number;
  enabled: boolean;
  dailyCallBudget: number;
}

export interface AiConfigTestPayload {
  baseUrl: string;
  model: string;
  apiKey?: string | null;
}

export interface AiConfigTestResult {
  success: boolean;
  message: string;
  latencyMs: number | null;
}

export const AI_MODELS = ['gpt-4o-mini', 'gpt-4o', 'gpt-4.1-mini'] as const;

export const aiConfigService = {
  get: async (): Promise<AiConfigData> => {
    const response = await api.get<AiConfigData>('/sysadmin/ai-config');
    return response.data;
  },

  update: async (payload: AiConfigUpdatePayload): Promise<AiConfigData> => {
    const response = await api.put<AiConfigData>('/sysadmin/ai-config', payload);
    return response.data;
  },

  testConnection: async (payload: AiConfigTestPayload): Promise<AiConfigTestResult> => {
    const response = await api.post<AiConfigTestResult>('/sysadmin/ai-config/test', payload);
    return response.data;
  },
};
