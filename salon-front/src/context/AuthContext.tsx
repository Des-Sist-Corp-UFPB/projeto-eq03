import { createContext, useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload, UserContextData } from '../types/auth';

interface AuthContextType {
  user: UserContextData | null;
  isAuthenticated: boolean;
  login: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
  isLoading: boolean;
}

export const AuthContext = createContext<AuthContextType>({} as AuthContextType);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<UserContextData | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('@Salon:token');

    if (token) {
      try {
        const decoded = jwtDecode<JwtPayload>(token);
        setUser({
          email: decoded.sub,
          role: decoded.role,
          userId: decoded.userId,
          authorities: decoded.authorities || [],
        });
      } catch {
        localStorage.removeItem('@Salon:token');
        localStorage.removeItem('@Salon:refreshToken');
      }
    }
    setIsLoading(false);
  }, []);

  const login = useCallback((accessToken: string, refreshToken: string) => {
    localStorage.setItem('@Salon:token', accessToken);
    localStorage.setItem('@Salon:refreshToken', refreshToken);

    const decoded = jwtDecode<JwtPayload>(accessToken);
    
    setUser({
      email: decoded.sub,
      role: decoded.role,
      userId: decoded.userId,
      authorities: decoded.authorities || [],
    });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('@Salon:token');
    localStorage.removeItem('@Salon:refreshToken');
    setUser(null);
    window.location.href = '/login';
  }, []);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
};
