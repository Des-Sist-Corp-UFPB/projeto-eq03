import { useState, useEffect, useCallback } from 'react';
import { KeyRound, Trash2, Copy } from 'lucide-react';
import { mcpTokensService, type McpTokenData } from '../../services/mcpTokens';
import { useAlert } from '../../hooks/useAlert';
import { getApiErrorMessage } from '../../utils/apiError';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const McpTokensSection = () => {
  const [tokens, setTokens] = useState<McpTokenData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [name, setName] = useState('');
  const [expiresInDays, setExpiresInDays] = useState('90');
  const [isCreating, setIsCreating] = useState(false);
  const [newToken, setNewToken] = useState<string | null>(null);

  const { error: showError, success: showSuccess, confirm } = useAlert();

  const loadTokens = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await mcpTokensService.list();
      setTokens(data);
    } catch (err) {
      showError(getApiErrorMessage(err, 'Erro ao carregar tokens MCP.'));
    } finally {
      setIsLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    loadTokens();
  }, [loadTokens]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      showError('Dê um nome ao token (ex.: "Claude Desktop — Rodrigo").');
      return;
    }
    setIsCreating(true);
    try {
      const days = expiresInDays ? Number(expiresInDays) : null;
      const generated = await mcpTokensService.generate(name.trim(), days);
      setNewToken(generated.rawValue);
      setName('');
      await loadTokens();
    } catch (err) {
      showError(getApiErrorMessage(err, 'Erro ao gerar token.'));
    } finally {
      setIsCreating(false);
    }
  };

  const handleRevoke = async (token: McpTokenData) => {
    await confirm(
      `Revogar o token "${token.name}"? Quem estiver usando perde acesso ao servidor MCP imediatamente.`,
      async () => {
        try {
          await mcpTokensService.revoke(token.id);
          showSuccess('Token revogado com sucesso.');
          await loadTokens();
        } catch (err) {
          showError(getApiErrorMessage(err, 'Erro ao revogar token.'));
        }
      },
      { isDangerous: true, confirmText: 'Revogar' }
    );
  };

  const copyToClipboard = async () => {
    if (!newToken) return;
    await navigator.clipboard.writeText(newToken);
    showSuccess('Token copiado para a área de transferência.');
  };

  return (
    <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-6 space-y-5 shadow-sm">
      <div className="flex items-center gap-3">
        <KeyRound size={22} className="text-[#be8a83]" />
        <div>
          <h3 className="font-heading text-lg font-bold text-[#3b3036]">Tokens de acesso MCP</h3>
          <p className="text-sm text-[#3b3036]/60">
            Cada integração (Claude Desktop, Cursor, etc.) usa o próprio token — revogável
            individualmente, sem afetar as demais.
          </p>
        </div>
      </div>

      {newToken && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl space-y-2">
          <p className="text-sm font-semibold text-amber-800">
            Copie agora — este valor não será mostrado novamente:
          </p>
          <div className="flex items-center gap-2">
            <code className="flex-1 text-xs bg-white border border-amber-200 rounded-lg px-3 py-2 break-all">
              {newToken}
            </code>
            <button
              type="button"
              onClick={copyToClipboard}
              className="shrink-0 p-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg cursor-pointer"
              aria-label="Copiar token"
            >
              <Copy size={16} />
            </button>
          </div>
        </div>
      )}

      <form onSubmit={handleCreate} className="flex flex-col sm:flex-row gap-3 items-end">
        <div className="flex-1 space-y-1.5 w-full">
          <label className={labelCls}>Nome do token</label>
          <input
            type="text"
            placeholder="Ex.: Claude Desktop — Rodrigo"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputCls}
          />
        </div>
        <div className="space-y-1.5 w-full sm:w-40">
          <label className={labelCls}>Validade (dias)</label>
          <input
            type="number"
            min="1"
            max="365"
            value={expiresInDays}
            onChange={(e) => setExpiresInDays(e.target.value)}
            className={inputCls}
          />
        </div>
        <button
          type="submit"
          disabled={isCreating}
          className="shrink-0 px-4 py-2.5 bg-[#be8a83] hover:bg-[#a1706a] text-[#fcf9f9] text-sm font-semibold rounded-xl transition-all disabled:opacity-50 disabled:pointer-events-none cursor-pointer w-full sm:w-auto"
        >
          {isCreating ? 'Gerando...' : 'Gerar token'}
        </button>
      </form>

      {isLoading ? (
        <div className="flex justify-center py-6">
          <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-[#be8a83]"></div>
        </div>
      ) : tokens.length === 0 ? (
        <p className="text-sm text-[#3b3036]/50 text-center py-4">Nenhum token gerado ainda.</p>
      ) : (
        <div className="space-y-2">
          {tokens.map((token) => (
            <div
              key={token.id}
              className="flex items-center justify-between gap-3 border border-[#eae1e1]/80 rounded-xl p-3"
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-sm text-[#3b3036]">{token.name}</span>
                  {token.revoked && (
                    <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-gray-100 text-gray-500 border border-gray-200">
                      Revogado
                    </span>
                  )}
                </div>
                <p className="text-xs text-[#3b3036]/50">
                  Criado por {token.createdBy} em {new Date(token.createdAt).toLocaleDateString('pt-BR')}
                  {token.expiresAt && ` · expira em ${new Date(token.expiresAt).toLocaleDateString('pt-BR')}`}
                  {token.lastUsedAt
                    ? ` · último uso em ${new Date(token.lastUsedAt).toLocaleString('pt-BR')}`
                    : ' · nunca usado'}
                </p>
              </div>
              {!token.revoked && (
                <button
                  type="button"
                  onClick={() => handleRevoke(token)}
                  className="shrink-0 p-2 text-rose-500 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                  aria-label={`Revogar ${token.name}`}
                >
                  <Trash2 size={16} />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
