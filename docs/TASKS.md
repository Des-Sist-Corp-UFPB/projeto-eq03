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
- [ ] Write `V1__create_security_tables.sql` (tb_role, tb_permission, tb_user, join tables)
- [ ] Write `V2__insert_roles_permissions.sql` (seed ADMIN, GERENTE, FUNCIONARIA, CLIENTE + permissions)
- [ ] Configure `CorsConfig.java`
- [ ] Configure `OpenApiConfig.java` (Swagger)
- [ ] Configure `BeanConfig.java` (PasswordEncoder, etc.)

### Backend — Security
- [ ] Implement `User` entity + `Role` entity + `Permission` entity
- [ ] Implement `SecurityUserDetailsService`
- [ ] Implement `JwtService` (generate, validate, extract claims)
- [ ] Implement `JwtAuthenticationFilter`
- [ ] Implement `CustomPermissionEvaluator` (authority-per-endpoint check)
- [ ] Implement `EntityPermissionEvaluator` (object-level ownership check)
- [ ] Implement `VerifyUserPermissions` bean
- [ ] Configure `SecurityConfig` (filter chain, public routes, `@EnableMethodSecurity`)
- [ ] Implement `AuthController` (`/auth/login`, `/auth/refresh`, `/auth/register`)
- [ ] Implement `GlobalExceptionHandler` (401, 403, 404, 400, 500)

### Frontend — Base Setup
- [ ] Configure Axios instance (`services/api.ts`) with JWT interceptor + auto-refresh
- [ ] Implement `AuthContext` (token, role, authorities, login, logout, refresh)
- [ ] Implement `useAuth` hook
- [ ] Implement `usePermission` hook
- [ ] Create `Router.tsx` with all routes (protected + public)
- [ ] Create `DefaultLayout`, `AdminLayout`, `CustomerLayout`
- [ ] Create `Login` page (form + API call + redirect)
- [ ] Create `Register` page (customer self-registration)

---

## Sprint 2 — Admin CRUD

### Backend
- [ ] Write `V3__create_business_tables.sql` (tb_service, tb_product, tb_employee, tb_appointment, tb_cashflow)
- [ ] Implement `UserController` (GET list, GET by id, PATCH, DELETE) with permission guards
- [ ] Implement `ServiceController` (CRUD) — GET public, mutate admin-only
- [ ] Implement `ProductController` (CRUD) — GET public, mutate admin-only
- [ ] Implement `EmployeeController` (CRUD) — admin-only
- [ ] Unit tests for all service layer classes (Sprint 2 scope)

### Frontend
- [ ] Implement generic `Table` component (pagination, search, row actions)
- [ ] Implement `ModalForm` component (React Hook Form + Bootstrap Modal)
- [ ] Implement `ConfirmDialog` component
- [ ] Implement `PermissionGate` component
- [ ] Implement `Users` admin page (list, edit, delete)
- [ ] Implement `Employees` admin page (list, create, edit, delete)
- [ ] Implement `AdminServices` page (list, create, edit, delete)
- [ ] Implement `Products` admin page (list, create, edit, delete)
- [ ] All form validation errors displayed in **pt-BR**

---

## Sprint 3 — Appointments

### Backend
- [ ] Implement `AppointmentController`
  - [ ] `GET /appointments/slots?date=&employee_id=` — available time slots (public)
  - [ ] `POST /appointments` — client books (CLIENTE role)
  - [ ] `GET /appointments/my` — client's own appointments
  - [ ] `GET /appointments` — all appointments (ADMIN, GERENTE)
  - [ ] `PATCH /appointments/{id}/cancel` — cancel (owner or ADMIN)
  - [ ] `PATCH /appointments/{id}/status` — update status (ADMIN, FUNCIONARIA)
- [ ] Slot logic: block already-booked times per employee, respect service duration
- [ ] Unit + integration tests for appointment logic

### Frontend
- [ ] Implement `PublicAppointment` page
  - [ ] Step 1: select service
  - [ ] Step 2: select employee
  - [ ] Step 3: pick date + available slot
  - [ ] Step 4: confirm + submit (requires login redirect if anonymous)
- [ ] Implement `MyAppointments` page (list with cancel button)
- [ ] Implement `AdminAppointments` page (calendar view + list + status update)
- [ ] Implement `PublicServices` page (cards with service info)

---

## Sprint 4 — Financial & Dashboard

### Backend
- [ ] Implement `CashFlowController`
  - [ ] `GET /cashflow?from=&to=` — filtered listing
  - [ ] `POST /cashflow` — manual entry
  - [ ] `DELETE /cashflow/{id}`
- [ ] Auto-create INCOME cash flow entry when appointment status → DONE
- [ ] Implement `ReportController`
  - [ ] `GET /reports/financial?from=&to=` — revenue, expenses, net per period
  - [ ] `GET /reports/appointments?from=&to=` — counts by status, by employee, by service
- [ ] Unit tests for report aggregation logic

### Frontend
- [ ] Implement `Dashboard` page
  - [ ] KPI cards: appointments today, revenue this month, active clients, active employees
  - [ ] Line chart: revenue last 30 days (Recharts)
  - [ ] Bar chart: appointments per employee this month
- [ ] Implement `CashFlow` page (table + add entry form + delete)
- [ ] Implement `Reports` page
  - [ ] Date range picker
  - [ ] Financial summary (revenue, expenses, net)
  - [ ] Appointments breakdown chart
  - [ ] Export to PDF (jsPDF)

---

## Sprint 5 — Quality & Docs

### Backend
- [ ] Integration tests with Testcontainers for all controllers
- [ ] Achieve ≥ 80% coverage on service + controller layers
- [ ] Validate all Swagger annotations are accurate
- [ ] Review all error messages are in **pt-BR**

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
- [ ] `Profile` page (customer updates name, phone, password, photo)
- [ ] Password reset via email token flow

### Polish
- [ ] Loading skeletons on all data-fetching pages
- [ ] Empty states with helpful messages (in pt-BR)
- [ ] Global 404 page
- [ ] Favicon + `<title>` tags per page (SEO)
- [ ] Final UX review and responsive polish