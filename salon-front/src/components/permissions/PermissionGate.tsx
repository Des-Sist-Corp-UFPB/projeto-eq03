import { ReactNode } from 'react';
import { usePermission } from '../../hooks/usePermission';

interface PermissionGateProps {
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | '*';
  endpoint: string;
  children: ReactNode;
  fallback?: ReactNode;
}

export const PermissionGate = ({ method, endpoint, children, fallback = null }: PermissionGateProps) => {
  const hasPermission = usePermission(method, endpoint);

  if (!hasPermission) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};
