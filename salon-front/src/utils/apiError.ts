import type { AxiosError } from 'axios';

type ErrorBody = {
  message?: string;
  error?: string;
  detail?: string;
};

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (error && typeof error === 'object' && 'response' in (error as any)) {
    const ax = error as AxiosError<ErrorBody | string>;
    const status = ax.response?.status;
    const data = ax.response?.data;

    if (data === undefined || data === null || data === '') {
      if (status === 401 || status === 403) {
        return 'Sua sessão expirou ou você não tem permissão. Faça login novamente e tente outra vez.';
      }
      if (ax.message === 'Network Error') {
        return 'Não foi possível conectar ao servidor. Verifique se o backend está rodando.';
      }
      return fallback;
    }

    if (typeof data === 'string') {
      return data.trim() || fallback;
    }

    const msg = data.message || data.detail || data.error;
    if (msg && typeof msg === 'string') return msg;
  }

  if (error instanceof Error && error.message) return error.message;
  return fallback;
}
