import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { useContext } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext, AuthProvider } from '../AuthContext';
import { jwtDecode } from 'jwt-decode';

vi.mock('jwt-decode', () => ({
  jwtDecode: vi.fn(),
}));

// Mock do api por padrão — retorna perfil genérico; testes específicos podem sobrescrever
const mockApiGet = vi.fn().mockResolvedValue({
  data: {
    userId: 10,
    email: 'user@salao.com',
    name: 'Test User',
    role: 'CLIENTE',
    cpf: null,
    permissions: [],
  },
});

vi.mock('../../services/api', () => ({
  default: {
    get: (...args: any[]) => mockApiGet(...args),
  },
}));

// Wrapper para fornecer o contexto de roteamento (necessário para useNavigate no AuthProvider)
const RouterWrapper = ({ children }: { children: React.ReactNode }) => (
  <MemoryRouter>{children}</MemoryRouter>
);

const TestComponent = ({ loginParams }: { loginParams?: [string, string] }) => {
  const { user, isAuthenticated, login, logout, isLoading } = useContext(AuthContext);

  if (isLoading) return <div>Loading...</div>;

  return (
    <div>
      <span data-testid="auth-status">
        {isAuthenticated ? 'Authenticated' : 'Not Authenticated'}
      </span>
      {user && <span data-testid="user-email">{user.email}</span>}
      {user && <span data-testid="user-role">{user.role}</span>}
      {user && <span data-testid="user-permissions">{JSON.stringify(user.permissions)}</span>}
      <button
        onClick={() => {
          if (loginParams) {
            login(...loginParams);
          } else {
            login('mockAccessToken', 'mockRefreshToken');
          }
        }}
        data-testid="login-btn"
      >
        Login
      </button>
      <button onClick={() => logout()} data-testid="logout-btn">
        Logout
      </button>
    </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    // Mock window.location para testes que ainda verificam a URL
    const originalLocation = window.location;
    delete (window as any).location;
    window.location = {
      ...originalLocation,
      href: '',
      assign: vi.fn(),
      replace: vi.fn(),
    } as any;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should initialize without user if no token in localStorage', async () => {
    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    expect(screen.queryByTestId('user-email')).not.toBeInTheDocument();
  });

  it('should restore user on mount if token exists in localStorage', async () => {
    localStorage.setItem('@Salon:token', 'valid-token');
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    expect(screen.getByTestId('user-email')).toHaveTextContent('user@salao.com');
    expect(screen.getByTestId('user-role')).toHaveTextContent('CLIENTE');
  });

  it('should clear localStorage if token is invalid on mount', async () => {
    localStorage.setItem('@Salon:token', 'invalid-token');
    localStorage.setItem('@Salon:refreshToken', 'invalid-refresh');
    vi.mocked(jwtDecode).mockImplementation(() => {
      throw new Error('invalid token');
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    expect(localStorage.getItem('@Salon:token')).toBeNull();
    expect(localStorage.getItem('@Salon:refreshToken')).toBeNull();
  });

  it('should handle login and update state and localStorage', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'client@salao.com',
      role: 'CLIENTE',
      userId: 5,
    });
    mockApiGet.mockResolvedValueOnce({
      data: { userId: 5, email: 'client@salao.com', name: 'Client', role: 'CLIENTE', cpf: null, permissions: [] },
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    expect(localStorage.getItem('@Salon:token')).toBe('mockAccessToken');
    expect(localStorage.getItem('@Salon:refreshToken')).toBe('mockRefreshToken');
    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    expect(screen.getByTestId('user-email')).toHaveTextContent('client@salao.com');
  });

  it('should set user state for sysadmin on login (redirection handled by Login.tsx)', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'sysadmin@salao.com',
      role: 'SYSADMIN',
      userId: 1,
    });
    mockApiGet.mockResolvedValueOnce({
      data: { userId: 1, email: 'sysadmin@salao.com', name: 'Sysadmin', role: 'SYSADMIN', cpf: null, permissions: [] },
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    // O redirecionamento agora é responsabilidade do Login.tsx, não do AuthContext
    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    expect(screen.getByTestId('user-role')).toHaveTextContent('SYSADMIN');
  });

  it('should set user state for admin on login (redirection handled by Login.tsx)', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'admin@salao.com',
      role: 'ADMIN',
      userId: 2,
    });
    mockApiGet.mockResolvedValueOnce({
      data: { userId: 2, email: 'admin@salao.com', name: 'Admin', role: 'ADMIN', cpf: null, permissions: [] },
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    // O redirecionamento agora é responsabilidade do Login.tsx, não do AuthContext
    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    expect(screen.getByTestId('user-role')).toHaveTextContent('ADMIN');
  });

  it('should clear state and localStorage on logout', async () => {
    localStorage.setItem('@Salon:token', 'valid-token');
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');

    const logoutButton = screen.getByTestId('logout-btn');
    await act(async () => {
      logoutButton.click();
    });

    expect(localStorage.getItem('@Salon:token')).toBeNull();
    expect(localStorage.getItem('@Salon:refreshToken')).toBeNull();
    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
  });

  it('should default permissions to empty array before /auth/me responds', async () => {
    localStorage.setItem('@Salon:token', 'valid-token');
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    // Após o init completo, as permissões devem estar carregadas (mockadas como [])
    expect(screen.getByTestId('user-permissions')).toHaveTextContent('[]');
  });

  it('should default permissions to empty array if not present on login', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent loginParams={['mockAccessToken', 'mockRefreshToken']} />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    expect(screen.getByTestId('user-permissions')).toHaveTextContent('[]');
  });

  it('should trigger logout when auth:logout event is dispatched', async () => {
    localStorage.setItem('@Salon:token', 'valid-token');
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
    });

    await act(async () => {
      render(
        <RouterWrapper>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </RouterWrapper>
      );
    });

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');

    await act(async () => {
      window.dispatchEvent(new CustomEvent('auth:logout'));
    });

    expect(localStorage.getItem('@Salon:token')).toBeNull();
    expect(localStorage.getItem('@Salon:refreshToken')).toBeNull();
    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
  });
});
