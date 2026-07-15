import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, customRender } from '../../../test/test-utils';
import { AiConfig } from '../AiConfig';
import { aiConfigService } from '../../../services/aiConfig';

vi.mock('../../../services/aiConfig', () => ({
  AI_MODELS: ['gpt-4o-mini', 'gpt-4o', 'gpt-4.1-mini'],
  aiConfigService: {
    get: vi.fn(),
    update: vi.fn(),
  },
}));

// Referências estáveis: o componente depende de showError/showSuccess num useCallback,
// então um mock que recria as funções a cada render causaria loop infinito de efeito.
const mockShowError = vi.fn();
const mockShowSuccess = vi.fn();
vi.mock('../../../hooks/useAlert', () => ({
  useAlert: () => ({
    error: mockShowError,
    success: mockShowSuccess,
  }),
}));

const mockConfig = {
  baseUrl: 'https://llm.rodrigor.com',
  model: 'gpt-4o-mini',
  apiKeyMasked: 'sk-•••••WesymE',
  apiKeyConfigured: true,
  temperature: 0.3,
  maxTokens: 500,
  enabled: true,
  dailyCallBudget: 200,
  updatedBy: 'sysadmin@salao.com',
  updatedAt: '2026-07-01T10:00:00',
};

const renderAiConfig = () =>
  customRender(<AiConfig />, {
    user: { email: 'sysadmin@salao.com', role: 'SYSADMIN', userId: 1, permissions: [] },
    isAuthenticated: true,
  });

describe('AiConfig page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(aiConfigService.get).mockResolvedValue(mockConfig);
  });

  it('loads and displays the current config, never showing the plain API key', async () => {
    await act(async () => {
      renderAiConfig();
    });

    expect(screen.getByDisplayValue('https://llm.rodrigor.com')).toBeInTheDocument();
    expect(screen.getByText(/sk-•••••WesymE/)).toBeInTheDocument();
    expect(screen.queryByText(/ivdkbnUwyFCPx5Cinwj/)).not.toBeInTheDocument();
  });

  it('submits the form and updates the config', async () => {
    vi.mocked(aiConfigService.update).mockResolvedValue({ ...mockConfig, enabled: false });

    await act(async () => {
      renderAiConfig();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /salvar configuração/i }));
    });

    expect(aiConfigService.update).toHaveBeenCalledWith(
      expect.objectContaining({ baseUrl: 'https://llm.rodrigor.com', model: 'gpt-4o-mini' })
    );
  });

  it('sends apiKey as null when the field is left blank (keeps the previous key)', async () => {
    vi.mocked(aiConfigService.update).mockResolvedValue(mockConfig);

    await act(async () => {
      renderAiConfig();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /salvar configuração/i }));
    });

    expect(aiConfigService.update).toHaveBeenCalledWith(
      expect.objectContaining({ apiKey: null })
    );
  });
});
