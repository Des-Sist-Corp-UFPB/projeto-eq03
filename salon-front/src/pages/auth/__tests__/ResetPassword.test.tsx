import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ResetPassword } from '../ResetPassword';
import api from '../../../services/api';

vi.mock('../../../services/api', () => ({
  default: {
    post: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
let mockSearchParams = new URLSearchParams();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useSearchParams: () => [mockSearchParams],
  };
});

const renderPage = () =>
  render(
    <MemoryRouter>
      <ResetPassword />
    </MemoryRouter>
  );

const fillPasswords = (password: string, confirmPassword: string) => {
  fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres com 1 número'), {
    target: { value: password },
  });
  fireEvent.change(screen.getByPlaceholderText('Confirme sua nova senha'), {
    target: { value: confirmPassword },
  });
};

describe('ResetPassword', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchParams = new URLSearchParams({ token: 'raw-token' });
  });

  it('shows a warning and disables submit when there is no token in the URL', () => {
    mockSearchParams = new URLSearchParams();
    renderPage();

    expect(screen.getByText(/link de redefinição inválido ou incompleto/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /redefinir senha/i })).toBeDisabled();
  });

  it('shows a validation error when passwords do not match', async () => {
    renderPage();

    fillPasswords('NewPass123', 'DifferentPass123');
    fireEvent.click(screen.getByRole('button', { name: /redefinir senha/i }));

    await waitFor(() => expect(screen.getByText('As senhas não coincidem')).toBeInTheDocument());
    expect(api.post).not.toHaveBeenCalled();
  });

  it('submits the token and new password, then redirects to /login on success', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: undefined });
    renderPage();

    fillPasswords('NewPass123', 'NewPass123');
    fireEvent.click(screen.getByRole('button', { name: /redefinir senha/i }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/auth/reset-password', {
        token: 'raw-token',
        newPassword: 'NewPass123',
      })
    );
    await waitFor(() =>
      expect(mockNavigate).toHaveBeenCalledWith('/login', { state: { passwordResetSuccess: true } })
    );
  });

  it('shows an error message when the token is invalid or expired', async () => {
    vi.mocked(api.post).mockRejectedValue({
      response: { status: 400, data: { message: 'Link de redefinição inválido ou expirado.' } },
    });
    renderPage();

    fillPasswords('NewPass123', 'NewPass123');
    fireEvent.click(screen.getByRole('button', { name: /redefinir senha/i }));

    expect(
      await screen.findByText('Link de redefinição inválido ou expirado.')
    ).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
