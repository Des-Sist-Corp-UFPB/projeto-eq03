import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ForgotPassword } from '../ForgotPassword';
import api from '../../../services/api';

vi.mock('../../../services/api', () => ({
  default: {
    post: vi.fn(),
  },
}));

const renderPage = () =>
  render(
    <MemoryRouter>
      <ForgotPassword />
    </MemoryRouter>
  );

describe('ForgotPassword', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows a validation error when the email is invalid', async () => {
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('seuemail@exemplo.com'), {
      target: { value: 'not-an-email' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar link de redefinição/i }));

    await waitFor(() =>
      expect(screen.getByText('Formato de e-mail inválido')).toBeInTheDocument()
    );
    expect(api.post).not.toHaveBeenCalled();
  });

  it('submits the email and shows the generic success message', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: undefined });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('seuemail@exemplo.com'), {
      target: { value: 'user@salao.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar link de redefinição/i }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/auth/forgot-password', { email: 'user@salao.com' })
    );
    expect(await screen.findByText(/se esse e-mail estiver cadastrado/i)).toBeInTheDocument();
  });

  it('shows an error message when the request genuinely fails (network/server error)', async () => {
    vi.mocked(api.post).mockRejectedValue({ response: { status: 500 } });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('seuemail@exemplo.com'), {
      target: { value: 'user@salao.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar link de redefinição/i }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(
      await screen.findByText(/erro ao solicitar redefinição de senha/i)
    ).toBeInTheDocument();
  });
});
