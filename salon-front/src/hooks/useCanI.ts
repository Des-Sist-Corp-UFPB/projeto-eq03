import { useAuth } from './useAuth';

/**
 * Hook useCanI — verifica se o usuário autenticado possui uma permissão específica.
 *
 * Uso: const canCreate = useCanI('POST:/v1/users');
 *
 * A verificação é feita contra a lista de permissões carregada do banco via /auth/me.
 * Retorna `true` para SYSADMIN e ADMIN por convenção (acesso total).
 */
export const useCanI = (permission: string): boolean => {
  const { user } = useAuth();

  if (!user) return false;

  // SYSADMIN e ADMIN têm acesso total por convenção
  if (user.role === 'SYSADMIN' || user.role === 'ADMIN') return true;

  return user.permissions.includes(permission);
};
