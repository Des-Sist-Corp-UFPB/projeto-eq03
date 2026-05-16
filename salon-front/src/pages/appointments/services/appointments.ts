import api from '../../../services/api';

export interface TimeSlotResponse {
  time: string;
  available: boolean;
}

export interface AppointmentRequest {
  employeeId: number;
  serviceId: number;
  scheduledAt: string;
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
  scheduledAt: string;
  status: string;
}

export const appointmentsApi = {
  getSlots: async (date: string, employeeId: number) => {
    const { data } = await api.get<TimeSlotResponse[]>('/appointments/slots', {
      params: { date, employeeId }
    });
    return data;
  },

  create: async (request: AppointmentRequest) => {
    const { data } = await api.post<AppointmentResponse>('/appointments', request);
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
