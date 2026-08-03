# Frontend

Stack: **React 19 · TypeScript · Vite · Vitest · Tailwind CSS v4.0 · Axios · React Router DOM v7 · React Hook Form · Recharts · jsPDF · PWA**

> All user-facing text (labels, messages, toasts, validation errors) must be in **pt-BR**.

## Structure (`salon-front/src/`)

```
components/
  table/        # Reusable table: pagination, sorting, search, row actions
  modal/        # ModalForm (generic), ConfirmDialog
  form/         # Controlled inputs, masked fields
  layout/       # Shared layout pieces
  feedback/     # Toast, Alert, Spinner
  permissions/  # PermissionGate
  loading/      # Skeleton loaders

context/
  AuthContext.tsx   # token, role, authorities, login(), logout(), refresh()

hooks/
  useAuth.ts        # consumes AuthContext
  usePermission.ts  # checks method+endpoint against authorities
  useAlert.ts       # customizable alerts

layouts/
  DefaultLayout.tsx   # public navbar + footer
  AdminLayout.tsx     # sidebar + topbar + outlet
  CustomerLayout.tsx  # customer header + outlet
  SysadminLayout.tsx  # sysadmin sidebar + outlet

pages/
  admin/
    users/           # Users
    clients/         # Clients (commercial client management)
    staff/           # StaffRegistration (team registration: CPF, PIX, address)
    employees/       # Employees
    services/        # AdminServices
    products/        # Products
    appointments/    # AdminAppointments
    cashflow/        # CashFlow
    reports/         # Reports
    recommendations/ # Recommendations (AI-generated insights)
    email-outbox/    # EmailOutbox
    salon-profile/   # SalonProfile (public profile + business hours)
    audit/           # AuditLog
  appointments/  # PublicAppointment, MyAppointments
  auth/          # Login, Register, ForgotPassword, ResetPassword
  home/          # PublicHome
  profile/       # Profile
  services/      # PublicServices
  error/         # NotFound
  sysadmin/      # FeatureFlags, AuditLog, Rbac, AiConfig (admin console)

services/
  api.ts           # Axios instance (baseURL, JWT header, auto-refresh interceptor)
  featureFlags.ts
  rbac.ts          # role/permission management calls
  aiConfig.ts       # AI provider config (sysadmin)
  mcpTokens.ts      # MCP token management (sysadmin)
  recommendations.ts
  push.ts           # push subscribe/unsubscribe calls
  salonProfile.ts

# Several page folders keep their own domain services instead of the shared services/ dir:
# pages/admin/{cashflow,clients,email-outbox,employees,products,staff,users}/services/*.ts
# pages/{appointments,profile,services}/services/*.ts

types/       # TypeScript interfaces matching API DTOs
utils/       # date formatting, currency masking, helpers
styles/      # CSS files
sw.ts        # Service worker (Web Push)
Router.tsx
App.tsx
main.tsx
```

## Key Components

**`PermissionGate`** — renders children only if the user holds the required authority:
```tsx
<PermissionGate method="DELETE" endpoint="/v1/users/*">
  <DeleteButton />
</PermissionGate>
```

**`usePermission`** — hook for imperative permission checks:
```tsx
const canDelete = usePermission("DELETE", "/v1/users/*");
```

**`Table`** — generic CRUD table accepting `columns`, `data`, `onEdit`, `onDelete` props; handles pagination and search client-side or via query params.

**`ModalForm`** — wraps React Hook Form + Tailwind Modal; handles custom submission callbacks.

## Routes

Derived directly from `Router.tsx`.

```
/                       → PublicHome        (DefaultLayout)
/services               → PublicServices    (DefaultLayout)
/appointment            → PublicAppointment (DefaultLayout)
/login                  → Login
/register               → Register
/forgot-password        → ForgotPassword
/reset-password         → ResetPassword

/admin                  → redirect to the first admin section the logged-in role can access
                          (e.g. FUNCIONARIA lands on /admin/appointments, not /admin/reports)
/admin/dashboard        → same redirect as /admin                (AdminLayout, protected)
/admin/clients          → Clients           (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/users            → Users             (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/employees        → Employees         (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/staff            → StaffRegistration (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/services         → AdminServices     (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/products         → Products          (AdminLayout, protected: ADMIN)
/admin/appointments     → AdminAppointments (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO, FUNCIONARIA)
/admin/cashflow         → CashFlow          (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/reports          → Reports           (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/recommendations  → Recommendations   (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/email-outbox     → EmailOutbox       (AdminLayout, protected: ADMIN, GERENTE_DE_ATENDIMENTO)
/admin/salon-profile    → SalonProfile      (AdminLayout, protected: ADMIN)

/my-appointments        → MyAppointments    (CustomerLayout, protected)
/profile                → Profile           (CustomerLayout, protected)

/sysadmin                → redirect to /sysadmin/feature-flags
/sysadmin/feature-flags → FeatureFlags      (SysadminLayout, protected: SYSADMIN)
/sysadmin/audit         → AuditLog          (SysadminLayout, protected: SYSADMIN)
/sysadmin/rbac          → Rbac              (SysadminLayout, protected: SYSADMIN)
/sysadmin/ai-config     → AiConfig          (SysadminLayout, protected: SYSADMIN)

*                        → NotFound
```

Protected routes redirect to `/login` if no valid token; `ProtectedRoute` additionally checks `allowedRoles` where specified.

**Maintenance-mode branch:** on mount, `Router` fetches the public feature flags and checks `ENABLE_CUSTOMER_PORTAL`. If it's off, the public/customer routes (`/`, `/services`, `/appointment`, `/my-appointments`, `/profile`) are replaced by a single `MaintenancePage`, while `/login`, `/register`, `/admin/**` and `/sysadmin/**` keep working normally — this lets staff keep using the system while the public booking portal is taken down for adjustments. If the flag fetch fails outright (network error), the portal is assumed enabled rather than blocking the whole site.

## Push Notifications (PWA)

`usePushNotification.ts` (called once from `Router`, gated on `isAuthenticated`) registers the service worker (`sw.ts`), requests the browser's push permission, subscribes using the `VITE_VAPID_PUBLIC_KEY` build-time env var, and posts the subscription to `POST /v1/push/subscribe` via `services/push.ts`. `sw.ts` handles the `push` and `notificationclick` events to display and route notifications (e.g. appointment reminders). See [CI_CD.md](./CI_CD.md) for how `VITE_VAPID_PUBLIC_KEY` gets embedded into the build.

## Axios (`services/api.ts`)

- `baseURL` = `/v1` (dev) / env var (prod)
- Request interceptor: injects `Authorization: Bearer <token>`
- Response interceptor: on 401, calls refresh, retries original request once; on second 401, calls `logout()`