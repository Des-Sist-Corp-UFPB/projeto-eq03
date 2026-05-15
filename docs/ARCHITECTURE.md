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
src/main/java/br/com/api/
├── config/
│   ├── CorsConfig.java
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── BeanConfig.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── EntityPermissionEvaluator.java
│   ├── VerifyUserPermissions.java
│   ├── CustomPermissionEvaluator.java
│   └── SecurityUserDetailsService.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ServiceController.java
│   ├── ProductController.java
│   ├── EmployeeController.java
│   ├── AppointmentController.java
│   ├── CashFlowController.java
│   └── ReportController.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── UnauthorizedException.java
└── models/
    ├── user/       { entity, dto, enums, repository, service }
    ├── service/    { entity, dto, enums, repository, service }
    ├── product/    { entity, dto, repository, service }
    ├── employee/   { entity, dto, repository, service }
    ├── appointment/{ entity, dto, enums, repository, service }
    ├── cashflow/   { entity, dto, repository, service }
    └── report/     { dto, service }

resources/
├── db/migration/
│   ├── V1__create_security_tables.sql
│   ├── V2__insert_roles_permissions.sql
│   └── V3__create_business_tables.sql
├── application.yml
├── application-dev.yml
└── application-prod.yml
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
│   ├── public/        # Home, Services, Appointment, Login, Register
│   ├── admin/         # Dashboard, Users, Employees, Services, Products, Appointments, CashFlow, Reports
│   ├── customer/      # MyAppointments, Profile
│   └── auth/          # Login, Register
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