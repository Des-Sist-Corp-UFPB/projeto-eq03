import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, customRender } from '../../../../test/test-utils';
import { AdminAppointments } from '../AdminAppointments';
import { appointmentsApi } from '../../../appointments/services/appointments';
import { salonServicesApi } from '../../../services/services/services';
import { employeesApi } from '../../employees/services/employees';
import { usersApi } from '../../users/services/users';

vi.mock('../../../../hooks/usePermission', () => ({
  usePermission: () => true,
}));

vi.mock('../../../appointments/services/appointments', () => ({
  appointmentsApi: {
    findAll: vi.fn(),
    confirm: vi.fn(),
    decline: vi.fn(),
    cancel: vi.fn(),
    updateStatus: vi.fn(),
    updatePaymentStatus: vi.fn(),
    generatePix: vi.fn(),
  },
}));

vi.mock('../../../services/services/services', () => ({
  salonServicesApi: {
    findAll: vi.fn(),
  },
}));

vi.mock('../../employees/services/employees', () => ({
  employeesApi: {
    findAll: vi.fn(),
  },
}));

vi.mock('../../users/services/users', () => ({
  usersApi: {
    findAll: vi.fn(),
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

const mockAppointments = [
  {
    id: 1,
    clientId: 5,
    clientName: 'Elksandro',
    employeeId: 10,
    employeeName: 'Mariana',
    serviceId: 100,
    serviceName: 'Corte de Cabelo',
    scheduledAt: '2026-06-25T14:00:00Z',
    status: 'CONFIRMED',
    paymentStatus: 'PENDING',
    pixQrCode: null,
  },
  {
    id: 2,
    clientId: 6,
    clientName: 'Joao',
    employeeId: 10,
    employeeName: 'Mariana',
    serviceId: 101,
    serviceName: 'Manicure',
    scheduledAt: null,
    preferredDate: '2026-06-26',
    status: 'REQUESTED',
    paymentStatus: null,
    pixQrCode: null,
  },
];

const mockServices = [
  { id: 100, name: 'Corte de Cabelo', price: 85.0, active: true, description: '' },
  { id: 101, name: 'Manicure', price: 40.0, active: true, description: '' },
];

const mockEmployees = [
  { id: 10, userId: 100, name: 'Mariana', active: true },
];

const mockUsers = [
  { id: 5, name: 'Elksandro', role: 'CLIENTE', email: 'elksandro@salao.com', phone: '999999999', active: true, createdAt: '2026-06-16T15:00:00Z' },
  { id: 6, name: 'Joao', role: 'CLIENTE', email: 'joao@salao.com', phone: '999999999', active: true, createdAt: '2026-06-16T15:00:00Z' },
];

describe('AdminAppointments Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(appointmentsApi.findAll).mockResolvedValue(mockAppointments);
    vi.mocked(salonServicesApi.findAll).mockResolvedValue(mockServices);
    vi.mocked(employeesApi.findAll).mockResolvedValue(mockEmployees);
    vi.mocked(usersApi.findAll).mockResolvedValue(mockUsers);
    
    vi.mocked(appointmentsApi.generatePix).mockResolvedValue({
      id: 1,
      clientId: 5,
      clientName: 'Elksandro',
      employeeId: 10,
      employeeName: 'Mariana',
      serviceId: 100,
      serviceName: 'Corte de Cabelo',
      scheduledAt: '2026-06-25T14:00:00Z',
      status: 'CONFIRMED',
      paymentStatus: 'PENDING',
      pixQrCode: 'pix-generated-code-admin-1',
    });
  });

  it('renders the appointments table and lists all entries', async () => {
    await act(async () => {
      customRender(<AdminAppointments />);
    });

    expect(screen.getByText('Agendamentos (Admin)')).toBeInTheDocument();
    expect(screen.getByText('Elksandro')).toBeInTheDocument();
    expect(screen.getByText('Joao')).toBeInTheDocument();
    expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument();
    expect(screen.getByText('Manicure')).toBeInTheDocument();
  });

  it('triggers updateStatus when the appointment status select is changed', async () => {
    await act(async () => {
      customRender(<AdminAppointments />);
    });

    const selects = screen.getAllByRole('combobox');
    
    // Row 0 is Joao (REQUESTED): renders paymentStatus select (index 0, value PENDING)
    // Row 1 is Elksandro (CONFIRMED): renders status select (index 1) and paymentStatus select (index 2)
    const appStatusSelect = selects[1];
    expect(appStatusSelect).toHaveValue('CONFIRMED');

    await act(async () => {
      fireEvent.change(appStatusSelect, { target: { value: 'DONE' } });
    });

    expect(appointmentsApi.updateStatus).toHaveBeenCalledWith(1, 'DONE');
  });

  it('triggers updatePaymentStatus when the payment status select is changed', async () => {
    await act(async () => {
      customRender(<AdminAppointments />);
    });

    const selects = screen.getAllByRole('combobox');
    
    // Row 1 is Elksandro (CONFIRMED): paymentStatus select is index 2
    const paymentStatusSelect = selects[2];
    expect(paymentStatusSelect).toHaveValue('PENDING');

    await act(async () => {
      fireEvent.change(paymentStatusSelect, { target: { value: 'PAID' } });
    });

    expect(appointmentsApi.updatePaymentStatus).toHaveBeenCalledWith(1, 'PAID');
  });

  it('triggers generatePix when clicking Pagar com PIX', async () => {
    await act(async () => {
      customRender(<AdminAppointments />);
    });

    const payBtn = screen.getByRole('button', { name: 'Pagar com PIX' });
    expect(payBtn).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(payBtn);
    });

    expect(appointmentsApi.generatePix).toHaveBeenCalledWith(1);
    
    // Check if the modal opens
    expect(screen.getByText('Pagamento via PIX')).toBeInTheDocument();
    expect(screen.getAllByText('Corte de Cabelo')).toHaveLength(2); // One in table, one in modal
  });

  it('opens confirmation modal and cancels appointment when cancel button is clicked', async () => {
    vi.mocked(appointmentsApi.cancel).mockResolvedValue({} as any);

    await act(async () => {
      customRender(<AdminAppointments />);
    });

    const cancelButtons = screen.getAllByRole('button', { name: 'Cancelar' });
    expect(cancelButtons.length).toBeGreaterThan(0);

    // cancelButtons[0] belongs to Row 0 (Joao, ID 2)
    fireEvent.click(cancelButtons[0]);

    // Check confirm modal
    expect(screen.getByText('Cancelar Agendamento')).toBeInTheDocument();
    
    const confirmBtn = screen.getByRole('button', { name: 'Confirmar' });
    await act(async () => {
      fireEvent.click(confirmBtn);
    });

    expect(appointmentsApi.cancel).toHaveBeenCalledWith(2);
  });

  it('allows defining date and time to confirm a requested appointment', async () => {
    vi.mocked(appointmentsApi.confirm).mockResolvedValue({} as any);

    await act(async () => {
      customRender(<AdminAppointments />);
    });

    const defineTimeBtn = screen.getByRole('button', { name: 'Definir horário' });
    expect(defineTimeBtn).toBeInTheDocument();

    fireEvent.click(defineTimeBtn);

    // Confirm Modal for Date/Time should open
    expect(screen.getByText('Confirmar horário')).toBeInTheDocument();

    const dateTimeInput = screen.getByLabelText('Data e hora');
    fireEvent.change(dateTimeInput, { target: { value: '2026-06-26T10:00' } });

    const submitBtn = screen.getByRole('button', { name: 'Confirmar solicitação' });
    await act(async () => {
      fireEvent.click(submitBtn);
    });

    expect(appointmentsApi.confirm).toHaveBeenCalledWith(2, '2026-06-26T10:00:00');
  });
});
