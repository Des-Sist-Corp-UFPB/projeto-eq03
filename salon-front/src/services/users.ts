import api from './api';

export interface UserData {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: string;
  active: boolean;
  createdAt: string;
}

export interface UserUpdateRequest {
  name?: string;
  email?: string;
  password?: string;
  phone?: string;
  active?: boolean;
  roleId?: number;
}

export const usersApi = {
  findAll: async () => {
    const { data } = await api.get<UserData[]>('/users');
    return data;
  },

  findById: async (id: number) => {
    const { data } = await api.get<UserData>(`/users/details/id/${id}`);
    return data;
  },

  update: async (id: number, updateData: UserUpdateRequest) => {
    const { data } = await api.patch<UserData>(`/users/${id}`, updateData);
    return data;
  },

  delete: async (id: number) => {
    await api.delete(`/users/${id}`);
  }
};
