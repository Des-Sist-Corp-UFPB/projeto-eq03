# Modelo de Segurança

---

## Visão Geral

O sistema utilizará segurança baseada em:

- JWT Authentication
- Roles
- Authorities
- Permissões granulares
- Controle baseado em endpoint
- Controle baseado em método HTTP
- Verificação de dono do recurso

---

# Fluxo de Autenticação

## Login

Endpoint:

```text
POST /v1/auth/login
```

Body:

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

## Refresh Token

Endpoint:

```text
POST /v1/auth/refresh
```

Body:

```json
{
  "refreshToken": "refresh-token"
}
```

Response:

```json
{
  "accessToken": "new-access-token"
}
```

---

# Estrutura do JWT

O JWT conterá:

```json
{
  "sub": "1",
  "role": "ADMIN",
  "authorities": [
    "GET:/v1/users",
    "DELETE:/v1/users/*"
  ]
}
```

---

# Roles do Sistema

| Role | Descrição |
|---|---|
| ADMIN | Acesso total |
| GERENTE_DE_ATENDIMENTO | Gerencia clientes e relatórios |
| FUNCIONARIA | Agenda e atendimentos |
| CLIENTE | Área do cliente |

---

# Permissões Granulares

Tabela:

```text
tb_permission
```

Campos:

| Campo | Descrição |
|---|---|
| name | Nome legível |
| endpoint | Endpoint |
| http_method | Método HTTP |
| classe | Domínio |

---

# Permissão Total do ADMIN

Migration obrigatória:

```sql
INSERT INTO tb_permission (
    name,
    endpoint,
    http_method,
    classe
)
VALUES (
    'Acesso Total',
    '/**',
    '*',
    'Administração'
);
```

---

## Vincular ao ADMIN

```sql
INSERT INTO tb_role_permissions (
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM tb_role r,
     tb_permission p
WHERE r.name = 'ADMIN'
  AND p.name = 'Acesso Total';
```

---

# Exemplos de Permissões

| Nome | Endpoint | Método |
|---|---|---|
| Listar Usuários | /v1/users | GET |
| Atualizar Usuário | /v1/users/* | PATCH |
| Remover Usuário | /v1/users/* | DELETE |
| Criar Serviço | /v1/services | POST |
| Atualizar Serviço | /v1/services/* | PUT |
| Remover Serviço | /v1/services/* | DELETE |

---

# EntityPermissionEvaluator

```java
@Component
public class EntityPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(
            Authentication auth,
            Serializable targetId,
            String targetType,
            Object permission
    ) {

        User user = (User) auth.getPrincipal();

        TargetDomain domain =
                TargetDomain.valueOf(targetType.toUpperCase());

        PermissionAction action =
                PermissionAction.valueOf(
                        ((String) permission).toUpperCase()
                );

        if (user.getRoleName().equals("ADMIN")) {
            return true;
        }

        return switch (domain) {
            case USER ->
                    handleUserPermission(
                            user,
                            (Long) targetId,
                            action
                    );

            default -> false;
        };
    }

    private boolean handleUserPermission(
            User logged,
            Long targetId,
            PermissionAction action
    ) {

        if (logged.getRoleName()
                .equals("GERENTE_DE_ATENDIMENTO")) {

            return true;
        }

        return switch (action) {
            case READ,
                 UPDATE,
                 DELETE -> logged.getId().equals(targetId);

            default -> false;
        };
    }
}
```

---

# VerifyUserPermissions

```java
@Component("verifyUserPermissions")
public class VerifyUserPermissions {

    private final CustomPermissionEvaluator permissionEvaluator;
    private final HttpServletRequest request;

    public boolean userOwnResourceOrHasPermission(Long userId) {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        User logged = (User) auth.getPrincipal();

        if (
            permissionEvaluator.hasPermission(
                auth,
                request.getRequestURI(),
                request.getMethod()
            )
        ) {
            return true;
        }

        if (
            request.getMethod().equals("DELETE")
            || request.getMethod().equals("POST")
        ) {
            return false;
        }

        return logged.getId().equals(userId);
    }
}
```

---

# SecurityFilterChain

Rotas públicas:

```text
/v1/auth/**
/v1/services (GET)
/v1/products (GET)
/swagger-ui/**
/v3/api-docs/**
```

Demais rotas:

```text
Autenticadas
```

---

# Tratamento de Exceções

| Exceção | Código |
|---|---|
| AuthenticationException | 401 |
| AccessDeniedException | 403 |
| ResourceNotFoundException | 404 |

---

# Resposta Padrão de Erro

```json
{
  "timestamp": "2026-05-15T10:00:00",
  "status": 403,
  "error": "Access Denied",
  "message": "Você não possui permissão",
  "path": "/v1/users/1"
}
```

---