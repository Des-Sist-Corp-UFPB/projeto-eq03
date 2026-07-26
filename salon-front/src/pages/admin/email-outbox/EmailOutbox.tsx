import { useState } from 'react';
import { RefreshCw, Mail } from 'lucide-react';
import { DataTable } from '../../../components/table/DataTable';
import type { FilterField } from '../../../components/table/DataTable';
import { emailOutboxApi } from './services/emailOutbox';
import type { EmailOutboxResponse, EmailOutboxFilter, EmailOutboxStatus } from './services/emailOutbox';
import { getApiErrorMessage } from '../../../utils/apiError';
import { useAlert } from '../../../hooks/useAlert';

const STATUS_LABELS: Record<EmailOutboxStatus, string> = {
  PENDING: 'Pendente',
  SENT: 'Enviado',
  FAILED: 'Falhou (tentando de novo)',
  DEAD_LETTER: 'Falhou (desistiu)',
};

const STATUS_BADGE_CLASS: Record<EmailOutboxStatus, string> = {
  PENDING: 'bg-gray-100 text-gray-700',
  SENT: 'bg-green-100 text-green-700',
  FAILED: 'bg-amber-100 text-amber-700',
  DEAD_LETTER: 'bg-red-100 text-red-700',
};

const filtersConfig: FilterField[] = [
  {
    key: 'statuses',
    label: 'Status',
    type: 'select',
    options: [
      { value: 'SENT', label: 'Enviado' },
      { value: 'FAILED,DEAD_LETTER', label: 'Falhou' },
    ],
  },
];

export const EmailOutbox = () => {
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [resendingId, setResendingId] = useState<number | null>(null);
  const { error: showError, success: showSuccess } = useAlert();

  const fetchData = async (filter: EmailOutboxFilter, page: number, size: number) => {
    return emailOutboxApi.findAll(filter, page, size);
  };

  const handleResend = async (id: number) => {
    setResendingId(id);
    try {
      await emailOutboxApi.resend(id);
      await showSuccess('Reenvio realizado com sucesso.');
      setRefreshTrigger((prev) => prev + 1);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao reenviar e-mail. Tente novamente.');
      await showError(msg);
    } finally {
      setResendingId(null);
    }
  };

  const columns = [
    { key: 'recipientEmail', label: 'Destinatário' },
    { key: 'subject', label: 'Assunto' },
    {
      key: 'relatedEntityType',
      label: 'Contexto',
      render: (item: EmailOutboxResponse) =>
        item.relatedEntityType ? `${item.relatedEntityType} #${item.relatedEntityId}` : '—',
    },
    {
      key: 'status',
      label: 'Status',
      render: (item: EmailOutboxResponse) => (
        <span
          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${STATUS_BADGE_CLASS[item.status]}`}
        >
          {STATUS_LABELS[item.status]}
        </span>
      ),
    },
    { key: 'attempts', label: 'Tentativas' },
    {
      key: 'createdAt',
      label: 'Criado em',
      render: (item: EmailOutboxResponse) => new Date(item.createdAt).toLocaleString('pt-BR'),
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: EmailOutboxResponse) =>
        item.status === 'FAILED' || item.status === 'DEAD_LETTER' ? (
          <button
            type="button"
            onClick={() => handleResend(item.id)}
            disabled={resendingId === item.id}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-accent/10 text-accent hover:bg-accent/20 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={14} className={resendingId === item.id ? 'animate-spin' : ''} />
            Reenviar
          </button>
        ) : (
          <span className="text-xs text-[#7a7074]">—</span>
        ),
    },
  ];

  return (
    <div className="space-y-6 animate-fade-in-up">
      <div className="flex items-center gap-3">
        <div className="w-11 h-11 bg-accent/10 rounded-xl flex items-center justify-center text-accent">
          <Mail size={22} />
        </div>
        <div>
          <h1 className="font-heading text-2xl font-bold text-[#3b3036] m-0">Central de E-mails</h1>
          <p className="text-sm text-[#7a7074] m-0">
            Envios recentes (últimos 7 a 90 dias, conforme o status) e fila de retry automático.
            Para o histórico permanente de todo envio, consulte o Log de Auditoria.
          </p>
        </div>
      </div>

      <DataTable<EmailOutboxResponse, EmailOutboxFilter>
        columns={columns}
        fetchData={fetchData}
        filtersConfig={filtersConfig}
        keyExtractor={(item) => item.id}
        refreshTrigger={refreshTrigger}
        initialFilters={{}}
      />
    </div>
  );
};
