# API REST

---

## Base URL

```text
/v1
```

---

# AUTH

## Login

```http
POST /auth/login
```

Request:

```json
{
  "email": "admin@email.com",
  "password": "123456"
}
```

Response:

```json
{
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token"
}
```

---

# USERS

## Listar usuários

```http
GET /users
```

---

## Buscar usuário

```http
GET /users/details/id/{id}
```

---

## Atualizar usuário

```http
PATCH /users/{id}
```

---

## Remover usuário

```http
DELETE /users/{id}
```

---

# SERVICES

## Listar serviços

```http
GET /services
```

---

## Criar serviço

```http
POST /services
```

---

## Atualizar serviço

```http
PUT /services/{id}
```

---

## Remover serviço

```http
DELETE /services/{id}
```

---

# PRODUCTS

## Listar produtos

```http
GET /products
```

---

## Criar produto

```http
POST /products
```

---

## Atualizar produto

```http
PUT /products/{id}
```

---

## Remover produto

```http
DELETE /products/{id}
```

---

# APPOINTMENTS

## Criar agendamento

```http
POST /appointments
```

---

## Horários disponíveis

```http
GET /appointments/slots
```

---

# DATABASE.md

# Banco de Dados

---

# Tabelas Principais

## tb_user

| Campo | Tipo |
|---|---|
| id | bigint |
| name | varchar |
| email | varchar |
| password | varchar |
| role_id | bigint |

---

## tb_role

| Campo | Tipo |
|---|---|
| id | bigint |
| name | varchar |

---

## tb_permission

| Campo | Tipo |
|---|---|
| id | bigint |
| name | varchar |
| endpoint | varchar |
| http_method | varchar |

---

## tb_service

| Campo | Tipo |
|---|---|
| id | bigint |
| name | varchar |
| description | text |
| price | numeric |

---

## tb_product

| Campo | Tipo |
|---|---|
| id | bigint |
| name | varchar |
| stock | integer |

---

## tb_appointment

| Campo | Tipo |
|---|---|
| id | bigint |
| client_id | bigint |
| employee_id | bigint |

---

# Migrations Flyway

## V1__create_security_tables.sql

- Roles
- Permissions
- Users

---

## V2__insert_roles_permissions.sql

- Inserções iniciais
- ADMIN
- GERENTE

---

## V3__create_business_tables.sql

- Serviços
- Produtos
- Funcionárias
- Agendamentos
- Fluxo de caixa

---