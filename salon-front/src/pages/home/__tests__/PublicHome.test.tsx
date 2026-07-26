import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, act, customRender } from '../../../test/test-utils';
import { PublicHome } from '../PublicHome';
import { salonProfileService } from '../../../services/salonProfile';

vi.mock('../../../services/salonProfile', () => ({
  salonProfileService: {
    getPublic: vi.fn(),
  },
  DAY_ORDER: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
  DAY_LABELS: {
    MONDAY: 'Segunda-feira',
    TUESDAY: 'Terça-feira',
    WEDNESDAY: 'Quarta-feira',
    THURSDAY: 'Quinta-feira',
    FRIDAY: 'Sexta-feira',
    SATURDAY: 'Sábado',
    SUNDAY: 'Domingo',
  },
}));

const fullProfile = {
  id: 1,
  name: 'Espaço Cristiane Moura',
  description: 'O melhor salão da cidade.',
  address: 'Rua das Flores, 123',
  phone: '(83) 99999-9999',
  instagram: '@espacocristiane',
  whatsapp: '(83) 98888-8888',
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

describe('PublicHome', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the hero section regardless of the salon profile call outcome', async () => {
    vi.mocked(salonProfileService.getPublic).mockRejectedValue(new Error('offline'));

    await act(async () => {
      customRender(<PublicHome />);
    });

    expect(screen.getByText('Beleza e bem-estar no seu ritmo')).toBeInTheDocument();
  });

  it('displays the salon description, contact info and business hours once loaded', async () => {
    vi.mocked(salonProfileService.getPublic).mockResolvedValue(fullProfile as any);

    await act(async () => {
      customRender(<PublicHome />);
    });

    expect(await screen.findByText('O melhor salão da cidade.')).toBeInTheDocument();
    expect(screen.getByText('Rua das Flores, 123')).toBeInTheDocument();
    expect(screen.getByText('@espacocristiane')).toBeInTheDocument();
    expect(screen.getAllByText('08:00 às 18:00').length).toBeGreaterThan(0);
    expect(screen.getByText('Fechado')).toBeInTheDocument();
  });

  it('does not render the profile section when the request fails', async () => {
    vi.mocked(salonProfileService.getPublic).mockRejectedValue(new Error('offline'));

    await act(async () => {
      customRender(<PublicHome />);
    });

    expect(screen.queryByText('Horário de Funcionamento')).not.toBeInTheDocument();
  });
});
