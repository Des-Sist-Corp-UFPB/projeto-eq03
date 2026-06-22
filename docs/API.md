# API Reference

Base URL: `/v1` — All responses in JSON. Protected routes require `Authorization: Bearer <token>`.

## Auth

| Method | Endpoint            | Auth | Description          |
|--------|---------------------|------|----------------------|
| POST   | `/auth/login`       | ✗    | Login, returns JWT   |
| POST   | `/auth/refresh`     | ✗    | Refresh access token |
| POST   | `/auth/register`    | ✗    | Customer self-register |

**Login request/response:**
```json
// POST /auth/login
{ "email": "admin@email.com", "password": "123456" }
// → { "accessToken": "...", "refreshToken": "..." }
```

## Users

| Method | Endpoint              | Auth | Permission           |
|--------|-----------------------|------|----------------------|
| GET    | `/users`              | ✓    | ADMIN, GERENTE       |
| GET    | `/users/details/id/{id}` | ✓ | Owner or ADMIN       |
| PATCH  | `/users/{id}`         | ✓    | Owner or ADMIN       |
| DELETE | `/users/{id}`         | ✓    | ADMIN                |

## Services (beauty services)

| Method | Endpoint          | Auth | Permission     |
|--------|-------------------|------|----------------|
| GET    | `/services`       | ✗    | Public         |
| POST   | `/services`       | ✓    | ADMIN          |
| PUT    | `/services/{id}`  | ✓    | ADMIN          |
| DELETE | `/services/{id}`  | ✓    | ADMIN          |

## Products

| Method | Endpoint          | Auth | Permission     |
|--------|-------------------|------|----------------|
| GET    | `/products`       | ✗    | Public         |
| POST   | `/products`       | ✓    | ADMIN          |
| PUT    | `/products/{id}`  | ✓    | ADMIN          |
| DELETE | `/products/{id}`  | ✓    | ADMIN          |

## Employees

| Method | Endpoint              | Auth | Permission |
|--------|-----------------------|------|------------|
| GET    | `/employees`          | ✓    | ADMIN      |
| GET    | `/employees/{id}`     | ✓    | ADMIN      |
| POST   | `/employees`          | ✓    | ADMIN      |
| PUT    | `/employees/{id}`     | ✓    | ADMIN      |
| DELETE | `/employees/{id}`     | ✓    | ADMIN      |

## Appointments

| Method | Endpoint                        | Auth | Permission       |
|--------|---------------------------------|------|------------------|
| GET    | `/appointments/slots`           | ✗    | Public           |
| POST   | `/appointments`                 | ✓    | CLIENTE          |
| GET    | `/appointments/my`              | ✓    | Owner            |
| GET    | `/appointments`                 | ✓    | ADMIN, GERENTE   |
| PATCH  | `/appointments/{id}/cancel`     | ✓    | Owner or ADMIN   |
| PATCH  | `/appointments/{id}/status`     | ✓    | ADMIN, FUNCIONARIA |

## Cash Flow

| Method | Endpoint              | Auth | Permission |
|--------|-----------------------|------|------------|
| GET    | `/cashflow`           | ✓    | ADMIN      |
| POST   | `/cashflow`           | ✓    | ADMIN      |
| DELETE | `/cashflow/{id}`      | ✓    | ADMIN      |

## Reports

| Method | Endpoint                      | Auth | Permission |
|--------|-------------------------------|------|------------|
| GET    | `/reports/financial`          | ✓    | ADMIN      |
| GET    | `/reports/appointments`       | ✓    | ADMIN, GERENTE |

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