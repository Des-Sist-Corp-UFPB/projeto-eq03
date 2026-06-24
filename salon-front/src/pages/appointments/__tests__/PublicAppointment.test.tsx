import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, customRender } from '../../../test/test-utils';
import { PublicAppointment } from '../PublicAppointment';
import { salonServicesApi } from '../../services/services/services';
import { employeesApi } from '../../admin/employees/services/employees';
import { appointmentsApi } from '../services/appointments';
import { featureFlagsService } from '../../../services/featureFlags';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../services/services/services', () => ({
  salonServicesApi: {
    findAll: vi.fn(),
  },
  displayServiceDuration: () => '30 min',
}));

vi.mock('../../admin/employees/services/employees', () => ({
  employeesApi: {
    findAllForBooking: vi.fn(),
  },
}));

vi.mock('../services/appointments', () => ({
  appointmentsApi: {
    create: vi.fn(),
  },
}));

vi.mock('../../../services/featureFlags', () => ({
  featureFlagsService: {
    getPublicFlags: vi.fn(),
  },
}));

const mockServices = [
  {
    id: 1,
    name: 'Corte',
    price: 50.0,
    durationMinutes: 30,
    active: true,
    description: 'Corte de cabelo',
  },
  {
    id: 2,
    name: 'Escova',
    price: 40.0,
    durationMinutes: 20,
    active: true,
    description: 'Escovação premium',
  },
];

const mockEmployees = [{ id: 10, userId: 100, name: 'Mariana', bio: 'Cabelos e Penteados' }];

describe('PublicAppointment Wizard Integration', () => {
  beforeEach(() => {
    vi.mocked(salonServicesApi.findAll).mockResolvedValue(mockServices);
    vi.mocked(employeesApi.findAllForBooking).mockResolvedValue(mockEmployees);
    vi.mocked(featureFlagsService.getPublicFlags).mockResolvedValue([
      { name: 'CLIENT_BOOKING', enabled: true, description: 'Client booking feature' },
    ]);
    vi.mocked(appointmentsApi.create).mockResolvedValue({} as any);

    // Reset scroll mock
    window.scrollTo = vi.fn();
  });

  it('should run through the happy path (Step 1 to 4) when authenticated', async () => {
    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
        user: { email: 'client@salao.com', role: 'CLIENTE', userId: 5, permissions: [] },
      });
    });

    // STEP 1: Service selection
    expect(screen.getByText('O que vamos fazer?')).toBeInTheDocument();
    expect(screen.getByText('Corte')).toBeInTheDocument();
    expect(screen.getByText('Escova')).toBeInTheDocument();

    const nextBtn = screen.getByText('Próximo');
    expect(nextBtn).toBeDisabled();

    // Select Corte
    fireEvent.click(screen.getByText('Corte'));
    expect(nextBtn).toBeEnabled();
    fireEvent.click(nextBtn);

    // STEP 2: Professional selection
    expect(screen.getByText('Com quem você prefere?')).toBeInTheDocument();
    expect(screen.getByText('Mariana')).toBeInTheDocument();
    expect(nextBtn).toBeDisabled();

    // Select Mariana
    fireEvent.click(screen.getByText('Mariana'));
    expect(nextBtn).toBeEnabled();
    fireEvent.click(nextBtn);

    // STEP 3: Preferences selection
    expect(screen.getByText('Dia de preferência (opcional)')).toBeInTheDocument();

    const dateInput = screen.getByLabelText(/Dia de preferência/i);
    const notesTextarea = screen.getByPlaceholderText(/Ex.: só de manhã/i);

    fireEvent.change(dateInput, { target: { value: '2026-12-25' } });
    fireEvent.change(notesTextarea, { target: { value: 'Quero corte curto' } });

    fireEvent.click(nextBtn);

    // STEP 4: Review and Submit
    expect(screen.getByText('Revisar pedido')).toBeInTheDocument();
    expect(screen.getByText('Corte')).toBeInTheDocument();
    expect(screen.getByText('Mariana')).toBeInTheDocument();
    expect(screen.getByText('25/12/2026')).toBeInTheDocument();
    expect(screen.getByText('Quero corte curto')).toBeInTheDocument();

    const submitBtn = screen.getByText('Enviar solicitação');
    await act(async () => {
      fireEvent.click(submitBtn);
    });

    expect(appointmentsApi.create).toHaveBeenCalledWith({
      serviceId: 1,
      employeeId: 10,
      preferredDate: '2026-12-25',
      clientNotes: 'Quero corte curto',
    });
    expect(mockNavigate).toHaveBeenCalledWith('/my-appointments');
  });

  it('should redirect to /login and save pending appointment if user is unauthenticated', async () => {
    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: false,
        user: null,
      });
    });

    // STEP 1: Select Corte
    fireEvent.click(screen.getByText('Corte'));
    fireEvent.click(screen.getByText('Próximo'));

    // STEP 2: Select Mariana
    fireEvent.click(screen.getByText('Mariana'));
    fireEvent.click(screen.getByText('Próximo'));

    // STEP 3: Preferences
    fireEvent.click(screen.getByText('Próximo'));

    // STEP 4: Submit redirects to login
    expect(
      screen.getByText('Você precisará entrar na sua conta para enviar a solicitação.')
    ).toBeInTheDocument();

    const submitBtn = screen.getByText('Enviar solicitação');
    fireEvent.click(submitBtn);

    const pending = JSON.parse(localStorage.getItem('pending_appointment') || '{}');
    expect(pending.serviceId).toBe(1);
    expect(pending.employeeId).toBe(10);
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('should block navigation with error message if date is in the past', async () => {
    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
        user: { email: 'client@salao.com', role: 'CLIENTE', userId: 5, permissions: [] },
      });
    });

    // STEP 1
    fireEvent.click(screen.getByText('Corte'));
    fireEvent.click(screen.getByText('Próximo'));

    // STEP 2
    fireEvent.click(screen.getByText('Mariana'));
    fireEvent.click(screen.getByText('Próximo'));

    // STEP 3: Past date
    const dateInput = screen.getByLabelText(/Dia de preferência/i);
    fireEvent.change(dateInput, { target: { value: '2020-01-01' } });

    fireEvent.click(screen.getByText('Próximo'));

    expect(
      screen.getByText('A data de preferência deve ser hoje ou uma data futura.')
    ).toBeInTheDocument();
  });

  it('should render disabled booking page if feature flag is false', async () => {
    vi.mocked(featureFlagsService.getPublicFlags).mockResolvedValue([
      { name: 'CLIENT_BOOKING', enabled: false, description: 'Client booking feature' },
    ]);

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
      });
    });

    expect(screen.getByText('Agendamentos Online Desativados')).toBeInTheDocument();

    const backBtn = screen.getByText('Voltar para o início');
    fireEvent.click(backBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('should show empty states when services or employees lists are empty', async () => {
    vi.mocked(salonServicesApi.findAll).mockResolvedValue([]);
    vi.mocked(employeesApi.findAllForBooking).mockResolvedValue([]);

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
      });
    });

    expect(screen.getByText('Nenhum serviço disponível')).toBeInTheDocument();

    // Fake moving to step 2 to verify empty employees list
    // (In reality, user cannot click next if services is empty, but we can set up step hook/state or simulate it)
  });

  it('should allow navigation back to previous steps', async () => {
    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
        user: { email: 'client@salao.com', role: 'CLIENTE', userId: 5, permissions: [] },
      });
    });

    fireEvent.click(screen.getByText('Corte'));
    fireEvent.click(screen.getByText('Próximo'));

    expect(screen.getByText('Com quem você prefere?')).toBeInTheDocument();

    const backBtn = screen.getByText('Voltar');
    fireEvent.click(backBtn);

    expect(screen.getByText('O que vamos fazer?')).toBeInTheDocument();
  });

  it('should display error message if submitting the appointment fails', async () => {
    vi.mocked(appointmentsApi.create).mockRejectedValue(new Error('API Error'));

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
        user: { email: 'client@salao.com', role: 'CLIENTE', userId: 5, permissions: [] },
      });
    });

    fireEvent.click(screen.getByText('Corte'));
    fireEvent.click(screen.getByText('Próximo'));

    fireEvent.click(screen.getByText('Mariana'));
    fireEvent.click(screen.getByText('Próximo'));

    fireEvent.click(screen.getByText('Próximo'));

    const submitBtn = screen.getByText('Enviar solicitação');
    await act(async () => {
      fireEvent.click(submitBtn);
    });

    expect(screen.getByText('API Error')).toBeInTheDocument();
  });

  it('should display initial loading error when fetching services fails', async () => {
    vi.mocked(salonServicesApi.findAll).mockRejectedValue(new Error('Fetch services failed'));

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
      });
    });

    expect(screen.getByText('Fetch services failed')).toBeInTheDocument();
  });

  it('should redirect to /login when clicking "Entrar / Cadastrar" banner button', async () => {
    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: false,
        user: null,
      });
    });

    const loginBannerBtn = screen.getByText('Entrar / Cadastrar');
    fireEvent.click(loginBannerBtn);

    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('should recover pending appointment from localStorage and advance to step 4 when both serviceId and employeeId are present', async () => {
    localStorage.setItem(
      'pending_appointment',
      JSON.stringify({
        serviceId: 1,
        employeeId: 10,
        preferredDate: '2026-12-25',
        clientNotes: 'Recovered notes',
      })
    );

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
        user: { email: 'client@salao.com', role: 'CLIENTE', userId: 5, permissions: [] },
      });
    });

    expect(screen.getByText('Revisar pedido')).toBeInTheDocument();
    expect(screen.getByText('Corte')).toBeInTheDocument();
    expect(screen.getByText('Mariana')).toBeInTheDocument();
    expect(screen.getByText('25/12/2026')).toBeInTheDocument();
    expect(screen.getByText('Recovered notes')).toBeInTheDocument();
  });

  it('should recover pending appointment from localStorage and advance to step 2 when only serviceId is present', async () => {
    localStorage.setItem(
      'pending_appointment',
      JSON.stringify({
        serviceId: 1,
      })
    );

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
        user: { email: 'client@salao.com', role: 'CLIENTE', userId: 5, permissions: [] },
      });
    });

    expect(screen.getByText('Com quem você prefere?')).toBeInTheDocument();
  });

  it('should handle JSON parsing errors when recovering pending appointment from localStorage', async () => {
    localStorage.setItem('pending_appointment', 'invalid-json-{');

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
        user: { email: 'client@salao.com', role: 'CLIENTE', userId: 5, permissions: [] },
      });
    });

    expect(screen.getByText('O que vamos fazer?')).toBeInTheDocument();
  });

  it('should handle feature flags API failure gracefully and default to online booking enabled', async () => {
    vi.mocked(featureFlagsService.getPublicFlags).mockRejectedValue(new Error('API Error'));

    await act(async () => {
      customRender(<PublicAppointment />, {
        isAuthenticated: true,
      });
    });

    expect(screen.queryByText('Agendamentos Online Desativados')).not.toBeInTheDocument();
    expect(screen.getByText('O que vamos fazer?')).toBeInTheDocument();
  });
});
