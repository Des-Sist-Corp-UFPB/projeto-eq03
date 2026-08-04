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
│   ├── BeanConfig.java
│   ├── CorsConfig.java
│   ├── HttpClientConfig.java
│   ├── SecurityConfig.java
│   ├── MethodSecurityConfig.java
│   ├── OpenApiConfig.java
│   ├── SalonClock.java
│   ├── TimeConfig.java
│   ├── SpaRedirectController.java
│   ├── SpaWebConfig.java
│   ├── AuditLogTableInitializer.java
│   └── logging/
│       └── MaskingMessageConverter.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomPermissionEvaluator.java
│   ├── AuditRequestFilter.java
│   ├── SecurityUserDetailsService.java
│   ├── VerifyUserPermissions.java
│   └── crypto/
│       ├── EncryptedStringConverter.java   # transparent AES-256-GCM JPA converter
│       ├── PiiEncryptionUtil.java
│       ├── PiiHashUtil.java
│       └── AiEncryptionUtil.java
├── mcp/                # MCP server (auth filter + tool exposure for the AI module)
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── UnauthorizedException.java
├── integrations/        # Outbound/inbound integrations with external services
│   ├── email/
│   │   ├── outbox/      { entity, dto, repository, service, controller } # retry queue
│   │   └── service/     # SMTP send + Thymeleaf templating
│   ├── payment/         { dto, service, controller }  # Mercado Pago PIX + webhook
│   └── push/             { entity, dto, repository, service, controller, config } # Web Push
└── models/
    ├── user/        { entity, dto, repository, service, controller }  # Auth, User, Client, Role
    ├── staff/        { entity, dto, enums, repository, service, controller }
    ├── service/     { entity, dto, repository, service, controller }
    ├── product/     { entity, dto, repository, service, controller }
    ├── employee/    { entity, dto, repository, service, controller }
    ├── appointment/ { entity, dto, enums, repository, service, controller }
    ├── cashflow/    { entity, dto, repository, service, controller }
    ├── report/      { dto, service, controller }
    ├── audit/       { entity, repository, service, controller }
    ├── featureflag/ { entity, repository, service, controller }
    ├── salonprofile/ { entity, dto, repository, service, controller }
    ├── ai/           { entity, dto, repository, service, controller }  # config, recommendations, MCP tokens
    └── ping/         { controller }
```

Controllers live inside each domain module's own `controller/` subpackage (e.g. `models/user/controller/AuthController.java`), not in a shared top-level `controller/` package.

```
resources/
├── db/migration/
│   ├── V1__create_security_tables.sql
│   ├── V2__insert_roles_permissions.sql
│   ├── ...
│   └── V46__unique_cashflow_appointment.sql
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
│   ├── CustomerLayout.tsx  # Customer area
│   └── SysadminLayout.tsx  # Sysadmin console sidebar + outlet
├── pages/
│   ├── admin/         # Users, Clients, Staff, Employees, Services, Products, Appointments,
│   │                  # CashFlow, Reports, Recommendations, EmailOutbox, SalonProfile, Audit
│   ├── appointments/  # PublicAppointment, MyAppointments
│   ├── auth/          # Login, Register, ForgotPassword, ResetPassword
│   ├── home/          # PublicHome
│   ├── profile/       # Profile
│   ├── services/      # PublicServices
│   ├── error/         # NotFound
│   └── sysadmin/      # FeatureFlags, AuditLog, Rbac, AiConfig (admin console)
├── services/
│   └── api.ts         # Axios instance with interceptors & auto-refresh
├── sw.ts               # Service worker (Web Push)
├── styles/
├── types/
├── utils/
├── Router.tsx
├── App.tsx
└── main.tsx
```

Several `pages/<domain>/` folders keep their own `services/` subfolder for domain-specific API calls (e.g. `pages/admin/staff/services/staff.ts`), in addition to the shared ones under top-level `services/`.

## Architectural Patterns

**Backend:** REST API · Layered architecture (Controller → Service → Repository) · DTO pattern (records) · Flyway migrations · Spring Security with JWT + Authorities · Resilience4j (timeout/circuit breaker/retry) around external integrations (Mercado Pago, AI provider) · OpenTelemetry auto-instrumentation + manual spans (see [opentelemetry.md](./opentelemetry.md) and [opentelemetry-logs.md](./opentelemetry-logs.md))

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