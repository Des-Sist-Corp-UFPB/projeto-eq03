import api from '../../../../services/api';

export interface FinancialReportResponse {
  totalIncome: number;
  totalExpense: number;
  netProfit: number;
  period: string;
}

export interface AppointmentReportResponse {
  totalAppointments: number;
  pending: number;
  confirmed: number;
  done: number;
  cancelled: number;
  byEmployee: Record<string, number>;
  byService: Record<string, number>;
  period: string;
}

export const reportsApi = {
  getFinancialReport: async (from?: string, to?: string) => {
    const params: Record<string, string> = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const { data } = await api.get<FinancialReportResponse>('/reports/financial', { params });
    return data;
  },

  getAppointmentReport: async (from?: string, to?: string) => {
    const params: Record<string, string> = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const { data } = await api.get<AppointmentReportResponse>('/reports/appointments', { params });
    return data;
  }
};
