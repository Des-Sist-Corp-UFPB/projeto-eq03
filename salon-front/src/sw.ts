/// <reference lib="webworker" />
// Excluído do tsconfig do app (ver tsconfig.app.json) — este arquivo roda no escopo de
// ServiceWorkerGlobalScope, não de Window/DOM, e o projeto principal usa lib "DOM".

import { clientsClaim } from 'workbox-core';
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching';
import { registerRoute } from 'workbox-routing';
import { NetworkFirst } from 'workbox-strategies';
import { ExpirationPlugin } from 'workbox-expiration';

declare const self: ServiceWorkerGlobalScope;

// Ativa a versão nova do SW imediatamente, sem esperar todas as abas fecharem — mesmo
// comportamento que registerType: 'autoUpdate' já dava com a estratégia generateSW anterior.
self.skipWaiting();
clientsClaim();

precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();

// Mesma regra de cache que existia em vite.config.ts (workbox.runtimeCaching) antes da troca
// pra injectManifest — reescrita à mão porque essa opção só existe na estratégia generateSW.
registerRoute(
  ({ url }) => /\/v1\/(services|employees\/booking|feature-flags|salon\/profile)/.test(url.pathname),
  new NetworkFirst({
    cacheName: 'api-public-cache',
    plugins: [new ExpirationPlugin({ maxEntries: 50, maxAgeSeconds: 86400 })],
  })
);

// Notificações push (issue #110). O payload é sempre JSON: { title, body, url } — ver
// PushService.java (backend) para o formato exato.
self.addEventListener('push', (event: PushEvent) => {
  if (!event.data) return;

  const data = event.data.json() as { title: string; body: string; url?: string };
  event.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      icon: '/pwa-192x192.png',
      badge: '/pwa-192x192.png',
      data: { url: data.url || '/' },
    })
  );
});

// Clique na notificação: foca uma aba já aberta na rota certa, ou abre uma nova.
self.addEventListener('notificationclick', (event: NotificationEvent) => {
  event.notification.close();
  const url = (event.notification.data?.url as string) || '/';

  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then((clientList) => {
      const existing = clientList.find((c) => c.url.includes(url)) as WindowClient | undefined;
      if (existing) {
        return existing.focus();
      }
      return self.clients.openWindow(url);
    })
  );
});
