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
  admin/         # Users, Employees, Services, Products, Appointments, CashFlow, Reports
  appointments/  # PublicAppointment, MyAppointments
  auth/          # Login, Register
  home/          # PublicHome
  profile/       # Profile
  services/      # PublicServices
  sysadmin/      # FeatureFlags, AuditLog (admin console)

services/
  api.ts     # Axios instance (baseURL, JWT header, auto-refresh interceptor)
  auth.ts    # login, register, refresh calls
  users.ts   # user CRUD calls
  services.ts
  products.ts
  employees.ts
  appointments.ts
  cashflow.ts
  reports.ts
  featureFlags.ts

types/       # TypeScript interfaces matching API DTOs
utils/       # date formatting, currency masking, helpers
styles/      # CSS files
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

```
/                       → PublicHome        (DefaultLayout)
/services               → PublicServices    (DefaultLayout)
/appointment            → PublicAppointment (DefaultLayout)
/login                  → Login
/register               → Register

/admin/reports          → Reports           (AdminLayout, protected)
/admin/users            → Users             (AdminLayout, protected)
/admin/employees        → Employees         (AdminLayout, protected)
/admin/services         → AdminServices     (AdminLayout, protected)
/admin/products         → Products          (AdminLayout, protected)
/admin/appointments     → AdminAppointments (AdminLayout, protected)
/admin/cashflow         → CashFlow          (AdminLayout, protected)

/my-appointments        → MyAppointments    (CustomerLayout, protected)
/profile                → Profile           (CustomerLayout, protected)

/sysadmin/feature-flags → FeatureFlags      (SysadminLayout, protected)
/sysadmin/audit         → AuditLog          (SysadminLayout, protected)
```

Protected routes redirect to `/login` if no valid token. Admin/Sysadmin routes additionally check for appropriate roles.

## Axios (`services/api.ts`)

- `baseURL` = `/v1` (dev) / env var (prod)
- Request interceptor: injects `Authorization: Bearer <token>`
- Response interceptor: on 401, calls refresh, retries original request once; on second 401, calls `logout()`