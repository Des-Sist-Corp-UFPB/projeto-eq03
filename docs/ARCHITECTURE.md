# Architecture

> All UI text output must be in **pt-BR**.

## Monorepo Layout

```
salao-cristiane/
├── salon-back/
├── salon-front/
├── docs/
├── docker-compose.yml
└── README.md
```

## Backend (`salon-back`)

```
src/main/java/com/cristiane/salon/
├── annotation/
│   └── Auditable.java
├── aspect/
│   └── AuditAspect.java
├── config/
│   ├── CorsConfig.java
│   ├── SecurityConfig.java
│   ├── MethodSecurityConfig.java
│   ├── OpenApiConfig.java
│   └── AuditLogTableInitializer.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomPermissionEvaluator.java
│   ├── EntityPermissionEvaluator.java
│   ├── AuditRequestFilter.java
│   ├── SecurityUserDetailsService.java
│   └── VerifyUserPermissions.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── SalonServiceController.java
│   ├── ProductController.java
│   ├── EmployeeController.java
│   ├── AppointmentController.java
│   ├── CashFlowController.java
│   ├── FeatureFlagController.java
│   ├── AuditController.java
│   └── ReportController.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── UnauthorizedException.java
└── models/
    ├── user/        { entity, dto, repository, service }
    ├── service/     { entity, dto, repository, service }
    ├── product/     { entity, dto, repository, service }
    ├── employee/    { entity, dto, repository, service }
    ├── appointment/ { entity, dto, enums, repository, service }
    ├── cashflow/    { entity, dto, repository, service }
    ├── report/      { dto, service }
    ├── audit/       { entity, repository, service }
    ├── featureflag/ { entity, repository, service }
    └── email/       { service }

resources/
├── db/migration/
│   ├── V1__create_security_tables.sql
│   ├── V2__insert_roles_permissions.sql
│   ├── ...
│   └── V17__cleanup_audit_logs.sql
├── templates/
│   └── mail/        # Thymeleaf e-mail templates
├── application.yaml
├── application-dev.yaml
├── application-test.yaml
└── application-prod.yaml
```

## Frontend (`salon-front`)

```
src/
├── components/
│   ├── table/         # Reusable paginated/sortable table
│   ├── modal/         # ModalForm, ConfirmDialog
│   ├── form/          # Controlled form fields
│   ├── charts/        # Recharts wrappers
│   ├── layout/        # Shared layout pieces
│   ├── feedback/      # Toast, alerts, spinners
│   ├── permissions/   # PermissionGate component
│   └── loading/       # Skeleton loaders
├── context/           # AuthContext
├── hooks/             # usePermission, useAuth, custom hooks
├── layouts/
│   ├── DefaultLayout.tsx   # Public pages
│   ├── AdminLayout.tsx     # Admin sidebar + header
│   └── CustomerLayout.tsx  # Customer area
├── pages/
│   ├── admin/         # Users, Employees, Services, Products, Appointments, CashFlow, Reports
│   ├── appointments/  # PublicAppointment, MyAppointments
│   ├── auth/          # Login, Register
│   ├── home/          # PublicHome
│   ├── profile/       # Profile
│   ├── services/      # PublicServices
│   └── sysadmin/      # FeatureFlags, AuditLog (admin console)
├── services/
│   └── api.ts         # Axios instance with interceptors & auto-refresh
├── styles/
├── types/
├── utils/
├── Router.tsx
├── App.tsx
└── main.tsx
```

## Architectural Patterns

**Backend:** REST API · Layered architecture (Controller → Service → Repository) · DTO pattern (records) · Flyway migrations · Spring Security with JWT + Authorities

**Frontend:** SPA · Component composition · Custom hooks · Context API · Domain-separated pages · Reusable layouts

## Naming Conventions

| Layer       | Pattern                          | Example                  |
|-------------|----------------------------------|--------------------------|
| DTOs        | `{Domain}Request/Response`       | `UserRequest`            |
| Entities    | PascalCase, no suffix            | `User`, `Appointment`    |
| Services    | `{Domain}Service`                | `AppointmentService`     |
| Controllers | `{Domain}Controller`             | `AuthController`         |
| Repositories| `{Domain}Repository`             | `ProductRepository`      |
| Tables      | `tb_{domain}`                    | `tb_appointment`         |

## Scalability Notes

- Designed for future multi-tenant support (one salon per tenant)
- Service layer decoupled for eventual microservices extraction
- Ready for distributed cache (Redis)
- Horizontal scaling via stateless JWT
- CDN-ready for image/asset serving