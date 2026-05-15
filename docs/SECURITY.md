# Security Model

## Overview

Authentication via **JWT** (stateless). Authorization via **Roles** + **granular Authorities** checked per endpoint and HTTP method. Resource ownership verified for self-service actions.

## Roles

| Role                   | Description                                 |
|------------------------|---------------------------------------------|
| ADMIN                  | Full access                                 |
| GERENTE_DE_ATENDIMENTO | Manages clients, views reports              |
| FUNCIONARIA            | Views/manages own appointments              |
| CLIENTE                | Self-service booking and profile            |

## Auth Flow

```
POST /v1/auth/login
Body:    { "email": "...", "password": "..." }
Returns: { "accessToken": "<jwt>", "refreshToken": "<token>" }

POST /v1/auth/refresh
Body:    { "refreshToken": "<token>" }
Returns: { "accessToken": "<new-jwt>" }
```

**Access token expiry:** 15 minutes  
**Refresh token expiry:** 7 days

## JWT Payload

```json
{
  "sub": "1",
  "role": "ADMIN",
  "authorities": ["GET:/v1/users", "DELETE:/v1/users/*"]
}
```

## Granular Permissions (`tb_permission`)

Each row maps a human-readable name to an `endpoint` + `http_method` pair. The ADMIN role receives a wildcard permission `/** / *` seeded in V2 migration.

Example seed data:

| name             | endpoint         | http_method |
|------------------|------------------|-------------|
| Listar Usuários  | /v1/users        | GET         |
| Atualizar Usuário| /v1/users/*      | PATCH       |
| Remover Usuário  | /v1/users/*      | DELETE      |
| Criar Serviço    | /v1/services     | POST        |
| Atualizar Serviço| /v1/services/*   | PUT         |
| Remover Serviço  | /v1/services/*   | DELETE      |
| Acesso Total     | /**              | *           |

## Public Routes (no token required)

```
POST /v1/auth/**
GET  /v1/services
GET  /v1/products
GET  /v1/appointments/slots
     /swagger-ui/**
     /v3/api-docs/**
```

## Key Security Components

**`JwtAuthenticationFilter`** — extracts and validates JWT on every request, loads authorities into `SecurityContext`.

**`CustomPermissionEvaluator`** — checks if the authenticated user's authority list contains a matching `METHOD:endpoint` entry.

**`EntityPermissionEvaluator`** — implements `PermissionEvaluator` for fine-grained object-level access (e.g., a CLIENTE can only read/update their own profile).

**`VerifyUserPermissions`** — Spring bean (`@verifyUserPermissions`) used in `@PreAuthorize` expressions. Combines authority check with ownership check:

```java
@PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(#id)")
```

## Error Responses

| Exception               | HTTP Status |
|-------------------------|-------------|
| AuthenticationException | 401         |
| AccessDeniedException   | 403         |
| ResourceNotFoundException | 404       |

Standard error body:

```json
{
  "timestamp": "2026-05-15T10:00:00",
  "status": 403,
  "error": "Access Denied",
  "message": "Você não possui permissão para acessar este recurso.",
  "path": "/v1/users/1"
}
```

> Error `message` field is always in **pt-BR** for end-user display.