import api from '../../../../services/api';
import { normalizePage, type SpringPageResponse } from '../../../../utils/pagination';
export type { PageResponse } from '../../../../utils/pagination';

export type EmailOutboxStatus = 'PENDING' | 'SENT' | 'FAILED' | 'DEAD_LETTER';

/**
 * Sem corpo do e-mail (htmlContent) — a tela só precisa saber para quem/o quê/status, não o
 * conteúdo renderizado inteiro (minimização de dado, e evita listas paginadas pesadas).
 */
export interface EmailOutboxResponse {
  id: number;
  recipientEmail: string;
  subject: string;
  status: EmailOutboxStatus;
  attempts: number;
  nextRetryAt?: string | null;
  lastError?: string | null;
  relatedEntityType?: string | null;
  relatedEntityId?: number | null;
  createdAt: string;
  sentAt?: string | null;
}

export interface EmailOutboxFilter {
  /** String única, formato aceito pelo backend: "SENT" ou "FAILED,DEAD_LETTER" (vazio = todos). */
  statuses?: string;
}

export const emailOutboxApi = {
  findAll: async (filter: EmailOutboxFilter = {}, page = 0, size = 20) => {
    const { data } = await api.get<SpringPageResponse<EmailOutboxResponse>>('/email-outbox', {
      params: { ...filter, page, size },
    });
    return normalizePage(data);
  },

  resend: async (id: number) => {
    const { data } = await api.post<EmailOutboxResponse>(`/email-outbox/${id}/resend`);
    return data;
  },
};
