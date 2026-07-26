/// <reference types="vitest" />
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: [
        'src/hooks/usePermission.ts',
        'src/context/AuthContext.tsx',
        'src/services/api.ts',
        'src/components/permissions/PermissionGate.tsx',
        'src/components/table/Table.tsx',
        'src/pages/appointments/PublicAppointment.tsx'
      ],
      thresholds: {
        statements: 85,
        branches: 85,
        functions: 85,
        lines: 85,
      },
    },
  },
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      // injectManifest (service worker escrito à mão em src/sw.ts) em vez de generateSW: é o
      // único jeito de registrar os listeners de 'push'/'notificationclick' (issue #110) — a
      // estratégia anterior gerava o SW automaticamente e não permitia customização de eventos.
      // O runtimeCaching que existia aqui foi reescrito manualmente dentro de src/sw.ts
      // (workbox-routing) para preservar o comportamento de cache anterior.
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.ts',
      injectManifest: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}']
      },
      registerType: 'autoUpdate',
      includeAssets: ['pwa-192x192.png', 'pwa-512x512.png'],
      manifest: {
        name: 'Espaço Cristiane Moura',
        short_name: 'Cristiane Moura',
        description: 'Salão de beleza — agendamentos e gestão',
        theme_color: '#ffffff',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait',
        scope: '/',
        start_url: '/',
        icons: [
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' }
        ]
      },
      devOptions: { enabled: true, type: 'module' }
    })
  ],
})
