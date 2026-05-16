// Auto-login helper for local development only.
// Seeds localStorage with access/refresh tokens using the default seeded admin.
// Runs only when Vite is in dev mode to avoid leaking credentials to production.
if (import.meta.env.DEV) {
  (async () => {
    try {
      if (typeof window === 'undefined') return;
      const existing = localStorage.getItem('@Salon:token');
      if (existing) return;

      const resp = await fetch('http://localhost:8080/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'admin@salao.com', password: 'Admin@123' }),
      });

      if (!resp.ok) return;
      const data = await resp.json();
      if (data?.accessToken && data?.refreshToken) {
        localStorage.setItem('@Salon:token', data.accessToken);
        localStorage.setItem('@Salon:refreshToken', data.refreshToken);
        // give app a chance to pick up auth state
        console.info('Auto-login: tokens stored');
      }
    } catch (err) {
      // ignore; dev helper should not break app
      console.debug('Auto-login failed', err);
    }
  })();
}
