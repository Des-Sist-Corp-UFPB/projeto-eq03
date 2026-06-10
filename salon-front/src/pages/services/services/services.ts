import api from '../../../services/api';

export interface SalonServiceData {
  id?: number;
  name: string;
  description: string;
  /** Opcional: referência &quot;a partir de&quot; */
  price?: number | null;
  /** Minutos opcionais — para cálculo de sobreposição na agenda */
  durationMin?: number | null;
  /** Ex.: &quot;Em média 50 min&quot;, &quot;Em média 1h30&quot; */
  durationEstimate?: string | null;
  active: boolean;
}

export function displayServiceDuration(
  s: Pick<SalonServiceData, 'durationEstimate' | 'durationMin'>
): string {
  const text = s.durationEstimate?.trim();
  if (text) return text;
  if (s.durationMin != null && s.durationMin > 0) return `Em média ${s.durationMin} min`;
  return 'Tempo a combinar';
}

export const salonServicesApi = {
  findAll: async (active?: boolean) => {
    const params = active !== undefined ? { active } : {};
    const { data } = await api.get<SalonServiceData[]>('/services', { params });
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
  },
};
