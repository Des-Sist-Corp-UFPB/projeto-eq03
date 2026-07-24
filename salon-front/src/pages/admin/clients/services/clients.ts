import api from '../../../../services/api';
import type { UserData } from '../../users/services/users';
import type { AppointmentResponse } from '../../../appointments/services/appointments';
import { normalizePage, type SpringPageResponse } from '../../../../utils/pagination';
export type { PageResponse } from '../../../../utils/pagination';

export interface ClientFilter {
  name?: string;
  email?: string;
  phone?: string;
  cpf?: string;
  active?: boolean;
}

export interface ClientDetailsResponse extends UserData {
  totalAppointments: number;
  lastAppointmentDate: string | null;
  appointments: AppointmentResponse[];
}

export const clientsApi = {
  findAll: async (filter: ClientFilter, page: number, size: number) => {
    const { data } = await api.get<SpringPageResponse<UserData>>('/clients', {
      params: {
        ...filter,
        page,
        size,
      },
    });
    return normalizePage(data);
  },

  findById: async (id: number) => {
    const { data } = await api.get<ClientDetailsResponse>(`/clients/${id}`);
    return data;
  },
};
