# Project Map & Developer Guide (Sass Salon)

This file serves as a quick reference map for AI agents and developers to quickly navigate and identify where each feature or business logic is located, saving time and token usage.

---

## 🏗️ Monorepo Structure

The project is split into two main parts:
*   [salon-back](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back): REST API built with Java 21, Spring Boot 3.4.6, and PostgreSQL.
*   [salon-front](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front): Frontend built with React 18, TypeScript, Vite, and Bootstrap 5.
*   [docs](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/docs): Detailed documentation regarding architecture, API, security, database, and tasks.

---

## 🛠️ Backend Distribution (`salon-back`)

The API follows a traditional layered architecture (`Controller` ➔ `Service` ➔ `Repository`) using DTOs (Java `record` objects) and entities managed via Lombok and JPA.

### 📁 Core Packages and Responsibilities

*   **Global Configuration (`config/`)**:
    *   [CorsConfig.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/config/CorsConfig.java): CORS origin permissions (open in local dev).
    *   [SecurityConfig.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/config/SecurityConfig.java): HTTP security config, filters, public routes, and protection rules.
    *   [MethodSecurityConfig.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/config/MethodSecurityConfig.java): Enables `@PreAuthorize` and binds the custom permission evaluator.

*   **Security & Authorization (`security/`)**:
    *   [JwtService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/security/JwtService.java): Emits, parses, and validates JWT Access and Refresh tokens.
    *   [JwtAuthenticationFilter.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/security/JwtAuthenticationFilter.java): Intercepts requests, validates the Bearer Token, and injects the user into the Spring Security Context.
    *   [CustomPermissionEvaluator.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/security/CustomPermissionEvaluator.java): Validates granular permissions with the `METHOD:ENDPOINT` pattern (e.g. `GET:/v1/users`).
    *   [EntityPermissionEvaluator.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/security/EntityPermissionEvaluator.java): Validates object-level properties (e.g., checking if a customer is accessing their own data).
    *   [VerifyUserPermissions.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/security/VerifyUserPermissions.java): Global bean `@verifyUserPermissions` used in `@PreAuthorize` controller annotations to combine ownership and explicit authority checks.

*   **Audit Logging (`aspect/` & `annotation/`)**:
    *   [Auditable.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/annotation/Auditable.java): Custom `@Auditable` annotation to mark critical methods.
    *   [AuditAspect.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/aspect/AuditAspect.java): AOP aspect that intercepts auditable methods to automatically log success or failure into the database audit table.

*   **Exception Handling (`exception/`)**:
    *   [GlobalExceptionHandler.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/exception/GlobalExceptionHandler.java): Centralizes JSON error responses with messages formatted for the end-user in **pt-BR**.

*   **Domain Models (`models/`)**:
    *   Each subfolder in `models/` represents a business domain and groups:
        *   `entity/`: JPA mapping with PostgreSQL tables.
        *   `dto/`: Java record files for Request/Response (Convention: never use `DTO` suffix, always `*Request` or `*Response`).
        *   `repository/`: Interfaces extending Spring Data JPA.
        *   `service/`: Core business logic.

| Business Domain | Entity Class | Primary Service | Controller Class |
| :--- | :--- | :--- | :--- |
| Auth / JWT | [user/entity/User.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/user/entity/User.java) | [AuthService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/user/service/AuthService.java) | [AuthController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/AuthController.java) |
| Users | [user/entity/User.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/user/entity/User.java) | [UserService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/user/service/UserService.java) | [UserController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/UserController.java) |
| Salon Services | [service/entity/SalonService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/service/entity/SalonService.java) | [SalonServiceManager.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/service/service/SalonServiceManager.java) | [SalonServiceController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/SalonServiceController.java) |
| Products | [product/entity/Product.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/product/entity/Product.java) | [ProductService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/product/service/ProductService.java) | [ProductController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/ProductController.java) |
| Employees | [employee/entity/Employee.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/employee/entity/Employee.java) | [EmployeeService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/employee/service/EmployeeService.java) | [EmployeeController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/EmployeeController.java) |
| Appointments | [appointment/entity/Appointment.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/appointment/entity/Appointment.java) | [AppointmentService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/appointment/service/AppointmentService.java) | [AppointmentController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/AppointmentController.java) |
| Cash Flow | [cashflow/entity/CashFlow.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/cashflow/entity/CashFlow.java) | [CashFlowService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/cashflow/service/CashFlowService.java) | [CashFlowController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/CashFlowController.java) |
| Reports | *DTOs only* | [ReportService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/report/service/ReportService.java) | [ReportController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/ReportController.java) |
| Audit Logs | [audit/AuditLog.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/audit/AuditLog.java) | [AuditLogService.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/models/audit/AuditLogService.java) | [AuditController.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/controller/AuditController.java) |

---

## 💻 Frontend Distribution (`salon-front`)

The frontend is built with React, consuming protected routes through a custom auth hook that handles tokens and auto-refresh interceptors.

### 📁 Key Components and Pages

*   **Routing & Authentication**:
    *   [Router.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/Router.tsx): Declares public paths, administrative panels, and customer-only screens.
    *   [ProtectedRoute.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/components/ProtectedRoute.tsx): Restricts access based on login status and role (`requiredRole`).
    *   [AuthContext.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/context/AuthContext.tsx): React context that handles credentials, decodes JWT payloads, and stores authorities in state.
    *   [useAuth.ts](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/hooks/useAuth.ts): Custom hook to easily consume AuthContext.
    *   [usePermission.ts](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/hooks/usePermission.ts): Imperative hook to check if the current user has a specific authority.
    *   [PermissionGate.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/components/permissions/PermissionGate.tsx): Declarative component to show/hide elements conditionally based on permissions.

*   **API Client (`services/`)**:
    *   [api.ts](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/services/api.ts): Centralized Axios instance. Automatically injects `Authorization: Bearer <token>` and intercepts responses to perform **silent JWT Refresh** using local storage refresh tokens on 401 errors. Redirects to `/login` if refresh fails or on 403.

*   **Public Area**:
    *   [PublicHome.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/home/PublicHome.tsx): Home landing page.
    *   [PublicServices.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/services/PublicServices.tsx): Displays available salon services, active status, duration estimate, and base prices.
    *   [PublicAppointment.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/appointments/PublicAppointment.tsx): Interactive multi-step booking wizard (1. Service ➔ 2. Professional ➔ 3. Preferred Date + Notes ➔ 4. Confirm). If anonymous, stores data in local storage and redirects to login before submitting.

*   **Admin Area (Admin / Gerente)**:
    *   [Reports.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/admin/reports/Reports.tsx): Visual dashboard containing financial charts, appointment metrics (using Recharts), and PDF export functionality (`jsPDF`).
    *   [AdminAppointments.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/admin/appointments/AdminAppointments.tsx): Backend appointment list view with tools for updating status and scheduling.
    *   [CashFlow.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/admin/cashflow/CashFlow.tsx): Ledger for income and expense records.
    *   [AuditLog.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/admin/audit/AuditLog.tsx): Admin tool to read system audit trails.

*   **Customer Dashboard**:
    *   [MyAppointments.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/appointments/MyAppointments.tsx): History of current requests and confirmed schedules, with cancellation options.
    *   [Profile.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/pages/profile/Profile.tsx): Edits customer's profile (name, phone, password).

*   **UI Reusable Assets**:
    *   [Table.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/components/table/Table.tsx): Generic paginated and searchable grid.
    *   [ConfirmDialog.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/components/modal/ConfirmDialog.tsx) and [ModalForm.tsx](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/components/modal/ModalForm.tsx): Generalized modal containers for confirmations and CRUD forms.

---

## 🗄️ Database Model & Flyway Migrations

Database tables are sequentially generated via migrations under `salon-back/src/main/resources/db/migration/`.

### 🚨 Golden Rule of Flyway Migrations
> **NEVER** edit a migration file that has already been executed or committed (e.g. `V1__`, `V2__`, etc.). Flyway validates file checksums on startup, and any difference will crash the backend. If you need to make database structure modifications, **ALWAYS** write a new sequential migration script (e.g., `V9__your_schema_change.sql`).

### Physical Schema Mapping

1.  `tb_role`: Role names (`ADMIN`, `GERENTE_DE_ATENDIMENTO`, `FUNCIONARIA`, `CLIENTE`).
2.  `tb_permission`: Granular mapping linking a name to an `http_method` + `endpoint` pattern (e.g., `PATCH`, `/v1/appointments/*/confirm`).
3.  `tb_role_permissions`: Join table linking roles to permissions (many-to-many).
4.  `tb_user`: Basic users storing email (unique), BCrypt-hashed password, phone number, active status, and `role_id`.
5.  `tb_salon_service`: Services containing base price (nullable), duration (in minutes and text), and active boolean.
6.  `tb_product`: Sales products containing stock and base price.
7.  `tb_employee`: Linked one-to-one to a `tb_user` object; contains custom bio text.
8.  `tb_appointment`:
    *   Links client, employee, and salon service.
    *   Can store `scheduled_at` (confirmed datetime) or `preferred_date` (requested date from wizard).
    *   Stores `status` (`PENDING`, `REQUESTED`, `CONFIRMED`, `DECLINED`, `DONE`, `CANCELLED`).
9.  `tb_cashflow`: Ledgers for income and expense transactions. Can link to a concluded appointment.
10. `tb_audit_log`: System logs capturing actor, action, target entity type, success status, request details, and IP address.

---

## 🔄 Critical Business Flows

### 1. Booking Request & Confirmation Lifecycle
*   **Customer Booking Request**: Customers requesting appointments do not pick exact times directly to prevent conflicts. The system saves the request with status **`REQUESTED`** storing only `preferred_date` and `client_notes`.
*   **Staff Confirmation**: Admins or reception managers confirm requests via `PATCH /appointments/{id}/confirm` sending a designated `scheduled_at` timestamp.
    *   **Schedule Collision Check**: The backend verifies all confirmed appointments on that day for the selected professional. It checks if the requested time overlaps with existing slots based on service duration (`duration_min`). Overlapping slots throw a `BadRequestException`.
*   **Conclude & Auto-Bill**: Setting an appointment status to **`DONE`** automatically detects the service price (if set and positive) and generates an **`INCOME`** entry in `tb_cashflow` pointing to the appointment.

### 2. Resource Ownership Validation
*   [VerifyUserPermissions.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/security/VerifyUserPermissions.java) checks if actions are legal:
    1.  `ROLE_ADMIN` bypasses security checks (full access).
    2.  Non-admins must match the HTTP method and URI path against authorities.
    3.  Resource-based endpoints (e.g. `/v1/users/{id}`) match the query `id` against the JWT logged user ID to prevent users from viewing/editing others' private information.

---

## ⚡ Quick Troubleshooting Guide

### Endpoints returning 403 Forbidden on the backend
*   **Check**:
    1.  Check the `@PreAuthorize` decorator configuration on the Spring Controller.
    2.  Check `tb_permission` and `tb_role_permissions`. Ensure that the specific HTTP method and path pattern exist and are assigned to the target role.
    3.  If the endpoint checks an ID (e.g. `#id`), verify that the logged-in user is either the resource owner or an `ADMIN`.

### CORS errors when fetching API from frontend
*   **Check**:
    *   Check [CorsConfig.java](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-back/src/main/java/com/cristiane/salon/config/CorsConfig.java). Ensure the frontend URL origin (default `http://localhost:5173`) is allowed and verify that the `dev` profile is active.

### Infinite refresh loops or abrupt logouts on the frontend
*   **Check**:
    *   Look at [api.ts](file:///home/elksandro/projects/faculdade/p7/dsc/sass-salon/salon-front/src/services/api.ts). This happens if `/v1/auth/refresh` responds with a 401/403 status, or if JWT tokens are not stored correctly under `@Salon:token` and `@Salon:refreshToken` inside `localStorage`.

### Server failed to boot due to Flyway validation/checksum failure
*   **Check**:
    *   This occurs if an active migration file has been edited. Revert files under `resources/db/migration/` back to their committed state and write a new numbered migration to apply database changes.
