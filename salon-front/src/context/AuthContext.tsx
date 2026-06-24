import { createContext, useState, useEffect, useCallback, useRef } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
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
  const navigate = useNavigate();

  // Ref para acessar navigate de forma estável dentro do event listener
  const navigateRef = useRef(navigate);
  useEffect(() => {
    navigateRef.current = navigate;
  }, [navigate]);

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

  const logout = useCallback(() => {
    localStorage.removeItem('@Salon:token');
    localStorage.removeItem('@Salon:refreshToken');
    setUser(null);
    navigateRef.current('/login');
  }, []);

  /**
   * Listener do Event Bus 'auth:logout' disparado pelo interceptor do Axios
   * quando um token expira e o refresh falha. Realiza o logout de forma
   * React-friendly (sem hard reload, sem window.location.href).
   */
  useEffect(() => {
    const handleAuthLogout = () => {
      logout();
    };

    window.addEventListener('auth:logout', handleAuthLogout);
    return () => {
      window.removeEventListener('auth:logout', handleAuthLogout);
    };
  }, [logout]);

  const login = useCallback((accessToken: string, refreshToken: string) => {
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
