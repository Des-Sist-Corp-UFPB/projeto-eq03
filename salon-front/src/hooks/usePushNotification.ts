import { useEffect } from 'react';
import { pushApi } from '../services/push';

const VAPID_PUBLIC_KEY = import.meta.env.VITE_VAPID_PUBLIC_KEY as string | undefined;

function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const output = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; i++) {
    output[i] = rawData.charCodeAt(i);
  }
  return output;
}

/**
 * Pede permissão de notificação e registra a assinatura de Web Push assim que houver um
 * usuário autenticado, uma vez por navegador/dispositivo (não reautoriza se já existir uma
 * subscription registrada no service worker).
 *
 * iOS: só recebe push se o PWA estiver instalado na tela inicial — o Safari comum (aba normal)
 * não implementa a Push API. Não há nada a fazer aqui além de deixar a assinatura falhar
 * silenciosamente nesse caso; é limitação da Apple, não do código.
 */
export function usePushNotification(isAuthenticated: boolean) {
  useEffect(() => {
    if (!isAuthenticated) return;
    if (!VAPID_PUBLIC_KEY) {
      // Sair calado aqui já custou um bug em produção: a chave não era passada como build-arg
      // no Dockerfile, então o bundle saía com ela undefined, o pedido de permissão nunca
      // aparecia, e o único sintoma visível era "o e-mail chegou mas a notificação não".
      console.warn(
        'VITE_VAPID_PUBLIC_KEY ausente no bundle — notificações push desativadas. ' +
          'Em produção ela precisa ser passada como build-arg no Dockerfile (ver cd.yml).'
      );
      return;
    }
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) return;

    let cancelled = false;

    async function subscribe() {
      try {
        const registration = await navigator.serviceWorker.ready;
        const existingSubscription = await registration.pushManager.getSubscription();
        if (existingSubscription) return;

        const permission = await Notification.requestPermission();
        if (permission !== 'granted' || cancelled) return;

        const subscription = await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY as string),
        });

        const raw = subscription.toJSON();
        if (!raw.endpoint || !raw.keys?.p256dh || !raw.keys?.auth) return;

        await pushApi.subscribe({
          endpoint: raw.endpoint,
          p256dh: raw.keys.p256dh,
          auth: raw.keys.auth,
        });
      } catch (err) {
        console.warn('Inscrição em notificações push falhou:', err);
      }
    }

    subscribe();

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated]);
}
