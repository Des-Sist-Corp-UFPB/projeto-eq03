import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, waitFor, customRender } from '../../../../test/test-utils';
import { EmailOutbox } from '../EmailOutbox';
import { emailOutboxApi } from '../services/emailOutbox';

vi.mock('../services/emailOutbox', () => ({
  emailOutboxApi: {
    findAll: vi.fn(),
    resend: vi.fn(),
  },
}));

vi.mock('../../../../hooks/useAlert', () => ({
  useAlert: () => ({
    error: vi.fn(),
    success: vi.fn(),
    alert: vi.fn(),
    confirm: vi.fn().mockResolvedValue(true),
  }),
}));

const mockList = [
  {
    id: 1,
    recipientEmail: 'cliente@example.com',
    subject: 'Seu Agendamento foi Confirmado!',
    status: 'SENT' as const,
    attempts: 1,
    relatedEntityType: 'Appointment',
    relatedEntityId: 42,
    createdAt: '2026-01-01T10:00:00',
    sentAt: '2026-01-01T10:00:01',
  },
  {
    id: 2,
    recipientEmail: 'falhou@example.com',
    subject: 'Agendamento Cancelado',
    status: 'FAILED' as const,
    attempts: 2,
    relatedEntityType: 'Appointment',
    relatedEntityId: 43,
    createdAt: '2026-01-01T11:00:00',
  },
];

const renderPage = () =>
  customRender(<EmailOutbox />, {
    user: { email: 'admin@salao.com', role: 'ADMIN', userId: 1, permissions: [] },
    isAuthenticated: true,
  });

describe('EmailOutbox', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (emailOutboxApi.findAll as ReturnType<typeof vi.fn>).mockResolvedValue({
      content: mockList,
      totalPages: 1,
      totalElements: 2,
      size: 20,
      number: 0,
    });
  });

  it('renders the list of recent email sends', async () => {
    renderPage();

    expect(await screen.findByText('cliente@example.com')).toBeInTheDocument();
    expect(screen.getByText('falhou@example.com')).toBeInTheDocument();
    // "Enviado" também aparece como opção do filtro de status — usa getAllByText.
    expect(screen.getAllByText('Enviado').length).toBeGreaterThan(0);
    expect(screen.getByText('Falhou (tentando de novo)')).toBeInTheDocument();
  });

  it('shows a resend button only for failed entries', async () => {
    renderPage();
    await screen.findByText('cliente@example.com');

    const resendButtons = screen.getAllByRole('button', { name: /reenviar/i });
    expect(resendButtons).toHaveLength(1);
  });

  it('resends a failed email and refreshes the list on success', async () => {
    (emailOutboxApi.resend as ReturnType<typeof vi.fn>).mockResolvedValue({
      ...mockList[1],
      status: 'SENT',
    });

    renderPage();
    await screen.findByText('falhou@example.com');

    fireEvent.click(screen.getByRole('button', { name: /reenviar/i }));

    await waitFor(() => expect(emailOutboxApi.resend).toHaveBeenCalledWith(2));
    await waitFor(() => expect(emailOutboxApi.findAll).toHaveBeenCalledTimes(2));
  });
});
