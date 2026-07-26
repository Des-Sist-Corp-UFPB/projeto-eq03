import api from './api';

export interface PushSubscribePayload {
  endpoint: string;
  p256dh: string;
  auth: string;
}

export const pushApi = {
  subscribe: async (payload: PushSubscribePayload) => {
    await api.post('/push/subscribe', payload);
  },

  unsubscribe: async (endpoint: string) => {
    await api.delete('/push/unsubscribe', { data: { endpoint } });
  },
};
