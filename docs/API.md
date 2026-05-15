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

### `tb_service`
| Column      | Type    |
|-------------|---------|
| id          | bigint  |
| name        | varchar |
| description | text    |
| price       | numeric |
| duration_min| integer |
| active      | boolean |

### `tb_product`
| Column | Type    |
|--------|---------|
| id     | bigint  |
| name   | varchar |
| stock  | integer |
| price  | numeric |

### `tb_employee`
| Column  | Type    | Notes         |
|---------|---------|---------------|
| id      | bigint  |               |
| user_id | bigint  | FK → tb_user  |
| bio     | text    |               |

### `tb_appointment`
| Column        | Type      | Notes                |
|---------------|-----------|----------------------|
| id            | bigint    |                      |
| client_id     | bigint    | FK → tb_user         |
| employee_id   | bigint    | FK → tb_employee     |
| service_id    | bigint    | FK → tb_service      |
| scheduled_at  | timestamp |                      |
| status        | varchar   | PENDING/CONFIRMED/DONE/CANCELLED |

### `tb_cashflow`
| Column      | Type      | Notes              |
|-------------|-----------|--------------------|
| id          | bigint    |                    |
| type        | varchar   | INCOME / EXPENSE   |
| amount      | numeric   |                    |
| description | varchar   |                    |
| date        | date      |                    |
| appointment_id | bigint | FK (nullable)      |

## Flyway Migrations

| Version | File                               | Creates                                          |
|---------|------------------------------------|--------------------------------------------------|
| V1      | `V1__create_security_tables.sql`   | `tb_role`, `tb_permission`, `tb_user`, join tables |
| V2      | `V2__insert_roles_permissions.sql` | Seed ADMIN, GERENTE, FUNCIONARIA, CLIENTE roles  |
| V3      | `V3__create_business_tables.sql`   | `tb_service`, `tb_product`, `tb_employee`, `tb_appointment`, `tb_cashflow` |