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
| SYSADMIN               | Manages system configurations/feature flags |

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

> The seed above reflects only the original **V2** migration. Permissions have been substantially extended since — most notably in V21-V24, V27, V34, V36, V39, and V42, adding client management, RBAC endpoints, FUNCIONARIA appointment actions, AI recommendations, staff registration, e-mail outbox, push, and salon profile permissions. See [API.md](./API.md#flyway-migrations) for the full migration list; the current permission set is managed dynamically through the RBAC endpoints (`/v1/roles`) rather than enumerated here, since it changes independently of the codebase.

## Password Reset Flow

```
POST /v1/auth/forgot-password
Body:    { "email": "..." }
→ Always returns 200 (whether or not the e-mail has an account) to avoid account enumeration.
  If it exists, a single-use token is generated, hashed (tb_password_reset_token.token_hash),
  and the plaintext token is e-mailed to the user — it is never stored in the database.

POST /v1/auth/reset-password
Body:    { "token": "...", "newPassword": "..." }
→ Validates the token hash, checks it isn't expired (~30 min) or already used, then updates
  the password and marks the token used (used_at).
```

Introduced in migration **V30** (`tb_password_reset_token`).

## PII Encryption at Rest

Sensitive fields on `StaffProfile` (`salon-back/.../models/staff/entity/StaffProfile.java`) — CPF and PIX key — are encrypted transparently at the JPA layer, introduced in migration **V33**:

- `cpf` and `pixKey` are annotated `@Convert(converter = EncryptedStringConverter.class)`, backed by `cpf_encrypted`/`pix_key_encrypted` `TEXT` columns. In memory these fields hold plaintext; in the database they hold AES-256-GCM ciphertext. The master key lives only in the `APP_PII_ENCRYPTION_KEY` env var — a database dump alone exposes nothing.
- `cpfHash` (`security/crypto/PiiHashUtil.java`) is an HMAC-SHA256 of the digits-only CPF, stored unique/unindexed-searchable, so duplicate-CPF checks and lookups work without ever decrypting.
- `pixKeyMasked` is a pre-computed display mask (e.g. `joa•••••@mail.com`) so the UI never needs to decrypt the PIX key just to show a hint of it.
- Key security components: `security/crypto/EncryptedStringConverter.java` (the JPA `AttributeConverter`), `PiiEncryptionUtil.java` (encrypt/decrypt), `PiiHashUtil.java` (HMAC hashing), and `AiEncryptionUtil.java` (same scheme, used to encrypt the AI provider's `api_key_encrypted` in `tb_ai_config`).

## Public Routes (no token required)

Read directly from `SecurityConfig.securityFilterChain()`:

```
GET  /ping
     /v1/auth/**                 (all methods)
GET  /v1/services
GET  /v1/products
GET  /v1/employees/booking
GET  /v1/feature-flags
GET  /v1/salon/profile
POST /v1/webhooks/mercadopago    (signature-verified in the controller, not by Spring Security)

     /swagger-ui/**, /swagger-ui.html, /v3/api-docs/**, /swagger-resources/**, /webjars/**

GET  /, /index.html, /static/**, /assets/**, /*.png, /*.ico, /*.json, /*.txt   (SPA static assets)
```

All other `/v1/**` routes require authentication. `/sse` and `/mcp/message` (the MCP server endpoints consumed by the AI module) also require authentication, but via a separate `McpAuthenticationFilter` using its own bearer tokens (`tb_ai_mcp_token`) rather than the regular JWT flow.

**Everything else defaults to permitAll().** The chain ends with `.anyRequest().permitAll()` — this is intentional, not an oversight: it exists so the SPA's client-side routes (e.g. `/admin/users`) fall through to `index.html` instead of getting a 403 from Spring Security, with the frontend's own `ProtectedRoute`/role checks (see [FRONTEND.md](./FRONTEND.md)) providing the UX-level gate. It is not a substitute for API authorization — every actual `/v1/**` data endpoint is explicitly locked down above.

## Key Security Components

**`JwtAuthenticationFilter`** — extracts and validates JWT on every request, loads authorities into `SecurityContext`.

**`CustomPermissionEvaluator`** — checks if the authenticated user's authority list contains a matching `METHOD:endpoint` entry.

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