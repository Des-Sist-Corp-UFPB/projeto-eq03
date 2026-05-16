import { useState, useEffect } from 'react';
import { Filter } from 'lucide-react';
import './AuditLog.css';
import api from '../../../services/api';

interface AuditLog {
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
  const PAGE_SIZE = 20;
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalItems, setTotalItems] = useState(0);
  const [page, setPage] = useState(0);
  const [filterAction, setFilterAction] = useState('');
  const [filterEntity, setFilterEntity] = useState('');
  const [filterUser, setFilterUser] = useState('');

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
    } catch (error) {
      console.error('Erro ao carregar logs de auditoria', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAuditLogs();
  }, [page, filterAction, filterEntity, filterUser]);

  const getStatusBadge = (status: string) => {
    return status === 'SUCCESS' 
      ? '✅ Sucesso' 
      : '❌ Falha';
  };

  const getActionColor = (action: string) => {
    switch (action.toUpperCase()) {
      case 'CREATE': return '#4CAF50';
      case 'UPDATE': return '#2196F3';
      case 'DELETE': return '#f44336';
      case 'LOGIN': return '#FF9800';
      case 'LOGOUT': return '#9C27B0';
      default: return '#9E9E9E';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('pt-BR');
  };

  return (
    <div className="audit-container">
      <div className="audit-header">
        <h2>📊 Log de Auditoria</h2>
        <p className="subtitle">Visualize todas as ações realizadas no sistema</p>
      </div>

      <div className="audit-filters">
        <div className="filter-group">
          <label>Filtrar por Usuário:</label>
          <input
            type="text"
            placeholder="Email do usuário"
            value={filterUser}
            onChange={(e) => {
              setFilterUser(e.target.value);
              setPage(0);
            }}
            className="filter-input"
          />
        </div>

        <div className="filter-group">
          <label>Filtrar por Ação:</label>
          <select
            value={filterAction}
            onChange={(e) => {
              setFilterAction(e.target.value);
              setPage(0);
            }}
            className="filter-input"
          >
            <option value="">Todas as ações</option>
            <option value="CREATE">Criar</option>
            <option value="UPDATE">Atualizar</option>
            <option value="DELETE">Deletar</option>
            <option value="LOGIN">Login</option>
            <option value="LOGOUT">Logout</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Filtrar por Entidade:</label>
          <select
            value={filterEntity}
            onChange={(e) => {
              setFilterEntity(e.target.value);
              setPage(0);
            }}
            className="filter-input"
          >
            <option value="">Todas as entidades</option>
            <option value="User">Usuário</option>
            <option value="Appointment">Agendamento</option>
            <option value="Service">Serviço</option>
            <option value="Product">Produto</option>
            <option value="Employee">Funcionário</option>
            <option value="CashFlow">Fluxo de Caixa</option>
          </select>
        </div>

        <button className="filter-reset" onClick={() => {
          setFilterAction('');
          setFilterEntity('');
          setFilterUser('');
          setPage(0);
        }}>
          <Filter size={18} /> Limpar Filtros
        </button>
      </div>

      {isLoading ? (
        <div className="loading">Carregando logs...</div>
      ) : (
        <>
          <div className="audit-table">
            <table>
              <thead>
                <tr>
                  <th>Data/Hora</th>
                  <th>Usuário</th>
                  <th>Ação</th>
                  <th>Entidade</th>
                  <th>ID Entidade</th>
                  <th>IP Address</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.map((log) => (
                  <tr key={log.id} className="audit-row">
                    <td className="date-cell">{formatDate(log.createdAt)}</td>
                    <td className="user-cell">{log.userEmail}</td>
                    <td>
                      <span 
                        className="action-badge" 
                        style={{ backgroundColor: getActionColor(log.action) }}
                      >
                        {log.action}
                      </span>
                    </td>
                    <td>{log.entityType}</td>
                    <td className="entity-id">{log.entityId || '-'}</td>
                    <td className="ip-cell">{log.ipAddress || '-'}</td>
                    <td>
                      <span className={`status-badge status-${log.status.toLowerCase()}`}>
                        {getStatusBadge(log.status)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pagination">
            <button 
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="pagination-btn"
            >
              ← Anterior
            </button>
            
            <span className="page-info">
              Página {page + 1} de {Math.ceil(totalItems / PAGE_SIZE)} 
              ({totalItems} registros)
            </span>
            
            <button 
              onClick={() => setPage(page + 1)}
              disabled={(page + 1) * PAGE_SIZE >= totalItems}
              className="pagination-btn"
            >
              Próxima →
            </button>
          </div>
        </>
      )}
    </div>
  );
};
