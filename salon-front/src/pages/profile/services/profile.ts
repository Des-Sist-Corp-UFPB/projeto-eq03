import api from '../../../services/api';
import type { UserData, UserUpdateRequest } from '../../admin/users/services/users';

export const profileApi = {
  getProfile: async () => {
    // Assuming GET /v1/users/me returns the current user's profile
    // Or we decode the JWT to get the ID and fetch the user.
    // For now, let's create a specific /v1/users/me or just use /v1/users/{id} if we know it.
    // The AuthContext provides the userId! We can pass it.
    throw new Error('Please pass the user ID');
  },
  
  getProfileById: async (id: number) => {
    const { data } = await api.get<UserData>(`/users/details/id/${id}`);
    return data;
  },

  updateProfile: async (id: number, userData: UserUpdateRequest) => {
    const { data } = await api.patch<UserData>(`/users/${id}`, userData);
    return data;
  }
};
