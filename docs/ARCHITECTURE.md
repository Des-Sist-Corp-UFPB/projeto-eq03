# Arquitetura do Sistema

---

## Estrutura Geral do Monorepo

```text
cristiane-moura/
├── backend/
│
├── frontend/
│
├── docs/
│
├── docker-compose.yml
│
└── README.md
```

---

# Backend

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/com/api/
│   │   │
│   │   │       ├── config/
│   │   │       │   ├── CorsConfig.java
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── OpenApiConfig.java
│   │   │       │   └── BeanConfig.java
│   │   │       │
│   │   │       ├── security/
│   │   │       │   ├── JwtService.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── EntityPermissionEvaluator.java
│   │   │       │   ├── VerifyUserPermissions.java
│   │   │       │   ├── CustomPermissionEvaluator.java
│   │   │       │   └── SecurityUserDetailsService.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── UserController.java
│   │   │       │   ├── ServiceController.java
│   │   │       │   ├── ProductController.java
│   │   │       │   ├── EmployeeController.java
│   │   │       │   ├── AppointmentController.java
│   │   │       │   ├── CashFlowController.java
│   │   │       │   └── ReportController.java
│   │   │       │
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── ResourceNotFoundException.java
│   │   │       │   ├── BadRequestException.java
│   │   │       │   └── UnauthorizedException.java
│   │   │       │
│   │   │       └── models/
│   │   │
│   │   │           ├── user/
│   │   │           │   ├── entity/
│   │   │           │   ├── dto/
│   │   │           │   ├── enums/
│   │   │           │   ├── repository/
│   │   │           │   └── service/
│   │   │
│   │   │           ├── service/
│   │   │           │   ├── entity/
│   │   │           │   ├── dto/
│   │   │           │   ├── enums/
│   │   │           │   ├── repository/
│   │   │           │   └── service/
│   │   │
│   │   │           ├── product/
│   │   │           ├── employee/
│   │   │           ├── appointment/
│   │   │           ├── cashflow/
│   │   │           └── report/
│   │
│   │   └── resources/
│   │       ├── db/
│   │       │   └── migration/
│   │       │       ├── V1__create_security_tables.sql
│   │       │       ├── V2__insert_roles_permissions.sql
│   │       │       └── V3__create_business_tables.sql
│   │       │
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   │
│   └── test/
│
└── pom.xml
```

---

# Frontend

```text
frontend/
├── src/
│
├── components/
│   ├── table/
│   ├── modal/
│   ├── form/
│   ├── charts/
│   ├── layout/
│   ├── feedback/
│   └── permissions/
│
├── layouts/
│   ├── DefaultLayout.tsx
│   ├── AdminLayout.tsx
│   └── CustomerLayout.tsx
│
├── pages/
│   ├── admin/
│   ├── public/
│   ├── customer/
│   └── auth/
│
├── hooks/
│
├── context/
│
├── services/
│
├── utils/
│
├── types/
│
├── Router.tsx
├── App.tsx
└── main.tsx
```

---

## Padrões Arquiteturais

### Backend

- REST API
- Camadas separadas
- DTO Pattern
- Service Layer
- Repository Pattern
- Validation Layer
- Security Layer

---

### Frontend

- SPA
- Componentização
- Hooks customizados
- Context API
- Separação por domínio
- Layouts reutilizáveis

---

## Padrões de Nomeação

### DTOs

Sem utilizar "DTO" no nome.

Correto:

```text
UserRequest
UserResponse
```

Errado:

```text
UserDTO
```

---

### Services

```text
UserService
AppointmentService
```

---

### Controllers

```text
UserController
AuthController
```

---

### Repositories

```text
UserRepository
ProductRepository
```

---

## Estratégia de Segurança

Toda rota será protegida utilizando:

- JWT
- Roles
- Authorities
- Endpoint permissions
- HTTP method permissions

---

## Estratégia de Escalabilidade

O projeto foi desenhado para:

- Multiempresa futuramente
- Microsserviços no futuro
- Cache distribuído
- Deploy horizontal
- CDN para imagens

---