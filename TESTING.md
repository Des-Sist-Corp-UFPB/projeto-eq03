## Guia de Teste - Sistema de Chamados (Etapa 1 + JWT)

### Pré-requisitos

- Java 21+
- Docker e Docker Compose
- Maven 3.9+
- cURL ou Postman (para testar API JWT)

### 🚀 Subindo o Ambiente

#### Opção 1: Ambiente Completo (Docker)

```bash
docker compose -f docker/docker-compose.dev.yml up
```

Isso irá:
- Iniciar PostgreSQL na porta 5432
- Iniciar a aplicação Spring Boot na porta 8080
- Iniciar Adminer (gerenciador de BD) na porta 8888

#### Opção 2: Banco no Docker + App Local

```bash
# Terminal 1: Inicie apenas o PostgreSQL
docker compose -f docker/docker-compose.dev.yml up postgres adminer

# Terminal 2: Rode a aplicação
mvn spring-boot:run
```

### 📌 Acessar a Aplicação

- **App**: http://localhost:8080
- **Adminer** (BD Web): http://localhost:8888
  - Sistema: PostgreSQL
  - Servidor: postgres
  - Usuário: admin
  - Senha: admin123
  - Banco: chamados

---

## 🔐 Autenticação JWT

### Credenciais Iniciais (DataInitializer)

| Matrícula | Senha      | Nome                        |
|-----------|------------|---------------------------|
| admin     | admin123   | Administrador do Sistema   |
| mat001    | senha123   | João Silva                 |

### Obter Token JWT

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "matricula": "admin",
    "senha": "admin123"
  }'
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "matricula": "admin",
  "nomeCompleto": "Administrador do Sistema"
}
```

### Usar Token para Acessar Recursos

```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/usuarios
```

---

## 🧪 Testar via Interface Web

### 1. Login

1. Acesse http://localhost:8080/login
2. Preencha:
   - **Matrícula:** admin
   - **Senha:** admin123
3. Clique em "Entrar"

### 2. Operações CRUD

#### Listar Usuários
- **URL**: http://localhost:8080/usuarios
- **Requer**: Autenticação (JWT ou session)

#### Criar Novo Usuário
- Clique em "Novo Usuário"
- Preencha o formulário
- O formulário é injetado via HTMX (sem reload de página)

#### Editar Usuário
- Clique no ✏️ (lápis)
- Modal abre com dados do usuário
- Modifique e clique "Atualizar"

#### Deletar Usuário
- Clique no 🗑️ (lixeira)
- Confirmação será solicitada
- Tabela atualiza automaticamente

---

## 🔌 Testar via API REST

### POST `/api/auth/login` - Obter Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "matricula": "mat001",
    "senha": "senha123"
  }'
```

### GET `/usuarios` - Listar com JWT

```bash
TOKEN="<seu-token-aqui>"

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/usuarios
```

### POST `/usuarios` - Criar Usuário

```bash
TOKEN="<seu-token-aqui>"

curl -X POST http://localhost:8080/usuarios \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "matricula=mat002&nomeCompleto=Maria Santos&senha=senha123&email=maria@example.com&ativo=true"
```

### PUT `/usuarios/{id}` - Atualizar

```bash
TOKEN="<seu-token-aqui>"

curl -X PUT http://localhost:8080/usuarios/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "matricula=mat002&nomeCompleto=Maria Santos ATUALIZADA&senha=novaSenha&email=novo@example.com&ativo=true"
```

### DELETE `/usuarios/{id}` - Deletar

```bash
TOKEN="<seu-token-aqui>"

curl -X DELETE http://localhost:8080/usuarios/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🗄️ Verificar Banco de Dados

### Via Adminer (Web UI)
- Acesse http://localhost:8888
- Navegue para tabela `usuario`
- Verifique que senhas estão com hash BCrypt (começam com `$2a$`)

### Via psql (CLI)

```bash
docker compose -f docker/docker-compose.dev.yml exec postgres psql -U admin -d chamados

# Dentro do psql:
SELECT id, matricula, nome_completo, email, ativo FROM usuario;

# Ver schema:
\d usuario
```

---

## 🧪 Rodar Testes Unitários

```bash
mvn test
```

Testes incluídos:
- `UsuarioServiceTest` - Lógica de negócio
- `UsuarioControllerTest` - Endpoints HTTP
- `ChamadosApplicationTests` - Context load

---

## 📊 Estrutura de Migrations

- `V1__criar_tabela_produto.sql` - Migration original (não usada)
- `V2__criar_tabela_usuario.sql` - **Nova migration** para tabela `usuario`

Flyway aplica automaticamente ao iniciar.

---

## ⚡ Build & Deploy

### Compilar

```bash
mvn clean compile
```

### Build com Testes

```bash
mvn clean verify
```

### Build Docker (Produção)

```bash
docker build -f docker/Dockerfile -t chamados:latest .
docker compose -f docker/docker-compose.prod.yml up -d
```

---

## 🔒 Segurança — Pontos Importantes

✅ Senhas criptografadas com **BCrypt** (não texto plano)
✅ Tokens JWT assinados com **HMAC256**
✅ Chave secreta em **variável de ambiente** (`.env`)
✅ Tokens com **expiração** (60 min padrão)
✅ **SecurityFilter** valida token em cada requisição

---

## 📚 Documentação Completa

Veja [JWT-GUIDE.md](JWT-GUIDE.md) para detalhes técnicos sobre:
- Arquitetura JWT
- Fluxo de autenticação
- Componentes de segurança
- Boas práticas em produção

---

## ✅ Checklist de Testes

- [ ] Login funciona (admin / admin123)
- [ ] Token JWT é retornado
- [ ] Acessar `/usuarios` com token funciona
- [ ] Acessar sem token redireciona para `/login`
- [ ] CRUD de usuários via web funciona
- [ ] CRUD via API REST com JWT funciona
- [ ] Senhas estão hashadas no banco
- [ ] Usuários iniciais foram criados automaticamente

---

**Status**: Etapa 1 ✅ Completa + JWT ✅ Implementado
**Data**: 03/05/2026
**Próxima Etapa**: Roles, Permissões, Rate Limiting
