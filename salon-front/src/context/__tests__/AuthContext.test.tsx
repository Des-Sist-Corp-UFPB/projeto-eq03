import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { useContext } from 'react';
import { AuthContext, AuthProvider } from '../AuthContext';
import { jwtDecode } from 'jwt-decode';

vi.mock('jwt-decode', () => ({
  jwtDecode: vi.fn(),
}));

const TestComponent = ({ loginParams }: { loginParams?: [string, string, boolean?] }) => {
  const { user, isAuthenticated, login, logout, isLoading } = useContext(AuthContext);

  if (isLoading) return <div>Loading...</div>;

  return (
    <div>
      <span data-testid="auth-status">
        {isAuthenticated ? 'Authenticated' : 'Not Authenticated'}
      </span>
      {user && <span data-testid="user-email">{user.email}</span>}
      {user && <span data-testid="user-role">{user.role}</span>}
      {user && <span data-testid="user-authorities">{JSON.stringify(user.authorities)}</span>}
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
    // Mock window.location
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

  it('should initialize without user if no token in localStorage', () => {
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    expect(screen.queryByTestId('user-email')).not.toBeInTheDocument();
  });

  it('should restore user on mount if token exists in localStorage', () => {
    localStorage.setItem('@Salon:token', 'valid-token');
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
      authorities: ['GET:/v1/users'],
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    expect(screen.getByTestId('user-email')).toHaveTextContent('user@salao.com');
    expect(screen.getByTestId('user-role')).toHaveTextContent('CLIENTE');
  });

  it('should clear localStorage if token is invalid on mount', () => {
    localStorage.setItem('@Salon:token', 'invalid-token');
    localStorage.setItem('@Salon:refreshToken', 'invalid-refresh');
    vi.mocked(jwtDecode).mockImplementation(() => {
      throw new Error('invalid token');
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    expect(localStorage.getItem('@Salon:token')).toBeNull();
    expect(localStorage.getItem('@Salon:refreshToken')).toBeNull();
  });

  it('should handle login and update state and localStorage', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'client@salao.com',
      role: 'CLIENTE',
      userId: 5,
      authorities: [],
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    expect(localStorage.getItem('@Salon:token')).toBe('mockAccessToken');
    expect(localStorage.getItem('@Salon:refreshToken')).toBe('mockRefreshToken');
    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    expect(screen.getByTestId('user-email')).toHaveTextContent('client@salao.com');
  });

  it('should redirect sysadmin to sysadmin page on login', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'sysadmin@salao.com',
      role: 'SYSADMIN',
      userId: 1,
      authorities: [],
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    expect(window.location.href).toBe('/sysadmin/feature-flags');
  });

  it('should redirect admin to admin page on login', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'admin@salao.com',
      role: 'ADMIN',
      userId: 2,
      authorities: [],
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    expect(window.location.href).toBe('/admin/reports');
  });

  it('should clear state, localStorage, and redirect on logout', async () => {
    localStorage.setItem('@Salon:token', 'valid-token');
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
      authorities: [],
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');

    const logoutButton = screen.getByTestId('logout-btn');
    await act(async () => {
      logoutButton.click();
    });

    expect(localStorage.getItem('@Salon:token')).toBeNull();
    expect(localStorage.getItem('@Salon:refreshToken')).toBeNull();
    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    expect(window.location.href).toBe('/login');
  });

  it('should default authorities to empty array if not present on mount', () => {
    localStorage.setItem('@Salon:token', 'valid-token');
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('user-authorities')).toHaveTextContent('[]');
  });

  it('should default authorities to empty array if not present on login and respect redirect false', async () => {
    vi.mocked(jwtDecode).mockReturnValue({
      sub: 'user@salao.com',
      role: 'CLIENTE',
      userId: 10,
    });

    render(
      <AuthProvider>
        <TestComponent loginParams={['mockAccessToken', 'mockRefreshToken', false]} />
      </AuthProvider>
    );

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    expect(screen.getByTestId('user-authorities')).toHaveTextContent('[]');
    expect(window.location.href).not.toBe('/admin/reports');
    expect(window.location.href).not.toBe('/sysadmin/feature-flags');
  });
});
