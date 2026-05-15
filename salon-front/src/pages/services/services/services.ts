import api from '../../../services/api';

export interface ServiceData {
  id?: number;
  name: string;
  description: string;
  price: number;
  durationMin: number;
  active: boolean;
}

export const servicesApi = {
  findAll: async () => {
    const { data } = await api.get<ServiceData[]>('/services');
    return data;
  },

  findById: async (id: number) => {
    const { data } = await api.get<ServiceData>(`/services/${id}`);
    return data;
  },

  create: async (serviceData: ServiceData) => {
    const { data } = await api.post<ServiceData>('/services', serviceData);
    return data;
  },

  update: async (id: number, serviceData: ServiceData) => {
    const { data } = await api.put<ServiceData>(`/services/${id}`, serviceData);
    return data;
  },

  delete: async (id: number) => {
    await api.delete(`/services/${id}`);
  }
};
