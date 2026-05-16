import api from '../../../services/api';

export interface SalonServiceData {
  id?: number;
  name: string;
  description: string;
  /** Opcional: referência &quot;a partir de&quot; */
  price?: number | null;
  durationMin: number;
  active: boolean;
}

export const salonServicesApi = {
  findAll: async () => {
    const { data } = await api.get<SalonServiceData[]>('/services');
    return data;
  },

  findById: async (id: number) => {
    const { data } = await api.get<SalonServiceData>(`/services/${id}`);
    return data;
  },

  create: async (salonServiceData: SalonServiceData) => {
    const { data } = await api.post<SalonServiceData>('/services', salonServiceData);
    return data;
  },

  update: async (id: number, salonServiceData: SalonServiceData) => {
    const { data } = await api.put<SalonServiceData>(`/services/${id}`, salonServiceData);
    return data;
  },

  delete: async (id: number) => {
    await api.delete(`/services/${id}`);
  }
};
