# Tasks / Roadmap

Status legend: `[ ]` todo · `[x]` done · `[-]` in progress

---

## Sprint 1 — Foundation

### Infrastructure
- [x] Create monorepo structure (`salon-back/`, `salon-front/`, `docs/`)
- [x] Initialize Spring Boot project (Java 21, Maven)
- [x] Initialize React + TypeScript + Vite project
- [x] Configure `docker-compose.yml` (PostgreSQL + backend + frontend)
- [ ] Create `.github/workflows/backend-ci.yml`
- [ ] Create `.github/workflows/frontend-ci.yml`
- [ ] Create `.github/workflows/deploy.yml`

### Backend — Base Setup
- [x] Configure `application.yml`, `application-dev.yml`, `application-prod.yml`
- [x] Configure Flyway
- [x] Write `V1__create_security_tables.sql` (tb_role, tb_permission, tb_user, join tables)
- [x] Write `V2__insert_roles_permissions.sql` (seed ADMIN, GERENTE, FUNCIONARIA, CLIENTE + permissions)
- [x] Configure `CorsConfig.java`
- [x] Configure `OpenApiConfig.java` (Swagger)
- [x] Configure `BeanConfig.java` (PasswordEncoder, etc.)

### Backend — Security
- [x] Implement `User` entity + `Role` entity + `Permission` entity
- [x] Implement `SecurityUserDetailsService`
- [x] Implement `JwtService` (generate, validate, extract claims)
- [x] Implement `JwtAuthenticationFilter`
- [x] Implement `CustomPermissionEvaluator` (authority-per-endpoint check)
- [x] Implement `EntityPermissionEvaluator` (object-level ownership check)
- [x] Implement `VerifyUserPermissions` bean
- [x] Configure `SecurityConfig` (filter chain, public routes, `@EnableMethodSecurity`)
- [x] Implement `AuthController` (`/auth/login`, `/auth/refresh`, `/auth/register`)
- [x] Implement `GlobalExceptionHandler` (401, 403, 404, 400, 500)

### Frontend — Base Setup
- [x] Configure Axios instance (`services/api.ts`) with JWT interceptor + auto-refresh
- [x] Implement `AuthContext` (token, role, authorities, login, logout, refresh)
- [x] Implement `useAuth` hook
- [x] Implement `usePermission` hook
- [x] Create `Router.tsx` with all routes (protected + public)
- [x] Create `DefaultLayout`, `AdminLayout`, `CustomerLayout`
- [x] Create `Login` page (form + API call + redirect)
- [x] Create `Register` page (customer self-registration)

---

## Sprint 2 — Admin CRUD

### Backend
- [x] Write `V3__create_business_tables.sql` (tb_service, tb_product, tb_employee, tb_appointment, tb_cashflow)
- [x] Implement `UserController` (GET list, GET by id, PATCH, DELETE) with permission guards
- [x] Implement `ServiceController` (CRUD) — GET public, mutate admin-only
- [x] Implement `ProductController` (CRUD) — GET public, mutate admin-only
- [x] Implement `EmployeeController` (CRUD) — admin-only
- [ ] Unit tests for all service layer classes (Sprint 2 scope)

### Frontend
- [x] Implement generic `Table` component (pagination, search, row actions)
- [x] Implement `ModalForm` component (React Hook Form + Bootstrap Modal)
- [x] Implement `ConfirmDialog` component
- [x] Implement `PermissionGate` component
- [x] Implement `Users` admin page (list, edit, delete)
- [x] Implement `Employees` admin page (list, create, edit, delete)
- [x] Implement `AdminServices` page (list, create, edit, delete)
- [x] Implement `Products` admin page (list, create, edit, delete)
- [x] All form validation errors displayed in **pt-BR**

---

## Sprint 3 — Appointments

### Backend
- [x] Implement `AppointmentController`
  - [x] `GET /appointments/slots?date=&employee_id=` — available time slots (public)
  - [x] `POST /appointments` — client books (CLIENTE role)
  - [x] `GET /appointments/my` — client's own appointments
  - [x] `GET /appointments` — all appointments (ADMIN, GERENTE)
  - [x] `PATCH /appointments/{id}/cancel` — cancel (owner or ADMIN)
  - [x] `PATCH /appointments/{id}/status` — update status (ADMIN, FUNCIONARIA)
- [x] Slot logic: block already-booked times per employee, respect service duration
- [ ] Unit + integration tests for appointment logic

### Frontend
- [x] Implement `PublicAppointment` page
  - [x] Step 1: select service
  - [x] Step 2: select employee
  - [x] Step 3: pick date + available slot
  - [x] Step 4: confirm + submit (requires login redirect if anonymous)
- [x] Implement `MyAppointments` page (list with cancel button)
- [x] Implement `AdminAppointments` page (calendar view + list + status update)
- [x] Implement `PublicServices` page (cards with service info)

---

## Sprint 4 — Financial & Dashboard

### Backend
- [x] Implement `CashFlowController`
  - [x] `GET /cashflow?from=&to=` — filtered listing
  - [x] `POST /cashflow` — manual entry
  - [x] `DELETE /cashflow/{id}`
- [x] Auto-create INCOME cash flow entry when appointment status → DONE
- [x] Implement `ReportController`
  - [x] `GET /reports/financial?from=&to=` — revenue, expenses, net per period
  - [x] `GET /reports/appointments?from=&to=` — counts by status, by employee, by service
- [x] Unit tests for report aggregation logic

### Frontend
- [x] Implement `Dashboard` page
  - [x] KPI cards: appointments today, revenue this month, active clients, active employees
  - [x] Line chart: revenue last 30 days (Recharts)
  - [x] Bar chart: appointments per employee this month
- [x] Implement `CashFlow` page (table + add entry form + delete)
- [x] Implement `Reports` page
  - [x] Date range picker
  - [x] Financial summary (revenue, expenses, net)
  - [x] Appointments breakdown chart
  - [x] Export to PDF (jsPDF)

---

## Sprint 5 — Quality & Docs

### Backend
- [ ] Integration tests with Testcontainers for all controllers
- [ ] Achieve ≥ 80% coverage on service + controller layers
- [x] Validate all Swagger annotations are accurate
- [x] Review all error messages are in **pt-BR**

### Frontend
- [ ] Unit tests (Vitest + RTL) for: Table, ModalForm, PermissionGate, AuthContext
- [ ] E2E tests (Cypress): login flow, admin CRUD, booking flow, cancel appointment
- [ ] Accessibility audit (labels, aria, keyboard nav)
- [ ] Responsive QA on mobile (≤ 768px) and tablet

---

## Sprint 6 — Production

### Infrastructure
- [ ] Finalize `docker-compose.yml` for production (volumes, restart policies, healthchecks)
- [ ] Configure Nginx (reverse proxy, gzip, HTTPS redirect)
- [ ] Obtain SSL certificate (Let's Encrypt / Certbot)
- [ ] Wire GitHub Actions deploy pipeline end-to-end
- [ ] Set all GitHub secrets (DB creds, JWT secret, SSH key, API URL)

### Features
- [ ] Email notification on appointment confirmed/cancelled (JavaMailSender)
- [ ] Profile image upload (store in filesystem or S3-compatible bucket)
- [x] `Profile` page (customer updates name, phone, password, photo)
- [ ] Password reset via email token flow

### Polish
- [ ] Loading skeletons on all data-fetching pages
- [ ] Empty states with helpful messages (in pt-BR)
- [x] Global 404 page
- [ ] Favicon + `<title>` tags per page (SEO)
- [ ] Final UX review and responsive polish