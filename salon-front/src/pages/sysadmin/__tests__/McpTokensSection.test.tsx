import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, waitFor, customRender } from '../../../test/test-utils';
import { McpTokensSection } from '../McpTokensSection';
import { mcpTokensService } from '../../../services/mcpTokens';

vi.mock('../../../services/mcpTokens', () => ({
  mcpTokensService: {
    list: vi.fn(),
    generate: vi.fn(),
    revoke: vi.fn(),
  },
}));

const mockShowError = vi.fn();
const mockShowSuccess = vi.fn();
const mockConfirm = vi.fn((_msg: string, onConfirm?: () => void | Promise<void>) => onConfirm?.());
vi.mock('../../../hooks/useAlert', () => ({
  useAlert: () => ({ error: mockShowError, success: mockShowSuccess, confirm: mockConfirm }),
}));

Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });

const existingToken = {
  id: 1,
  name: 'Claude Desktop',
  createdBy: 'sysadmin@salao.com',
  createdAt: '2026-07-01T10:00:00',
  expiresAt: null,
  lastUsedAt: null,
  revoked: false,
};

const renderSection = () =>
  customRender(<McpTokensSection />, {
    user: { email: 'sysadmin@salao.com', role: 'SYSADMIN', userId: 1, permissions: [] },
    isAuthenticated: true,
  });

describe('McpTokensSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists existing tokens', async () => {
    vi.mocked(mcpTokensService.list).mockResolvedValue([existingToken]);

    await act(async () => {
      renderSection();
    });

    expect(screen.getByText('Claude Desktop')).toBeInTheDocument();
    expect(screen.getByText(/nunca usado/i)).toBeInTheDocument();
  });

  it('generates a new token and shows the raw value once', async () => {
    vi.mocked(mcpTokensService.list).mockResolvedValue([]);
    vi.mocked(mcpTokensService.generate).mockResolvedValue({
      token: { ...existingToken, id: 2, name: 'Cursor' },
      rawValue: 'mcp_abc123',
    });

    await act(async () => {
      renderSection();
    });

    fireEvent.change(screen.getByPlaceholderText(/claude desktop/i), { target: { value: 'Cursor' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /gerar token/i }));
    });

    await waitFor(() => {
      expect(screen.getByText('mcp_abc123')).toBeInTheDocument();
    });
    expect(mcpTokensService.generate).toHaveBeenCalledWith('Cursor', 90);
  });

  it('requires a name before generating', async () => {
    vi.mocked(mcpTokensService.list).mockResolvedValue([]);

    await act(async () => {
      renderSection();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /gerar token/i }));
    });

    expect(mockShowError).toHaveBeenCalled();
    expect(mcpTokensService.generate).not.toHaveBeenCalled();
  });

  it('revokes a token after confirmation', async () => {
    vi.mocked(mcpTokensService.list).mockResolvedValue([existingToken]);
    vi.mocked(mcpTokensService.revoke).mockResolvedValue(undefined);

    await act(async () => {
      renderSection();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /revogar claude desktop/i }));
    });

    expect(mcpTokensService.revoke).toHaveBeenCalledWith(1);
    expect(mockShowSuccess).toHaveBeenCalled();
  });
});
