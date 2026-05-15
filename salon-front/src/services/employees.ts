import api from './api';

export interface EmployeeData {
  id?: number;
  userId: number;
  name?: string;
  email?: string;
  bio?: string;
}

export const employeesApi = {
  findAll: async () => {
    const { data } = await api.get<EmployeeData[]>('/employees');
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
  }
};
