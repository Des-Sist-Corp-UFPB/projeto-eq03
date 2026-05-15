# Frontend — React + TypeScript + Vite

---

## Estrutura do Frontend

```text
frontend/
├── src/
│
├── components/
│   ├── table/
│   ├── modal/
│   ├── form/
│   ├── charts/
│   ├── feedback/
│   ├── permissions/
│   └── loading/
│
├── context/
│
├── hooks/
│
├── layouts/
│
├── pages/
│   ├── admin/
│   ├── public/
│   ├── customer/
│   └── auth/
│
├── services/
│
├── styles/
│
├── types/
│
├── utils/
│
├── Router.tsx
├── App.tsx
└── main.tsx
```

---

# Componentes Reutilizáveis

## Table

Tabela reutilizável para CRUDs.

Características:

- Paginação
- Ordenação
- Busca
- Ações customizadas
- Responsiva

---

## ModalForm

Modal reutilizável para formulários.

Características:

- Validação
- Submit genérico
- Feedback visual

---

## ConfirmDialog

Usado para:

- Exclusões
- Confirmações críticas

---

## PermissionGate

Renderiza conteúdo baseado em permissões.

Exemplo:

```tsx
<PermissionGate
    method="DELETE"
    endpoint="/v1/users/*"
>
    <DeleteButton />
</PermissionGate>
```

---

# Layouts

## DefaultLayout

Área pública:

- Home
- Serviços
- Produtos
- Agendamento
- Login

---

## AdminLayout

Área administrativa:

- Sidebar
- Dashboard
- Gestão completa

---

## CustomerLayout

Área do cliente:

- Perfil
- Agendamentos
- Histórico

---

# AuthContext

Responsável por:

- Armazenar token
- Armazenar role
- Armazenar authorities
- Login
- Logout
- Refresh token

---

# usePermission

Hook responsável por verificar permissões.

Exemplo:

```tsx
const canDelete =
    usePermission(
        "DELETE",
        "/v1/users/*"
    );
```

---

# Axios

Arquivo:

```text
services/api.ts
```

Responsável por:

- Base URL
- Interceptors
- Refresh automático
- Headers JWT

---

# Router.tsx

```tsx
<Routes>

  <Route element={<DefaultLayout />}>

    <Route
        path="/"
        element={<Home />}
    />

    <Route
        path="/services"
        element={<PublicServices />}
    />

    <Route
        path="/appointment"
        element={<PublicAppointment />}
    />

    <Route
        path="/login"
        element={<Login />}
    />

    <Route
        path="/register"
        element={<Register />}
    />

  </Route>

  <Route element={<AdminLayout />}>

    <Route
        path="/admin/dashboard"
        element={<Dashboard />}
    />

    <Route
        path="/admin/users"
        element={<Users />}
    />

    <Route
        path="/admin/employees"
        element={<Employees />}
    />

    <Route
        path="/admin/services"
        element={<AdminServices />}
    />

    <Route
        path="/admin/products"
        element={<Products />}
    />

    <Route
        path="/admin/appointments"
        element={<AdminAppointments />}
    />

    <Route
        path="/admin/cashflow"
        element={<CashFlow />}
    />

    <Route
        path="/admin/reports"
        element={<Reports />}
    />

  </Route>

  <Route element={<CustomerLayout />}>

    <Route
        path="/my-appointments"
        element={<MyAppointments />}
    />

    <Route
        path="/profile"
        element={<Profile />}
    />

  </Route>

</Routes>
```

---