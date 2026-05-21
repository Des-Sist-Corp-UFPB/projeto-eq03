import api from './api';

export interface FeatureFlag {
  name: string;
  enabled: boolean;
  description: string;
}

export const featureFlagsService = {
  getPublicFlags: async (): Promise<FeatureFlag[]> => {
    const response = await api.get<FeatureFlag[]>('/feature-flags');
    return response.data;
  },

  getAllFlags: async (): Promise<FeatureFlag[]> => {
    const response = await api.get<FeatureFlag[]>('/sysadmin/feature-flags');
    return response.data;
  },

  toggleFlag: async (name: string): Promise<FeatureFlag> => {
    const response = await api.patch<FeatureFlag>(`/sysadmin/feature-flags/${name}/toggle`);
    return response.data;
  },
};
