import api from '../../../services/api';

export interface AppointmentRequestBody {
  employeeId: number;
  serviceId: number;
  /** Fluxo admin: horário já definido */
  scheduledAt?: string | null;
  /** Fluxo cliente: dia preferido */
  preferredDate?: string | null;
  clientNotes?: string | null;
  clientId?: number;
}

export interface AppointmentResponse {
  id: number;
  clientId: number;
  clientName: string;
  employeeId: number;
  employeeName: string;
  serviceId: number;
  serviceName: string;
  scheduledAt: string | null;
  preferredDate?: string | null;
  clientNotes?: string | null;
  status: string;
}

export const appointmentsApi = {
  create: async (request: AppointmentRequestBody) => {
    const { data } = await api.post<AppointmentResponse>('/appointments', request);
    return data;
  },

  confirm: async (id: number, scheduledAtIso: string) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/confirm`, {
      scheduledAt: scheduledAtIso
    });
    return data;
  },

  decline: async (id: number) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/decline`);
    return data;
  },

  getMyAppointments: async () => {
    const { data } = await api.get<AppointmentResponse[]>('/appointments/my');
    return data;
  },

  findAll: async () => {
    const { data } = await api.get<AppointmentResponse[]>('/appointments');
    return data;
  },

  cancel: async (id: number) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/cancel`);
    return data;
  },

  updateStatus: async (id: number, status: string) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/status`, null, {
      params: { status }
    });
    return data;
  }
};
