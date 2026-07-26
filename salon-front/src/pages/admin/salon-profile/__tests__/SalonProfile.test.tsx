import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, waitFor, customRender } from '../../../../test/test-utils';
import { SalonProfile } from '../SalonProfile';
import { salonProfileService } from '../../../../services/salonProfile';

vi.mock('../../../../services/salonProfile', async () => {
  const actual = await vi.importActual<typeof import('../../../../services/salonProfile')>(
    '../../../../services/salonProfile'
  );
  return {
    ...actual,
    salonProfileService: {
      getPublic: vi.fn(),
      update: vi.fn(),
    },
  };
});

const mockShowError = vi.fn();
const mockShowSuccess = vi.fn();
vi.mock('../../../../hooks/useAlert', () => ({
  useAlert: () => ({
    error: mockShowError,
    success: mockShowSuccess,
  }),
}));

const mockProfile = {
  id: 1,
  name: 'Espaço Cristiane Moura',
  description: 'Sobre nós',
  address: 'Rua Teste, 123',
  phone: '83999999999',
  instagram: '@salao',
  whatsapp: '83999999999',
  logoUrl: null,
  updatedAt: '2026-01-01T10:00:00',
  businessHours: [
    { dayOfWeek: 'MONDAY', open: true, openTime: '08:00:00', closeTime: '18:00:00' },
    { dayOfWeek: 'TUESDAY', open: true, openTime: '08:00:00', closeTime: '18:00:00' },
    { dayOfWeek: 'WEDNESDAY', open: true, openTime: '08:00:00', closeTime: '18:00:00' },
    { dayOfWeek: 'THURSDAY', open: true, openTime: '08:00:00', closeTime: '18:00:00' },
    { dayOfWeek: 'FRIDAY', open: true, openTime: '08:00:00', closeTime: '18:00:00' },
    { dayOfWeek: 'SATURDAY', open: true, openTime: '08:00:00', closeTime: '18:00:00' },
    { dayOfWeek: 'SUNDAY', open: false, openTime: null, closeTime: null },
  ],
} as const;

const renderPage = () =>
  customRender(<SalonProfile />, {
    user: { email: 'admin@salao.com', role: 'ADMIN', userId: 1, permissions: [] },
    isAuthenticated: true,
  });

describe('SalonProfile admin page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(salonProfileService.getPublic).mockResolvedValue(mockProfile as any);
  });

  it('loads and displays the current profile', async () => {
    await act(async () => {
      renderPage();
    });

    expect(screen.getByDisplayValue('Espaço Cristiane Moura')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Rua Teste, 123')).toBeInTheDocument();
  });

  it('renders all 7 weekdays with Sunday unchecked (closed)', async () => {
    await act(async () => {
      renderPage();
    });

    expect((document.getElementById('business-hour-SUNDAY-open') as HTMLInputElement).checked).toBe(false);
    expect((document.getElementById('business-hour-MONDAY-open') as HTMLInputElement).checked).toBe(true);
  });

  it('submits the updated profile including business hours', async () => {
    vi.mocked(salonProfileService.update).mockResolvedValue(mockProfile as any);

    await act(async () => {
      renderPage();
    });

    fireEvent.change(screen.getByDisplayValue('Espaço Cristiane Moura'), {
      target: { value: 'Novo Nome do Salão' },
    });
    fireEvent.click(screen.getByRole('button', { name: /salvar perfil/i }));

    await waitFor(() => expect(salonProfileService.update).toHaveBeenCalled());
    const payload = vi.mocked(salonProfileService.update).mock.calls[0][0];
    expect(payload.name).toBe('Novo Nome do Salão');
    expect(payload.businessHours).toHaveLength(7);
    expect(payload.businessHours.find((h) => h.dayOfWeek === 'SUNDAY')?.open).toBe(false);
    expect(mockShowSuccess).toHaveBeenCalled();
  });

  it('shows an error alert when loading the profile fails', async () => {
    vi.mocked(salonProfileService.getPublic).mockRejectedValue(new Error('offline'));

    await act(async () => {
      renderPage();
    });

    expect(mockShowError).toHaveBeenCalled();
  });
});
