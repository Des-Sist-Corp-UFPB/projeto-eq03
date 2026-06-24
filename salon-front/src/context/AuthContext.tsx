import { createContext, useState, useEffect, useCallback, useRef } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload, UserContextData, UserProfileResponse } from '../types/auth';
import api from '../services/api';

interface AuthContextType {
  user: UserContextData | null;
  isAuthenticated: boolean;
  login: (accessToken: string, refreshToken: string, redirect?: boolean) => Promise<void>;
  logout: () => void;
  updateUserCpf: (cpf: string) => void;
  isLoading: boolean;
}

export const AuthContext = createContext<AuthContextType>({} as AuthContextType);

/**
 * Busca o perfil completo do usuário (com permissões do banco) via GET /v1/auth/me.
 * Retorna null se não autenticado ou em caso de erro.
 */
async function fetchUserProfile(): Promise<UserProfileResponse | null> {
  try {
    const { data } = await api.get<UserProfileResponse>('/auth/me');
    return data;
  } catch {
    return null;
  }
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<UserContextData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  // Ref para acessar navigate de forma estável dentro do event listener
  const navigateRef = useRef(navigate);
  useEffect(() => {
    navigateRef.current = navigate;
  }, [navigate]);

  /**
   * Inicialização: se houver token salvo, decodifica para identidade básica e
   * faz GET /auth/me para buscar permissões completas do banco de dados.
   */
  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('@Salon:token');

      if (token) {
        try {
          const decoded = jwtDecode<JwtPayload>(token);

          // Seta o usuário com identidade básica do token (sem permissões ainda)
          // para que a navegação de rota (role-based) funcione imediatamente.
          const baseUser: UserContextData = {
            email: decoded.sub,
            role: decoded.role,
            userId: decoded.userId,
            permissions: [],
            cpf: null,
          };
          setUser(baseUser);

          // Busca permissões completas do banco de dados de forma assíncrona.
          const profile = await fetchUserProfile();
          if (profile) {
            setUser({
              email: profile.email,
              role: profile.role,
              userId: profile.userId,
              permissions: profile.permissions,
              cpf: profile.cpf,
            });
          }
        } catch {
          localStorage.removeItem('@Salon:token');
          localStorage.removeItem('@Salon:refreshToken');
        }
      }
      setIsLoading(false);
    };

    initAuth();
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

  /**
   * Login: salva tokens, popula identidade básica do JWT e dispara fetch
   * assíncrono para buscar permissões completas do banco.
   */
  const login = useCallback(async (accessToken: string, refreshToken: string) => {
    localStorage.setItem('@Salon:token', accessToken);
    localStorage.setItem('@Salon:refreshToken', refreshToken);

    const decoded = jwtDecode<JwtPayload>(accessToken);

    // Identidade básica imediata
    setUser({
      email: decoded.sub,
      role: decoded.role,
      userId: decoded.userId,
      permissions: [],
      cpf: null,
    });

    // Busca permissões completas do banco
    const profile = await fetchUserProfile();
    if (profile) {
      setUser({
        email: profile.email,
        role: profile.role,
        userId: profile.userId,
        permissions: profile.permissions,
        cpf: profile.cpf,
      });
    }
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
