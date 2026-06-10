import api from '../../../../services/api';

export interface EmployeeData {
  id?: number;
  userId: number;
  name?: string;
  email?: string;
  bio?: string;
  remunerationType?: 'SALARIO_FIXO' | 'COMISSIONADO' | 'FIXO_E_COMISSIONADO';
  commissionScope?: 'INDIVIDUAL' | 'GLOBAL';
  remunerationValue?: number;
  commissionValue?: number;
}

export const employeesApi = {
  findAll: async () => {
    const { data } = await api.get<EmployeeData[]>('/employees');
    return data;
  },

  /** Lista funcionárias para o fluxo de agendamento público (sem expor email). */
  findAllForBooking: async () => {
    const { data } = await api.get<EmployeeData[]>('/employees/booking');
    return data;
  },

  findById: async (id: number) => {
    const { data } = await api.get<EmployeeData>(`/employees/${id}`);
    return data;
  },

  create: async (employeeData: EmployeeData) => {
    const { data } = await api.post<EmployeeData>('/employees', employeeData);
    return data;
  },

  update: async (id: number, employeeData: EmployeeData) => {
    const { data } = await api.put<EmployeeData>(`/employees/${id}`, employeeData);
    return data;
  },

  delete: async (id: number) => {
    await api.delete(`/employees/${id}`);
  },
};
