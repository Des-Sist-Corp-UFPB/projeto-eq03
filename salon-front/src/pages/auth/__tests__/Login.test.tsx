import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { Login } from '../Login';
import { useAuth } from '../../../hooks/useAuth';
import api from '../../../services/api';

vi.mock('jwt-decode', () => ({
  jwtDecode: vi.fn(),
}));

vi.mock('../../../services/api', () => ({
  default: {
    post: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

const renderLogin = () =>
  render(
    <MemoryRouter>
      <Login />
    </MemoryRouter>
  );

const submitLogin = async () => {
  fireEvent.change(screen.getByPlaceholderText('seuemail@exemplo.com'), {
    target: { value: 'user@salao.com' },
  });
  fireEvent.change(screen.getByPlaceholderText('Sua senha'), {
    target: { value: 'senha1234' },
  });
  fireEvent.click(screen.getByRole('button', { name: /entrar na minha conta/i }));
};

describe('Login redirect by role', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    vi.mocked(useAuth).mockReturnValue({
      login: vi.fn().mockResolvedValue(undefined),
    } as any);
    vi.mocked(api.post).mockResolvedValue({
      data: { accessToken: 'token', refreshToken: 'refresh' },
    });
  });

  it('sends SYSADMIN to /sysadmin/rbac', async () => {
    vi.mocked(jwtDecode).mockReturnValue({ role: 'SYSADMIN' } as any);
    renderLogin();
    await submitLogin();
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/sysadmin/rbac', { replace: true }));
  });

  it('sends ADMIN to their default admin section (/admin/reports)', async () => {
    vi.mocked(jwtDecode).mockReturnValue({ role: 'ADMIN' } as any);
    renderLogin();
    await submitLogin();
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/admin/reports', { replace: true }));
  });

  it('sends GERENTE_DE_ATENDIMENTO to their default admin section (/admin/reports)', async () => {
    vi.mocked(jwtDecode).mockReturnValue({ role: 'GERENTE_DE_ATENDIMENTO' } as any);
    renderLogin();
    await submitLogin();
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/admin/reports', { replace: true }));
  });

  it('sends FUNCIONARIA to /admin/appointments instead of the client area', async () => {
    vi.mocked(jwtDecode).mockReturnValue({ role: 'FUNCIONARIA' } as any);
    renderLogin();
    await submitLogin();
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/admin/appointments', { replace: true }));
  });

  it('sends CLIENTE to /my-appointments (the route that actually exists)', async () => {
    vi.mocked(jwtDecode).mockReturnValue({ role: 'CLIENTE' } as any);
    renderLogin();
    await submitLogin();
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/my-appointments', { replace: true }));
  });

  it('sends CLIENTE with a pending appointment to /appointment', async () => {
    localStorage.setItem('pending_appointment', '1');
    vi.mocked(jwtDecode).mockReturnValue({ role: 'CLIENTE' } as any);
    renderLogin();
    await submitLogin();
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/appointment', { replace: true }));
  });
});
