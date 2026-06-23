import { createContext, useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload, UserContextData } from '../types/auth';

interface AuthContextType {
  user: UserContextData | null;
  isAuthenticated: boolean;
  login: (accessToken: string, refreshToken: string, redirect?: boolean) => void;
  logout: () => void;
  updateUserCpf: (cpf: string) => void;
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
          cpf: null,
        });
      } catch {
        localStorage.removeItem('@Salon:token');
        localStorage.removeItem('@Salon:refreshToken');
      }
    }
    setIsLoading(false);
  }, []);

  const login = useCallback((accessToken: string, refreshToken: string, redirect = true) => {
    localStorage.setItem('@Salon:token', accessToken);
    localStorage.setItem('@Salon:refreshToken', refreshToken);

    const decoded = jwtDecode<JwtPayload>(accessToken);

    const userData: UserContextData = {
      email: decoded.sub,
      role: decoded.role,
      userId: decoded.userId,
      authorities: decoded.authorities || [],
      cpf: null,
    };

    setUser(userData);

    // Redirecionar admin/sysadmin para dashboard/feature-flags automaticamente
    if (redirect) {
      if (decoded.role === 'SYSADMIN') {
        window.location.href = '/sysadmin/feature-flags';
      } else if (decoded.role === 'ADMIN') {
        window.location.href = '/admin/reports';
      }
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('@Salon:token');
    localStorage.removeItem('@Salon:refreshToken');
    setUser(null);
    window.location.href = '/login';
  }, []);

  /**
   * Atualiza o CPF do usuário em memória sem recarregar a página.
   * Chamado após o fluxo JIT de coleta no PixPaymentModal.
   */
  const updateUserCpf = useCallback((cpf: string) => {
    setUser((prev) => (prev ? { ...prev, cpf } : prev));
  }, []);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout, updateUserCpf, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
};

