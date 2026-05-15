import { useAuth } from './useAuth';

export const usePermission = (method: string, endpoint: string) => {
  const { user } = useAuth();

  if (!user) return false;
  if (user.role === 'ADMIN') return true;

  const requestAuthority = `${method.toUpperCase()}:${endpoint}`;

  return user.authorities.some((authority) => {
    if (authority === requestAuthority) return true;
    
    // Support for wildcard endpoints like GET:/v1/users/*
    const authParts = authority.split(':');
    if (authParts.length !== 2) return false;
    
    const [authMethod, authEndpoint] = authParts;
    
    if (authMethod !== '*' && authMethod !== method.toUpperCase()) return false;
    
    // Very basic wildcard match (can be improved to regex if needed)
    if (authEndpoint.endsWith('/*')) {
      const baseEndpoint = authEndpoint.replace('/*', '');
      return endpoint.startsWith(baseEndpoint);
    }
    
    return authEndpoint === endpoint;
  });
};
