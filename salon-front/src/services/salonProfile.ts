import api from './api';

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface BusinessHourData {
  dayOfWeek: DayOfWeek;
  open: boolean;
  openTime: string | null;
  closeTime: string | null;
}

/** GET /v1/salon/profile é público — nenhum campo aqui é sensível. */
export interface SalonProfileData {
  id: number;
  name: string;
  description: string | null;
  address: string | null;
  phone: string | null;
  instagram: string | null;
  whatsapp: string | null;
  logoUrl: string | null;
  updatedAt: string | null;
  businessHours: BusinessHourData[];
}

export interface SalonProfileUpdatePayload {
  name: string;
  description?: string | null;
  address?: string | null;
  phone?: string | null;
  instagram?: string | null;
  whatsapp?: string | null;
  logoUrl?: string | null;
  businessHours: BusinessHourData[];
}

export const DAY_ORDER: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Segunda-feira',
  TUESDAY: 'Terça-feira',
  WEDNESDAY: 'Quarta-feira',
  THURSDAY: 'Quinta-feira',
  FRIDAY: 'Sexta-feira',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

export const salonProfileService = {
  getPublic: async () => {
    const { data } = await api.get<SalonProfileData>('/salon/profile');
    return data;
  },

  update: async (payload: SalonProfileUpdatePayload) => {
    const { data } = await api.put<SalonProfileData>('/admin/salon/profile', payload);
    return data;
  },
};
