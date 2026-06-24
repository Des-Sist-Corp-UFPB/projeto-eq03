import api from './api';

export interface PermissionItem {
  id: number;
  name: string;
  httpMethod: string;
  endpoint: string;
  classe: string;
}

export interface RoleWithPermissions {
  roleId: number;
  roleName: string;
  permissions: PermissionItem[];
}

export const rbacService = {
  /**
   * Retorna todos os roles com suas permissões associadas.
   */
  getAllRoles: async (): Promise<RoleWithPermissions[]> => {
    const { data } = await api.get<RoleWithPermissions[]>('/roles');
    return data;
  },

  /**
   * Retorna todas as permissões disponíveis no sistema.
   */
  getAllPermissions: async (): Promise<PermissionItem[]> => {
    const { data } = await api.get<PermissionItem[]>('/roles/permissions');
    return data;
  },

  /**
   * Concede uma permissão a um cargo.
   */
  grantPermission: async (roleId: number, permissionId: number): Promise<RoleWithPermissions> => {
    const { data } = await api.post<RoleWithPermissions>(`/roles/${roleId}/permissions/${permissionId}`);
    return data;
  },

  /**
   * Revoga uma permissão de um cargo.
   */
  revokePermission: async (roleId: number, permissionId: number): Promise<RoleWithPermissions> => {
    const { data } = await api.delete<RoleWithPermissions>(`/roles/${roleId}/permissions/${permissionId}`);
    return data;
  },
};
