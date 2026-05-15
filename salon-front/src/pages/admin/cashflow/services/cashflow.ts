import api from '../../../services/api';

export interface CashFlowData {
  id?: number;
  type: 'INCOME' | 'EXPENSE';
  amount: number;
  description: string;
  date: string;
  appointmentId?: number | null;
}

export const cashFlowApi = {
  findByPeriod: async (from?: string, to?: string) => {
    const params: any = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const { data } = await api.get<CashFlowData[]>('/cashflow', { params });
    return data;
  },

  create: async (cashFlowData: CashFlowData) => {
    const { data } = await api.post<CashFlowData>('/cashflow', cashFlowData);
    return data;
  },

  delete: async (id: number) => {
    await api.delete(`/cashflow/${id}`);
  }
};
