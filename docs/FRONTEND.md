# Frontend

Stack: **React 18 · TypeScript · Vite · Bootstrap 5 · Axios · React Router DOM · React Hook Form · Recharts · jsPDF**

> All user-facing text (labels, messages, toasts, validation errors) must be in **pt-BR**.

## Structure (`salon-front/src/`)

```
components/
  table/        # Reusable table: pagination, sorting, search, row actions
  modal/        # ModalForm (generic), ConfirmDialog
  form/         # Controlled inputs, masked fields
  charts/       # Recharts wrappers (line, bar, pie)
  feedback/     # Toast, Alert, Spinner
  permissions/  # PermissionGate
  loading/      # Skeleton loaders

context/
  AuthContext.tsx   # token, role, authorities, login(), logout(), refresh()

hooks/
  useAuth.ts        # consumes AuthContext
  usePermission.ts  # checks method+endpoint against authorities
  useApi.ts         # generic fetcher with loading/error state

layouts/
  DefaultLayout.tsx   # public navbar + footer
  AdminLayout.tsx     # sidebar + topbar + outlet
  CustomerLayout.tsx  # customer header + outlet

pages/
  public/    Home, PublicServices, PublicAppointment, Login, Register
  admin/     Dashboard, Users, Employees, Services, Products,
             Appointments, CashFlow, Reports
  customer/  MyAppointments, Profile

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

types/       # TypeScript interfaces matching API DTOs
utils/       # date formatting, currency masking, helpers
styles/      # global CSS overrides, Bootstrap customizations
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

**`ModalForm`** — wraps React Hook Form + Bootstrap Modal; accepts a `schema` (Zod/Yup) and `onSubmit` callback.

## Routes

```
/                       → Home            (DefaultLayout)
/services               → PublicServices  (DefaultLayout)
/appointment            → PublicAppointment (DefaultLayout)
/login                  → Login           (DefaultLayout)
/register               → Register        (DefaultLayout)

/admin/dashboard        → Dashboard       (AdminLayout, protected)
/admin/users            → Users           (AdminLayout, protected)
/admin/employees        → Employees       (AdminLayout, protected)
/admin/services         → AdminServices   (AdminLayout, protected)
/admin/products         → Products        (AdminLayout, protected)
/admin/appointments     → AdminAppointments (AdminLayout, protected)
/admin/cashflow         → CashFlow        (AdminLayout, protected)
/admin/reports          → Reports         (AdminLayout, protected)

/my-appointments        → MyAppointments  (CustomerLayout, protected)
/profile                → Profile         (CustomerLayout, protected)
```

Protected routes redirect to `/login` if no valid token. Admin routes additionally check for ADMIN or GERENTE role.

## Axios (`services/api.ts`)

- `baseURL` = `http://localhost:8080/v1` (dev) / env var (prod)
- Request interceptor: injects `Authorization: Bearer <token>`
- Response interceptor: on 401, calls refresh, retries original request once; on second 401, calls `logout()`