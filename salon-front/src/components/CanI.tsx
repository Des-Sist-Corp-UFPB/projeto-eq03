import type { ReactNode } from 'react';
import { useCanI } from '../hooks/useCanI';

interface CanIProps {
  /** Permissão no formato "METHOD:/v1/endpoint", ex: "POST:/v1/users" */
  permission: string;
  /** Conteúdo renderizado apenas se o usuário tiver a permissão */
  children: ReactNode;
  /** Conteúdo de fallback renderizado quando o usuário NÃO tem a permissão (opcional) */
  fallback?: ReactNode;
}

/**
 * Componente <CanI> — Renderização condicional baseada em permissões do usuário.
 *
 * Uso:
 * ```tsx
 * <CanI permission="POST:/v1/users">
 *   <button>Criar Usuário</button>
 * </CanI>
 *
 * <CanI permission="DELETE:/v1/users/*" fallback={<span>Sem acesso</span>}>
 *   <button>Deletar</button>
 * </CanI>
 * ```
 *
 * A verificação é feita contra as permissões do banco (via /auth/me), nunca do JWT.
 */
export const CanI = ({ permission, children, fallback = null }: CanIProps) => {
  const hasPermission = useCanI(permission);
  return hasPermission ? <>{children}</> : <>{fallback}</>;
};
