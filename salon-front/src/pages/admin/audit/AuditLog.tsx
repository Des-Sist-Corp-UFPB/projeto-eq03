import { useState, useEffect } from 'react';
import { Filter, Eye, X, AlertCircle, Copy, Check } from 'lucide-react';
import api from '../../../services/api';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

interface AuditLogEntry {
  id: number;
  userId: number;
  userEmail: string;
  action: string;
  entityType: string;
  entityId?: number;
  details?: string;
  status: string;
  createdAt: string;
  ipAddress?: string;
}

export const AuditLog = () => {
  const PAGE_SIZE = 15;
  const [auditLogs, setAuditLogs] = useState<AuditLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalItems, setTotalItems] = useState(0);
  const [page, setPage] = useState(0);
  const [filterAction, setFilterAction] = useState('');
  const [filterEntity, setFilterEntity] = useState('');
  const [filterUser, setFilterUser] = useState('');
  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null);
  const [copiedFullJson, setCopiedFullJson] = useState(false);
  const { error: showError } = useAlert();

  const loadAuditLogs = async () => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', PAGE_SIZE.toString());

      if (filterAction) params.append('action', filterAction);
      if (filterEntity) params.append('entityType', filterEntity);
      if (filterUser) params.append('userEmail', filterUser);

      const { data } = await api.get(`/audit?${params.toString()}`);

      setAuditLogs(data.content);
      setTotalItems(data.totalElements);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar logs de auditoria');
      await showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAuditLogs();
  }, [page, filterAction, filterEntity, filterUser]);

  const getStatusBadge = (status: string) => {
    const isSuccess = status === 'SUCCESS';
    return (
      <span
        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
          isSuccess
            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
            : 'bg-rose-50 text-rose-700 border border-rose-200'
        }`}
      >
        <span
          className={`h-1.5 w-1.5 rounded-full ${isSuccess ? 'bg-emerald-500' : 'bg-rose-500'}`}
        />
        {isSuccess ? 'Sucesso' : 'Falha'}
      </span>
    );
  };

  const getActionBadge = (action: string) => {
    let colorClasses = 'bg-gray-100 text-gray-700 border border-gray-200';
    switch (action.toUpperCase()) {
      case 'CREATE':
        colorClasses = 'bg-teal-50 text-teal-700 border border-teal-200';
        break;
      case 'UPDATE':
        colorClasses = 'bg-indigo-50 text-indigo-700 border border-indigo-200';
        break;
      case 'DELETE':
        colorClasses = 'bg-rose-50 text-rose-700 border border-rose-200';
        break;
      case 'LOGIN':
        colorClasses = 'bg-amber-50 text-amber-700 border border-amber-200';
        break;
      case 'LOGOUT':
        colorClasses = 'bg-purple-50 text-purple-700 border border-purple-200';
        break;
      default:
        break;
    }
    return (
      <span
        className={`inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-semibold tracking-wider ${colorClasses}`}
      >
        {action}
      </span>
    );
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('pt-BR');
  };

  const formatUserEmail = (email: string | null) => {
    if (!email || email === 'anonymousUser') return 'Sistema / Visitante';
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  };

  const getPrettyDetails = (detailsStr?: string) => {
    if (!detailsStr) return null;
    try {
      const parsed = JSON.parse(detailsStr);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return detailsStr;
    }
  };

  const getFullLogJson = (log: AuditLogEntry) => {
    let parsedDetails = null;
    try {
      if (log.details) {
        parsedDetails = JSON.parse(log.details);
      }
    } catch {
      parsedDetails = log.details;
    }
    return JSON.stringify({ ...log, details: parsedDetails }, null, 2);
  };

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedFullJson(true);
      setTimeout(() => setCopiedFullJson(false), 2000);
    } catch {
      // ignore
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h2 className="font-heading text-2xl font-bold text-[#3b3036] tracking-wide">
          📊 Logs de Auditoria
        </h2>
        <p className="text-sm text-[#3b3036]/60 mt-1">
          Acompanhe e audite todas as atividades e operações realizadas no sistema.
        </p>
      </div>

      {/* Filters Card */}
      <div className="bg-white rounded-2xl border border-gray-100 p-5 shadow-xs">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
          <div className="space-y-1">
            <label className="label-premium">Usuário / E-mail</label>
            <input
              type="text"
              placeholder="Ex: admin@salao.com"
              value={filterUser}
              onChange={(e) => {
                setFilterUser(e.target.value);
                setPage(0);
              }}
              className="input-premium"
            />
          </div>

          <div className="space-y-1">
            <label className="label-premium">Ação</label>
            <select
              value={filterAction}
              onChange={(e) => {
                setFilterAction(e.target.value);
                setPage(0);
              }}
              className="input-premium"
            >
              <option value="">Todas as ações</option>
              <option value="CREATE">CREATE</option>
              <option value="UPDATE">UPDATE</option>
              <option value="DELETE">DELETE</option>
              <option value="LOGIN">LOGIN</option>
              <option value="LOGOUT">LOGOUT</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="label-premium">Entidade</label>
            <select
              value={filterEntity}
              onChange={(e) => {
                setFilterEntity(e.target.value);
                setPage(0);
              }}
              className="input-premium"
            >
              <option value="">Todas as entidades</option>
              <option value="User">User</option>
              <option value="Appointment">Appointment</option>
              <option value="Service">Service</option>
              <option value="Product">Product</option>
              <option value="Employee">Employee</option>
              <option value="CashFlow">CashFlow</option>
              <option value="FeatureFlag">FeatureFlag</option>
            </select>
          </div>

          <button
            onClick={() => {
              setFilterAction('');
              setFilterEntity('');
              setFilterUser('');
              setPage(0);
            }}
            className="flex items-center justify-center gap-2 px-5 py-2.5 border border-gray-200 text-sm font-semibold text-[#3b3036]/80 hover:bg-gray-50 hover:text-[#3b3036] rounded-xl transition-all h-[42px] w-full sm:w-auto cursor-pointer"
          >
            <Filter size={16} /> Limpar Filtros
          </button>
        </div>
      </div>

      {/* Table Container */}
      <div className="bg-white rounded-2xl border border-gray-100 overflow-hidden shadow-xs">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 gap-3">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-[#be8a83]"></div>
            <span className="text-sm text-[#3b3036]/60 font-medium">
              Buscando logs de auditoria...
            </span>
          </div>
        ) : auditLogs.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 gap-2">
            <AlertCircle size={40} className="text-gray-300" />
            <span className="text-sm font-semibold text-[#3b3036]/80">
              Nenhum registro encontrado
            </span>
            <span className="text-xs text-[#3b3036]/50">Tente ajustar seus filtros de busca.</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50/70 border-b border-gray-100 text-xs font-bold text-[#3b3036]/70 uppercase tracking-wider">
                  <th className="px-6 py-4">Data / Hora</th>
                  <th className="px-6 py-4">Usuário</th>
                  <th className="px-6 py-4">Ação</th>
                  <th className="px-6 py-4">Entidade</th>
                  <th className="px-6 py-4">ID Ref</th>
                  <th className="px-6 py-4">Endereço IP</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-3.5 text-sm text-[#3b3036]/60 font-medium whitespace-nowrap">
                      {formatDate(log.createdAt)}
                    </td>
                    <td
                      className="px-6 py-3.5 text-sm font-semibold text-[#3b3036] max-w-[200px] truncate"
                      title={log.userEmail}
                    >
                      {formatUserEmail(log.userEmail)}
                    </td>
                    <td className="px-6 py-3.5 whitespace-nowrap">{getActionBadge(log.action)}</td>
                    <td className="px-6 py-3.5 text-sm text-[#3b3036]/80 font-medium">
                      {log.entityType}
                    </td>
                    <td className="px-6 py-3.5 text-sm font-mono text-gray-500">
                      {log.entityId || '-'}
                    </td>
                    <td className="px-6 py-3.5 text-sm font-mono text-gray-500">
                      {log.ipAddress || '-'}
                    </td>
                    <td className="px-6 py-3.5 whitespace-nowrap">{getStatusBadge(log.status)}</td>
                    <td className="px-6 py-3.5 text-right whitespace-nowrap">
                      <button
                        onClick={() => setSelectedLog(log)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-[#be8a83] hover:text-white bg-[#be8a83]/5 hover:bg-[#be8a83] border border-[#be8a83]/20 hover:border-transparent rounded-lg transition-all cursor-pointer"
                      >
                        <Eye size={14} /> Detalhes
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Pagination */}
      {!isLoading && auditLogs.length > 0 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 py-4">
          <span className="text-sm font-medium text-[#3b3036]/60">
            Exibindo <span className="font-semibold text-[#3b3036]">{auditLogs.length}</span> de{' '}
            <span className="font-semibold text-[#3b3036]">{totalItems}</span> registros
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="px-4 py-2 border border-gray-200 rounded-xl text-sm font-semibold text-[#3b3036] hover:bg-gray-50 disabled:opacity-40 disabled:hover:bg-transparent transition-all cursor-pointer"
            >
              Anterior
            </button>
            <span className="text-sm font-semibold text-[#3b3036] px-3">
              Página {page + 1} de {Math.ceil(totalItems / PAGE_SIZE)}
            </span>
            <button
              onClick={() => setPage(page + 1)}
              disabled={(page + 1) * PAGE_SIZE >= totalItems}
              className="px-4 py-2 border border-gray-200 rounded-xl text-sm font-semibold text-[#3b3036] hover:bg-gray-50 disabled:opacity-40 disabled:hover:bg-transparent transition-all cursor-pointer"
            >
              Próxima
            </button>
          </div>
        </div>
      )}

      {/* Details Modal */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-fadeIn">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-3xl border border-gray-100 flex flex-col max-h-[85vh] overflow-hidden transform scale-100 transition-all">
            {/* Modal Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 bg-gray-50/50">
              <div>
                <h3 className="font-heading text-lg font-bold text-[#3b3036]">
                  Detalhes do Log #{selectedLog.id}
                </h3>
                <p className="text-xs text-[#3b3036]/50">
                  Gerado em {formatDate(selectedLog.createdAt)}
                </p>
              </div>
              <button
                onClick={() => setSelectedLog(null)}
                className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all cursor-pointer"
              >
                <X size={20} />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto space-y-6">
              {/* Structured Metadata Fields */}
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                <div className="bg-gray-50/60 p-3 rounded-xl border border-gray-100">
                  <span className="block text-xs font-bold text-[#3b3036]/50 uppercase tracking-wider">
                    Ação
                  </span>
                  <span className="mt-1 block">{getActionBadge(selectedLog.action)}</span>
                </div>
                <div className="bg-gray-50/60 p-3 rounded-xl border border-gray-100">
                  <span className="block text-xs font-bold text-[#3b3036]/50 uppercase tracking-wider">
                    Entidade Afetada
                  </span>
                  <span className="mt-1.5 block text-sm font-semibold text-[#3b3036]">
                    {selectedLog.entityType}
                  </span>
                </div>
                <div className="bg-gray-50/60 p-3 rounded-xl border border-gray-100">
                  <span className="block text-xs font-bold text-[#3b3036]/50 uppercase tracking-wider">
                    ID de Referência
                  </span>
                  <span className="mt-1.5 block text-sm font-mono text-gray-600">
                    {selectedLog.entityId || 'N/A'}
                  </span>
                </div>
                <div className="bg-gray-50/60 p-3 rounded-xl border border-gray-100">
                  <span className="block text-xs font-bold text-[#3b3036]/50 uppercase tracking-wider">
                    Usuário / Autor
                  </span>
                  <span
                    className="mt-1.5 block text-sm font-semibold text-[#3b3036] truncate"
                    title={selectedLog.userEmail}
                  >
                    {selectedLog.userEmail || 'Sistema / Visitante'}
                  </span>
                </div>
                <div className="bg-gray-50/60 p-3 rounded-xl border border-gray-100">
                  <span className="block text-xs font-bold text-[#3b3036]/50 uppercase tracking-wider">
                    Endereço IP
                  </span>
                  <span className="mt-1.5 block text-sm font-mono text-gray-600">
                    {selectedLog.ipAddress || 'Desconhecido'}
                  </span>
                </div>
                <div className="bg-gray-50/60 p-3 rounded-xl border border-gray-100">
                  <span className="block text-xs font-bold text-[#3b3036]/50 uppercase tracking-wider">
                    Status Execução
                  </span>
                  <span className="mt-1 block">{getStatusBadge(selectedLog.status)}</span>
                </div>
              </div>

              {/* Pretty parsed details string */}
              {selectedLog.details && (
                <div className="space-y-2">
                  <span className="block text-xs font-bold text-[#3b3036]/60 uppercase tracking-wider">
                    Resumo do Payload / Detalhes
                  </span>
                  <pre className="bg-[#261f23] text-[#e5a49c] p-4 rounded-xl overflow-x-auto text-xs font-mono border border-black/10 max-h-40 shadow-inner">
                    {getPrettyDetails(selectedLog.details)}
                  </pre>
                </div>
              )}

              {/* Complete JSON Payload with copy tool */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="block text-xs font-bold text-[#3b3036]/60 uppercase tracking-wider">
                    JSON Completo do Log
                  </span>
                  <button
                    onClick={() => copyToClipboard(getFullLogJson(selectedLog))}
                    className="flex items-center gap-1 text-xs text-[#be8a83] hover:text-[#a6726b] font-semibold bg-gray-50 hover:bg-gray-100 px-2.5 py-1 rounded-lg border border-gray-200 transition-all cursor-pointer"
                  >
                    {copiedFullJson ? (
                      <>
                        <Check size={12} /> Copiado!
                      </>
                    ) : (
                      <>
                        <Copy size={12} /> Copiar JSON
                      </>
                    )}
                  </button>
                </div>
                <pre className="bg-[#1e191c] text-green-400 p-4 rounded-xl overflow-x-auto text-xs font-mono border border-black/20 max-h-64 shadow-inner">
                  {getFullLogJson(selectedLog)}
                </pre>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-100 bg-gray-50/50">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-5 py-2 bg-[#be8a83] text-white hover:bg-[#a6726b] font-semibold text-sm rounded-xl transition-all shadow-xs cursor-pointer"
              >
                Fechar Detalhes
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
