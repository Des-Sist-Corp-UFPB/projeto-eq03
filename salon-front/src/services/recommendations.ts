import api from './api';

export type RecommendationType = 'FINANCEIRO' | 'RETENCAO';
export type RecommendationPriority = 'ALTA' | 'MEDIA' | 'BAIXA';

export interface RecommendationItem {
  title: string;
  description: string;
  suggestedAction: string;
  priority: RecommendationPriority;
}

export interface RecommendationResult {
  type: RecommendationType;
  items: RecommendationItem[];
  generatedAt: string;
  fromCache: boolean;
}

export const recommendationsService = {
  getLatest: async (type: RecommendationType): Promise<RecommendationResult> => {
    const response = await api.get<RecommendationResult>(`/admin/recommendations/${type}`);
    return response.data;
  },

  generate: async (type: RecommendationType): Promise<RecommendationResult> => {
    const response = await api.post<RecommendationResult>(`/admin/recommendations/${type}/generate`);
    return response.data;
  },
};
