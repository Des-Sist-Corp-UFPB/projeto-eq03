# API Reference

Base URL: `/v1` — All responses in JSON. Protected routes require `Authorization: Bearer <token>`.

## Auth

| Method | Endpoint               | Auth | Description          |
|--------|------------------------|------|----------------------|
| POST   | `/auth/login`          | ✗    | Login, returns JWT   |
| POST   | `/auth/refresh`        | ✗    | Refresh access token |
| POST   | `/auth/register`       | ✗    | Customer self-register |
| POST   | `/auth/forgot-password`| ✗    | Requests a password reset e-mail (always 200, avoids account enumeration) |
| POST   | `/auth/reset-password` | ✗    | Resets password using the token received by e-mail |
| GET    | `/auth/me`             | ✓    | Returns the authenticated user's profile + permissions (any role) |

**Login request/response:**
```json
// POST /auth/login
{ "email": "admin@email.com", "password": "123456" }
// → { "accessToken": "...", "refreshToken": "..." }
```

## Users

| Method | Endpoint                  | Auth | Permission           |
|--------|----------------------------|------|----------------------|
| GET    | `/users`                  | ✓    | ADMIN, GERENTE       |
| POST   | `/users`                  | ✓    | ADMIN                |
| GET    | `/users/details/id/{id}`  | ✓    | Owner or ADMIN       |
| PATCH  | `/users/{id}`             | ✓    | Owner or ADMIN       |
| DELETE | `/users/{id}`             | ✓    | ADMIN                |
| PATCH  | `/users/{id}/restore`     | ✓    | ADMIN — reactivates a soft-deleted user |
| PATCH  | `/users/me/cpf`           | ✓    | Any authenticated — updates own CPF (JIT capture at PIX checkout) |
| GET    | `/users/me/cpf-info`      | ✓    | Any authenticated — checks whether own CPF is on file |

## Clients

| Method | Endpoint          | Auth | Permission     |
|--------|-------------------|------|----------------|
| GET    | `/clients`        | ✓    | ADMIN, GERENTE — paginated list with filters |
| GET    | `/clients/{id}`   | ✓    | ADMIN, GERENTE — client detail + booking history |

## Staff

Full team registration (FUNCIONARIA / GERENTE_DE_ATENDIMENTO) — separate from `/users`, holds sensitive personal data (CPF, PIX key) encrypted at rest. See [SECURITY.md](./SECURITY.md) for the encryption details.

| Method | Endpoint                    | Auth | Permission     |
|--------|-----------------------------|------|----------------|
| POST   | `/staff`                    | ✓    | ADMIN, SYSADMIN |
| GET    | `/staff`                    | ✓    | ADMIN, GERENTE — paginated list with filters |
| GET    | `/staff/{id}`               | ✓    | ADMIN, GERENTE |
| POST   | `/staff/{id}/pix-qrcode`    | ✓    | ADMIN, SYSADMIN — generates a PIX QR code to pay this staff member without exposing the key |

## Services (beauty services)

| Method | Endpoint          | Auth | Permission     |
|--------|-------------------|------|----------------|
| GET    | `/services`       | ✗    | Public         |
| POST   | `/services`       | ✓    | ADMIN          |
| PUT    | `/services/{id}`  | ✓    | ADMIN          |
| DELETE | `/services/{id}`  | ✓    | ADMIN          |
| PATCH  | `/services/{id}/reactivate` | ✓ | ADMIN — reactivates a soft-deleted service |

## Products

| Method | Endpoint          | Auth | Permission     |
|--------|-------------------|------|----------------|
| GET    | `/products`       | ✗    | Public         |
| GET    | `/products/{id}`  | ✗    | Public         |
| POST   | `/products`       | ✓    | ADMIN          |
| PUT    | `/products/{id}`  | ✓    | ADMIN          |
| DELETE | `/products/{id}`  | ✓    | ADMIN          |
| PATCH  | `/products/{id}/reactivate` | ✓ | ADMIN — reactivates a soft-deleted product |

## Employees

| Method | Endpoint              | Auth | Permission |
|--------|-----------------------|------|------------|
| GET    | `/employees`          | ✓    | ADMIN, GERENTE |
| GET    | `/employees/booking`  | ✗    | Public — lightweight list for the booking wizard |
| GET    | `/employees/{id}`     | ✓    | ADMIN, GERENTE |
| POST   | `/employees`          | ✓    | ADMIN, GERENTE |
| PUT    | `/employees/{id}`     | ✓    | ADMIN, GERENTE |
| DELETE | `/employees/{id}`     | ✓    | ADMIN, GERENTE |

## Appointments

| Method | Endpoint                          | Auth | Permission       |
|--------|------------------------------------|------|------------------|
| GET    | `/appointments/slots`             | ✗    | Public           |
| POST   | `/appointments`                   | ✓    | CLIENTE, staff — client requests, staff can create with a fixed time |
| PATCH  | `/appointments/{id}/confirm`      | ✓    | ADMIN, GERENTE — confirms a client request, sets date/time |
| PATCH  | `/appointments/{id}/decline`      | ✓    | ADMIN, GERENTE — declines a client request |
| GET    | `/appointments/my`                | ✓    | Any authenticated — own appointments |
| GET    | `/appointments`                   | ✓    | ADMIN, GERENTE   |
| GET    | `/appointments/{id}`              | ✓    | Any authenticated |
| PATCH  | `/appointments/{id}/cancel`       | ✓    | Any authenticated (ownership enforced in service) |
| PATCH  | `/appointments/{id}/status`       | ✓    | ADMIN, GERENTE, FUNCIONARIA |
| PATCH  | `/appointments/{id}/payment-status` | ✓  | ADMIN, GERENTE, FUNCIONARIA |
| POST   | `/appointments/{id}/pix`          | ✓    | Any authenticated (ownership enforced in service) — generates a PIX copy-and-paste code via Mercado Pago |

## Cash Flow

| Method | Endpoint              | Auth | Permission |
|--------|-----------------------|------|------------|
| GET    | `/cashflow`           | ✓    | ADMIN      |
| POST   | `/cashflow`           | ✓    | ADMIN      |
| DELETE | `/cashflow/{id}`      | ✓    | ADMIN      |

## Reports

| Method | Endpoint                                   | Auth | Permission |
|--------|---------------------------------------------|------|------------|
| GET    | `/reports/financial`                       | ✓    | ADMIN      |
| GET    | `/reports/appointments`                    | ✓    | ADMIN, GERENTE |
| GET    | `/reports/payroll`                         | ✓    | ADMIN — payroll and commission calculation per staff member |
| GET    | `/reports/financial/employees/{employeeId}`| ✓    | ADMIN, GERENTE — financial history for a specific professional |

## Roles (RBAC)

Dynamic management of role → permission mappings, on top of the seeded `tb_permission` rows (see [SECURITY.md](./SECURITY.md)).

| Method | Endpoint                                   | Auth | Permission |
|--------|---------------------------------------------|------|------------|
| GET    | `/roles`                                   | ✓    | ADMIN, SYSADMIN — all roles with their granted permissions |
| GET    | `/roles/permissions`                       | ✓    | ADMIN, SYSADMIN — all permissions available in the system |
| POST   | `/roles/{roleId}/permissions/{permissionId}` | ✓  | ADMIN, SYSADMIN — grants a permission to a role |
| DELETE | `/roles/{roleId}/permissions/{permissionId}` | ✓  | ADMIN, SYSADMIN — revokes a permission from a role |

## Salon Profile

| Method | Endpoint                    | Auth | Permission     |
|--------|------------------------------|------|----------------|
| GET    | `/salon/profile`            | ✗    | Public — name, description, address, socials, business hours |
| PUT    | `/admin/salon/profile`      | ✓    | ADMIN, SYSADMIN — updates salon profile and business hours |

## Push Notifications

Web Push subscription management; any authenticated user manages their own subscription (not an admin action).

| Method | Endpoint              | Auth | Permission |
|--------|-----------------------|------|------------|
| POST   | `/push/subscribe`     | ✓    | Any authenticated — registers/reaffirms this browser's push subscription |
| DELETE | `/push/unsubscribe`   | ✓    | Any authenticated — removes this browser's push subscription |

## Email Outbox

Retry queue and recent send history for outbound e-mail (see `tb_email_outbox`).

| Method | Endpoint                     | Auth | Permission |
|--------|-------------------------------|------|------------|
| GET    | `/email-outbox`              | ✓    | ADMIN, GERENTE — paginated, filterable by status |
| POST   | `/email-outbox/{id}/resend`  | ✓    | ADMIN, SYSADMIN — forces an immediate resend, bypassing backoff |

## Webhooks

| Method | Endpoint                     | Auth | Description |
|--------|-------------------------------|------|-------------|
| POST   | `/webhooks/mercadopago`      | ✗ (signature-verified) | Receives Mercado Pago payment notifications; validates `x-signature`/`x-request-id` before processing |

## AI Module

Recommendations are consumed by ADMIN/GERENTE; provider configuration and MCP tokens are SYSADMIN-only. See [opentelemetry.md](./opentelemetry.md) for tracing of these calls.

**Recommendations** (`/v1/admin/recommendations`)

| Method | Endpoint                              | Auth | Permission |
|--------|-----------------------------------------|------|------------|
| GET    | `/admin/recommendations/status`        | ✓    | ADMIN, GERENTE — whether recommendation generation is currently available |
| GET    | `/admin/recommendations/{type}`        | ✓    | ADMIN, GERENTE — latest cached recommendation for the type |
| POST   | `/admin/recommendations/{type}/generate` | ✓  | ADMIN, GERENTE — generates a new recommendation via the AI provider |

**AI Config** (`/v1/sysadmin/ai-config`)

| Method | Endpoint                     | Auth | Permission |
|--------|-------------------------------|------|------------|
| GET    | `/sysadmin/ai-config`        | ✓    | SYSADMIN |
| PUT    | `/sysadmin/ai-config`        | ✓    | SYSADMIN |
| POST   | `/sysadmin/ai-config/test`   | ✓    | SYSADMIN — tests connectivity with the values currently in the form |

**MCP Tokens** (`/v1/sysadmin/ai-config/tokens`)

| Method | Endpoint                              | Auth | Permission |
|--------|-----------------------------------------|------|------------|
| GET    | `/sysadmin/ai-config/tokens`           | ✓    | SYSADMIN |
| POST   | `/sysadmin/ai-config/tokens`           | ✓    | SYSADMIN — generates a token; plaintext value returned only in this response |
| DELETE | `/sysadmin/ai-config/tokens/{id}`      | ✓    | SYSADMIN — revokes a token |

---

## Database Schema

### `tb_user`
| Column     | Type    | Notes              |
|------------|---------|--------------------|
| id         | bigint  | PK                 |
| name       | varchar |                    |
| email      | varchar | unique             |
| password   | varchar | bcrypt             |
| phone      | varchar |                    |
| role_id    | bigint  | FK → tb_role       |
| created_at | timestamp |                  |

### `tb_role`
| Column | Type    |
|--------|---------|
| id     | bigint  |
| name   | varchar |

### `tb_permission`
| Column      | Type    | Notes                     |
|-------------|---------|---------------------------|
| id          | bigint  |                           |
| name        | varchar | Human-readable label      |
| endpoint    | varchar | e.g. `/v1/users/*`        |
| http_method | varchar | GET, POST, PUT, DELETE, * |
| classe      | varchar | Domain grouping           |

### `tb_salon_service`
| Column       | Type      | Notes                     |
|--------------|-----------|---------------------------|
| id           | bigint  | PK                        |
| name         | varchar   |                           |
| description  | text      |                           |
| price        | numeric   | Nullable (defined on checkout)|
| duration_min | integer   |                           |
| duration_text| varchar   | e.g. "45 min"             |
| active       | boolean   |                           |

### `tb_product`
| Column | Type    | Notes                     |
|--------|---------|---------------------------|
| id     | bigint  | PK                        |
| name   | varchar |                           |
| stock  | integer |                           |
| price  | numeric |                           |
| active | boolean | Default TRUE (soft-delete)|

### `tb_employee`
| Column           | Type    | Notes                     |
|------------------|---------|---------------------------|
| id               | bigint  | PK                        |
| user_id          | bigint  | FK → tb_user              |
| bio              | text    |                           |
| remuneration     | numeric | Monthly fixed salary      |
| commission_value | numeric | Service commission rate   |

### `tb_appointment`
| Column         | Type      | Notes                               |
|----------------|-----------|-------------------------------------|
| id             | bigint    | PK                                  |
| client_id      | bigint    | FK → tb_user                        |
| employee_id    | bigint    | FK → tb_employee                    |
| service_id     | bigint    | FK → tb_salon_service               |
| scheduled_at   | timestamp | Nullable (defined when confirmed)  |
| preferred_date | date      | Client preference                   |
| client_notes   | text      | Client notes                        |
| status         | varchar   | REQUESTED/CONFIRMED/DONE/CANCELLED/DECLINED|
| created_at     | timestamp |                                     |

### `tb_cashflow`
| Column         | Type      | Notes              |
|----------------|-----------|--------------------|
| id             | bigint    | PK                 |
| type           | varchar   | INCOME / EXPENSE   |
| amount         | numeric   |                    |
| description    | varchar   |                    |
| date           | date      |                    |
| appointment_id | bigint    | FK (nullable)      |

### `tb_audit_log`
| Column        | Type      | Notes                               |
|---------------|-----------|-------------------------------------|
| id            | bigint    | PK                                  |
| user_id       | bigint    | Nullable (actor ID)                 |
| user_email    | varchar   | Actor email / SYSTEM                |
| action        | varchar   | e.g. CREATE, APPOINTMENT_COMPLETED  |
| entity_type   | varchar   | e.g. User, Appointment              |
| entity_id     | bigint    | Target entity ID reference          |
| details       | text      | Masked parameters payload in JSON   |
| ip_address    | varchar   | Client IP address                   |
| user_agent    | text      | Client web browser header           |
| status        | varchar   | SUCCESS / FAILURE                   |
| error_message | text      |                                     |
| created_at    | timestamp |                                     |

### `tb_feature_flag`
| Column      | Type    | Notes                     |
|-------------|---------|---------------------------|
| name        | varchar | PK (e.g. EMAIL_NOTIFICATIONS) |
| enabled     | boolean | Toggle state              |
| description | varchar |                           |

### `tb_staff_profile`
| Column                  | Type      | Notes                                         |
|-------------------------|-----------|------------------------------------------------|
| id                      | bigint    | PK                                             |
| user_id                 | bigint    | FK → tb_user, unique                           |
| full_name, social_name  | varchar   |                                                 |
| cpf_encrypted           | text      | AES-256-GCM ciphertext (see SECURITY.md)       |
| cpf_hash                | varchar   | HMAC of digits-only CPF, unique — lookup w/o decrypting |
| birth_date, gender      | date, varchar |                                             |
| phone, emergency_contact_name, emergency_contact_phone | varchar | |
| zip_code, street, street_number, complement, district, city, state_uf | varchar | Address, kept in clear |
| pix_key_type            | varchar   | Nullable                                       |
| pix_key_encrypted       | text      | AES-256-GCM ciphertext, nullable               |
| pix_key_masked          | varchar   | Pre-computed mask for UI display                |
| hired_at, notes, created_by_user_id | date, text, bigint | |
| created_at, updated_at  | timestamp |                                                 |

### `tb_salon_profile`
| Column       | Type      | Notes                          |
|--------------|-----------|---------------------------------|
| id           | bigint    | PK — singleton (lowest id used) |
| name         | varchar   |                                  |
| description  | text      |                                  |
| address, phone, instagram, whatsapp | varchar | |
| logo_url     | varchar   |                                  |
| updated_at   | timestamp |                                  |

### `tb_business_hours`
| Column       | Type    | Notes                                     |
|--------------|---------|--------------------------------------------|
| id           | bigint  | PK                                          |
| day_of_week  | varchar | Unique, one of the 7 `DayOfWeek` values     |
| is_open      | boolean |                                              |
| open_time, close_time | time | Nullable when `is_open = false`        |

### `tb_email_outbox`
| Column                | Type      | Notes                                    |
|-----------------------|-----------|--------------------------------------------|
| id                    | bigint    | PK                                          |
| recipient_email, subject, html_content, reply_to | varchar/text | |
| status                | varchar   | PENDING / SENT / FAILED / DEAD_LETTER       |
| attempts              | integer   |                                              |
| next_retry_at         | timestamp | Nullable                                    |
| last_error            | varchar   | Nullable                                    |
| related_entity_type, related_entity_id | varchar, bigint | For admin-screen context |
| created_at, sent_at, updated_at | timestamp | |

### `tb_push_subscription`
| Column     | Type      | Notes                                  |
|------------|-----------|------------------------------------------|
| id         | bigint    | PK                                        |
| user_id    | bigint    | FK → tb_user                              |
| endpoint   | text      | Browser push service URL                  |
| p256dh, auth | text    | Web Push encryption keys                  |
| user_agent | varchar   | Nullable                                  |
| created_at | timestamp |                                            |

Unique on `(user_id, endpoint)`.

### `tb_password_reset_token`
| Column      | Type      | Notes                                  |
|-------------|-----------|-------------------------------------------|
| id          | bigint    | PK                                         |
| user_id     | bigint    | FK → tb_user                               |
| token_hash  | varchar   | Unique — only the hash is stored           |
| created_at, expires_at, used_at | timestamp | expires_at ~30 min, used_at marks single use |

### AI tables

`tb_ai_config` — singleton (id fixed at 1): `base_url`, `model`, `api_key_encrypted`, `temperature`, `max_tokens`, `enabled`, `daily_call_budget`, `updated_by`, `updated_at`.

`tb_ai_call_log` — one row per AI provider call: `caller_type`, `caller_id`, `call_type`, `tokens_used`, `latency_ms`, `success`, `error_message`, `created_at`.

`tb_ai_recommendation` — cache of the latest generated result per type: `type`, `payload` (JSON text), `generated_at`.

`tb_ai_mcp_token` — MCP access tokens: `name`, `token_hash` (unique, only hash stored), `created_by`, `created_at`, `expires_at`, `last_used_at`, `revoked`.

## Flyway Migrations

> ⚠️ **WARNING FOR DEVELOPERS:**
> **NEVER** edit a Flyway migration file once it has been run or committed. Doing so will break the checksum validation on startup. 
> To apply database changes, **ALWAYS** write a new sequential migration file (e.g., `V18__your_new_change.sql`).

| Version | File | Description |
|---|---|---|
| **V1** | `V1__create_security_tables.sql` | Creates role, permission, user, and join tables |
| **V2** | `V2__insert_roles_permissions.sql` | Seeds default role profiles and admin authorities |
| **V3** | `V3__create_business_tables.sql` | Creates service, product, employee, appointment, and cashflow tables |
| **V4** | `V4__rename_service_table.sql` | Renames `tb_service` to `tb_salon_service` to avoid keyword conflicts |
| **V5** | `V5__flexible_price_and_appointment_request.sql` | Makes service price and appointment scheduled date nullable, adds client notes/preferred date |
| **V6** | `V6__service_duration_estimate.sql` | Adds service duration in text representation |
| **V7** | `V7__create_audit_log_table.sql` | Creates system audit log table and tracking indices |
| **V8** | `V8__add_permissions_to_roles.sql` | Seed granular endpoint mapping and permissions for all roles |
| **V9** | `V9__add_sysadmin_role_and_feature_flags.sql` | Adds SYSADMIN profile, creates feature flags table, seeds system user and initial flags |
| **V10** | `V10__update_admin_password.sql` | Updates seeded administrator user password |
| **V11** | `V11__make_audit_log_fields_nullable.sql` | Relaxes constraints on audit logging to allow anonymous/system-triggered events |
| **V12** | `V12__add_active_column_to_product.sql` | Adds active column to products for soft-deleting |
| **V13** | `V13__add_remuneration_to_employee.sql` | Adds base remuneration column for professionals |
| **V14** | `V14__add_commission_value_to_employee.sql` | Adds commission value column for professionals |
| **V15** | `V15__add_enable_customer_portal_feature_flag.sql` | Seeds customer portal availability feature toggle |
| **V16** | `V16__set_sysadmin_role_for_sysadmin_user.sql` | Maps system admin role explicitly to sysadmin user |
| **V17** | `V17__cleanup_audit_logs.sql` | Removes deprecated/dev-related audit logs |
| **V18** | `V18__add_payment_fields_to_appointment.sql` | Adds payment_status, payment_id, pix_qr_code to appointments |
| **V19** | `V19__add_cpf_to_user.sql` | Adds optional CPF field, collected JIT at PIX payment time |
| **V20** | `V20__remove_cpf_unique_constraint.sql` | Removes CPF unique constraint, clears existing values |
| **V21** | `V21__add_client_permissions_and_update_endpoints.sql` | Seeds client-management permissions |
| **V22** | `V22__add_rbac_role_permissions.sql` | Seeds permissions for the dynamic RBAC endpoints (SYSADMIN only) |
| **V23** | `V23__update_default_passwords.sql` | Updates default seeded admin password |
| **V24** | `V24__add_funcionaria_appointment_status_permissions.sql` | Grants FUNCIONARIA the status/payment-status appointment permissions |
| **V25** | `V25__add_ai_config_and_call_log.sql` | Creates `tb_ai_config` (singleton) and `tb_ai_call_log` |
| **V26** | `V26__add_ai_recommendation.sql` | Creates `tb_ai_recommendation` cache table |
| **V27** | `V27__add_ai_recommendations_permissions.sql` | Grants GERENTE_DE_ATENDIMENTO access to the recommendations screen |
| **V28** | `V28__add_ai_mcp_token.sql` | Creates `tb_ai_mcp_token` for MCP access tokens |
| **V29** | `V29__add_ai_recommendations_feature_flag.sql` | Ships the AI module dark (feature flag defaults off) |
| **V30** | `V30__add_password_reset_token.sql` | Creates `tb_password_reset_token` for the forgot/reset password flow |
| **V31** | `V31__add_appointment_custom_service_fields.sql` | Adds per-appointment price/duration/notes overrides |
| **V32** | `V32__appointment_multiple_services.sql` | Migrates appointments to support multiple services per booking |
| **V33** | `V33__create_staff_profile.sql` | Creates `tb_staff_profile` with encrypted CPF/PIX fields |
| **V34** | `V34__add_staff_permissions.sql` | Seeds staff-registration permissions (create/PIX QR restricted to ADMIN/SYSADMIN) |
| **V35** | `V35__create_email_outbox.sql` | Creates `tb_email_outbox` retry queue |
| **V36** | `V36__add_email_outbox_permissions.sql` | Seeds email outbox permissions (resend restricted to ADMIN/SYSADMIN) |
| **V37** | `V37__add_appointment_reminded_at.sql` | Adds `reminded_at` for the D-1 appointment reminder job |
| **V38** | `V38__create_push_subscription.sql` | Creates `tb_push_subscription` for Web Push |
| **V39** | `V39__add_push_permissions.sql` | Seeds push subscribe/unsubscribe permissions for all authenticated users |
| **V40** | `V40__create_salon_profile.sql` | Creates `tb_salon_profile` singleton, seeds default row |
| **V41** | `V41__create_business_hours.sql` | Creates `tb_business_hours`, seeds all 7 days |
| **V42** | `V42__add_salon_profile_permissions.sql` | Documents salon profile endpoint (no explicit grants; ADMIN/SYSADMIN only via bypass) |
| **V43** | `V43__machine_timestamps_to_timestamptz.sql` | Converts machine-generated timestamps to `timestamptz` (fixes a timezone display bug) |
| **V44** | `V44__funcionaria_confirm_own_appointments.sql` | Lets FUNCIONARIA confirm/decline appointments where she's the assigned professional |
| **V45** | `V45__cleanup_duplicate_cashflow_appointments.sql` | One-time cleanup: removes pre-existing duplicate cash flow entries per appointment (keeps the oldest), required before V46 can add the unique constraint |
| **V46** | `V46__unique_cashflow_appointment.sql` | Adds a unique constraint preventing duplicate cash flow entries per appointment |