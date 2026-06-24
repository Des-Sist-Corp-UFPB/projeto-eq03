import api from '../../../../services/api';
import type { UserData } from '../../users/services/users';
import type { AppointmentResponse } from '../../../appointments/services/appointments';

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

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export const clientsApi = {
  findAll: async (filter: ClientFilter, page: number, size: number) => {
    const { data } = await api.get<PageResponse<UserData>>('/clients', {
      params: {
        ...filter,
        page,
        size,
      },
    });
    return data;
  },

  findById: async (id: number) => {
    const { data } = await api.get<ClientDetailsResponse>(`/clients/${id}`);
    return data;
  },
};
