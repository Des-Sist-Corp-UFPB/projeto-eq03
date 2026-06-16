import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, customRender } from '../../../../test/test-utils';
import { AuditLog } from '../AuditLog';
import api from '../../../../services/api';

vi.mock('../../../../services/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

vi.mock('../../../../hooks/useAlert', () => ({
  useAlert: () => ({
    error: vi.fn(),
    success: vi.fn(),
    alert: vi.fn(),
    confirm: vi.fn(),
  }),
}));

const mockAuditLogs = {
  content: [
    {
      id: 1,
      userId: 10,
      userEmail: 'admin@salao.com',
      action: 'CREATE',
      entityType: 'Product',
      entityId: 5,
      details: '{"name": "Shampoo", "price": 25.5}',
      status: 'SUCCESS',
      createdAt: '2026-06-16T15:00:00Z',
      ipAddress: '127.0.0.1',
    },
  ],
  totalElements: 1,
};

describe('AuditLog Page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.get).mockResolvedValue({ data: mockAuditLogs });
  });

  it('renders table and handles search input changes and submit', async () => {
    await act(async () => {
      customRender(<AuditLog />);
    });

    expect(screen.getByText(/Logs de Auditoria/)).toBeInTheDocument();
    expect(screen.getByText('Admin')).toBeInTheDocument();
    expect(screen.getAllByText('CREATE')[0]).toBeInTheDocument();
    expect(screen.getAllByText('Product')[0]).toBeInTheDocument();

    const idInput = screen.getByLabelText('ID do Usuário');
    const actionSelect = screen.getByLabelText('Ação');
    const entitySelect = screen.getByLabelText('Entidade');
    const startInput = screen.getByLabelText('Data Inicial');
    const endInput = screen.getByLabelText('Data Final');

    fireEvent.change(idInput, { target: { value: '10' } });
    fireEvent.change(actionSelect, { target: { value: 'CREATE' } });
    fireEvent.change(entitySelect, { target: { value: 'Product' } });
    fireEvent.change(startInput, { target: { value: '2026-06-16' } });
    fireEvent.change(endInput, { target: { value: '2026-06-17' } });

    const filterBtn = screen.getByRole('button', { name: 'Filtrar' });
    await act(async () => {
      fireEvent.click(filterBtn);
    });

    expect(api.get).toHaveBeenCalledWith(expect.stringContaining('userId=10'));
    expect(api.get).toHaveBeenCalledWith(expect.stringContaining('action=CREATE'));
    expect(api.get).toHaveBeenCalledWith(expect.stringContaining('entityType=Product'));
    expect(api.get).toHaveBeenCalledWith(expect.stringContaining('startDate=2026-06-16'));
    expect(api.get).toHaveBeenCalledWith(expect.stringContaining('endDate=2026-06-17'));
  });

  it('opens details modal and displays prettified JSON', async () => {
    await act(async () => {
      customRender(<AuditLog />);
    });

    const detailsBtn = screen.getByRole('button', { name: 'Detalhes' });
    await act(async () => {
      fireEvent.click(detailsBtn);
    });

    expect(screen.getByText(/Detalhes do Log/)).toBeInTheDocument();
    expect(screen.getByText('Resumo do Payload / Detalhes')).toBeInTheDocument();
    expect(screen.getAllByText(/"name"/)[0]).toBeInTheDocument();
    expect(screen.getAllByText(/"Shampoo"/)[0]).toBeInTheDocument();

    const closeBtn = screen.getByRole('button', { name: 'Fechar Detalhes' });
    await act(async () => {
      fireEvent.click(closeBtn);
    });

    expect(screen.queryByText(/Detalhes do Log/)).not.toBeInTheDocument();
  });

  it('clears filters when clicking Limpar', async () => {
    await act(async () => {
      customRender(<AuditLog />);
    });

    const idInput = screen.getByLabelText('ID do Usuário');
    fireEvent.change(idInput, { target: { value: '10' } });

    const clearBtn = screen.getByRole('button', { name: 'Limpar' });
    await act(async () => {
      fireEvent.click(clearBtn);
    });

    expect(idInput).toHaveValue(null);
  });
});
