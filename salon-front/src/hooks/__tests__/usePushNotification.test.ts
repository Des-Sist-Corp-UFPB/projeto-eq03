import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { usePushNotification } from '../usePushNotification';
import { pushApi } from '../../services/push';

vi.mock('../../services/push', () => ({
  pushApi: {
    subscribe: vi.fn(),
    unsubscribe: vi.fn(),
  },
}));

describe('usePushNotification', () => {
  let getSubscription: ReturnType<typeof vi.fn>;
  let subscribe: ReturnType<typeof vi.fn>;
  let requestPermission: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();

    getSubscription = vi.fn();
    subscribe = vi.fn();
    requestPermission = vi.fn();

    Object.defineProperty(window, 'PushManager', {
      value: class {},
      configurable: true,
    });

    Object.defineProperty(window, 'Notification', {
      value: { requestPermission },
      configurable: true,
    });

    Object.defineProperty(navigator, 'serviceWorker', {
      value: {
        ready: Promise.resolve({
          pushManager: { getSubscription, subscribe },
        }),
      },
      configurable: true,
    });
  });

  afterEach(() => {
    // @ts-expect-error limpeza de stub de teste
    delete window.PushManager;
    // @ts-expect-error limpeza de stub de teste
    delete window.Notification;
    // @ts-expect-error limpeza de stub de teste
    delete navigator.serviceWorker;
  });

  it('does nothing when the user is not authenticated', async () => {
    renderHook(() => usePushNotification(false));

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(getSubscription).not.toHaveBeenCalled();
    expect(requestPermission).not.toHaveBeenCalled();
  });

  it('does not re-subscribe when a subscription already exists', async () => {
    getSubscription.mockResolvedValue({ endpoint: 'https://push.example.com/existing' });

    renderHook(() => usePushNotification(true));

    await waitFor(() => expect(getSubscription).toHaveBeenCalled());

    expect(requestPermission).not.toHaveBeenCalled();
    expect(subscribe).not.toHaveBeenCalled();
    expect(pushApi.subscribe).not.toHaveBeenCalled();
  });

  it('subscribes and sends the subscription to the backend when permission is granted', async () => {
    getSubscription.mockResolvedValue(null);
    requestPermission.mockResolvedValue('granted');
    subscribe.mockResolvedValue({
      toJSON: () => ({
        endpoint: 'https://push.example.com/new',
        keys: { p256dh: 'p256dh-value', auth: 'auth-value' },
      }),
    });

    renderHook(() => usePushNotification(true));

    await waitFor(() =>
      expect(pushApi.subscribe).toHaveBeenCalledWith({
        endpoint: 'https://push.example.com/new',
        p256dh: 'p256dh-value',
        auth: 'auth-value',
      })
    );
  });

  it('does not subscribe when the user denies permission', async () => {
    getSubscription.mockResolvedValue(null);
    requestPermission.mockResolvedValue('denied');

    renderHook(() => usePushNotification(true));

    await waitFor(() => expect(requestPermission).toHaveBeenCalled());

    expect(subscribe).not.toHaveBeenCalled();
    expect(pushApi.subscribe).not.toHaveBeenCalled();
  });
});
