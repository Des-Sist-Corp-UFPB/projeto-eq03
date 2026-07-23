import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, waitFor, customRender } from '../../../../test/test-utils';
import { Recommendations } from '../Recommendations';
import { recommendationsService } from '../../../../services/recommendations';

vi.mock('../../../../services/recommendations', () => ({
  recommendationsService: {
    getStatus: vi.fn(),
    getLatest: vi.fn(),
    generate: vi.fn(),
  },
}));

const mockShowError = vi.fn();
vi.mock('../../../../hooks/useAlert', () => ({
  useAlert: () => ({ error: mockShowError, success: vi.fn() }),
}));

vi.mock('../../../../hooks/useFeatureFlag', () => ({
  useFeatureFlag: () => ({ enabled: true, isLoading: false }),
}));

const cachedResult = {
  type: 'FINANCEIRO' as const,
  items: [
    { title: 'Ociosidade nas terças', description: '40% de vagas livres.', suggestedAction: 'Criar promoção', priority: 'ALTA' as const },
  ],
  generatedAt: '2026-07-01T10:00:00',
  fromCache: true,
};

const renderPage = () =>
  customRender(<Recommendations />, {
    user: { email: 'admin@salao.com', role: 'ADMIN', userId: 1, permissions: [] },
    isAuthenticated: true,
  });

describe('Recommendations page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(recommendationsService.getStatus).mockResolvedValue({ available: true });
  });

  it('shows an empty state when there is no cached recommendation yet (404)', async () => {
    vi.mocked(recommendationsService.getLatest).mockRejectedValue({ response: { status: 404 } });

    await act(async () => {
      renderPage();
    });

    expect(screen.getAllByText(/nenhuma recomendação gerada ainda/i)).toHaveLength(2);
    expect(mockShowError).not.toHaveBeenCalled();
  });

  it('renders cached recommendations for both sections', async () => {
    vi.mocked(recommendationsService.getLatest).mockImplementation(async (type) =>
      type === 'FINANCEIRO' ? cachedResult : { ...cachedResult, type: 'RETENCAO', items: [] } as any
    );

    await act(async () => {
      renderPage();
    });

    expect(screen.getByText('Ociosidade nas terças')).toBeInTheDocument();
    expect(screen.getByText(/criar promoção/i)).toBeInTheDocument();
  });

  it('calls generate and displays the fresh result when clicking Gerar/Atualizar', async () => {
    vi.mocked(recommendationsService.getLatest).mockRejectedValue({ response: { status: 404 } });
    vi.mocked(recommendationsService.generate).mockResolvedValue({
      ...cachedResult,
      fromCache: false,
      items: [{ title: 'Nova recomendação', description: 'desc', suggestedAction: 'acao', priority: 'MEDIA' }],
    });

    await act(async () => {
      renderPage();
    });

    const buttons = screen.getAllByRole('button', { name: /gerar/i });
    await act(async () => {
      fireEvent.click(buttons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('Nova recomendação')).toBeInTheDocument();
    });
  });

  it('shows an error toast when generate fails', async () => {
    vi.mocked(recommendationsService.getLatest).mockRejectedValue({ response: { status: 404 } });
    vi.mocked(recommendationsService.generate).mockRejectedValue({
      response: { status: 400, data: { message: 'Orçamento diário atingido.' } },
    });

    await act(async () => {
      renderPage();
    });

    const buttons = screen.getAllByRole('button', { name: /gerar/i });
    await act(async () => {
      fireEvent.click(buttons[0]);
    });

    await waitFor(() => {
      expect(mockShowError).toHaveBeenCalled();
    });
  });
});
