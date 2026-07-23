import api from '../../../services/api';
import { normalizePage, type SpringPageResponse } from '../../../utils/pagination';
export type { PageResponse } from '../../../utils/pagination';

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
  paymentStatus?: string | null;
  paymentId?: number | null;
  pixQrCode?: string | null;
  clientHasSavedCpf?: boolean;
  clientCpfMasked?: string;
}

interface AppointmentCreatePayload {
  employeeId: number;
  serviceId: number;
  scheduledAt?: string | null;
  clientId?: number | null;
  preferredDate?: string | null;
  clientNotes?: string | null;
}

function buildCreatePayload(request: AppointmentRequestBody): AppointmentCreatePayload {
  const body: AppointmentCreatePayload = {
    employeeId: request.employeeId,
    serviceId: request.serviceId,
  };
  if (request.scheduledAt != null && String(request.scheduledAt).trim() !== '') {
    body.scheduledAt = request.scheduledAt;
  }
  if (request.clientId != null) {
    body.clientId = request.clientId;
  }
  if (request.preferredDate != null && String(request.preferredDate).trim() !== '') {
    body.preferredDate = request.preferredDate;
  }
  if (request.clientNotes != null && request.clientNotes.trim() !== '') {
    body.clientNotes = request.clientNotes.trim();
  }
  return body;
}

export interface GeneratePixRequest {
  useSavedCpf: boolean;
  cpf?: string;
}

export interface AppointmentFilter {
  status?: string;
  paymentStatus?: string;
  employeeId?: number;
  clientId?: number;
}

export const appointmentsApi = {
  create: async (request: AppointmentRequestBody) => {
    const { data } = await api.post<AppointmentResponse>(
      '/appointments',
      buildCreatePayload(request)
    );
    return data;
  },

  confirm: async (id: number, scheduledAtIso: string) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/confirm`, {
      scheduledAt: scheduledAtIso,
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

  findAll: async (filter: AppointmentFilter = {}, page = 0, size = 20) => {
    const { data } = await api.get<SpringPageResponse<AppointmentResponse>>('/appointments', {
      params: { ...filter, page, size },
    });
    return normalizePage(data);
  },

  cancel: async (id: number) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/cancel`);
    return data;
  },

  updateStatus: async (id: number, status: string) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/status`, null, {
      params: { status },
    });
    return data;
  },

  updatePaymentStatus: async (id: number, paymentStatus: string) => {
    const { data } = await api.patch<AppointmentResponse>(`/appointments/${id}/payment-status`, null, {
      params: { paymentStatus },
    });
    return data;
  },

  generatePix: async (id: number, payload: GeneratePixRequest) => {
    const { data } = await api.post<AppointmentResponse>(`/appointments/${id}/pix`, payload);
    return data;
  },

  findById: async (id: number) => {
    const { data } = await api.get<AppointmentResponse>(`/appointments/${id}`);
    return data;
  },
};
