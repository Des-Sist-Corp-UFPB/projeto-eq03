## Guia Completo — Segurança JWT & Testes

### 🔐 Arquitetura de Segurança Implementada

#### Fluxo de Autenticação JWT

```
1. [Cliente] ─── POST /api/auth/login ────┐
                 (matricula + senha)       │
                                           ▼
2. [Servidor] ─ Valida credenciais (BCrypt)
                 ├─ Busca usuário no banco
                 ├─ Compara senha com hash
                 └─ Se válido → Gera JWT
                                           │
3. [Servidor] ◄─ Retorna LoginResponse ◄──┘
                 {
                   "token": "eyJhbGc...",
                   "matricula": "mat001",
                   "nomeCompleto": "João Silva"
                 }

4. [Cliente] ─── Armazena token
                 (localStorage, cookie, etc.)

5. [Cliente] ─── GET /usuarios ──────────┐
                 Header: Authorization:   │
                 Bearer <token>           │
                                          ▼
6. [Servidor] ─ SecurityFilter
                 ├─ Extrai token
                 ├─ Valida assinatura
                 ├─ Extrai claims (ID, matrícula)
                 └─ Carrega usuário no SecurityContext

7. [Servidor] ─ Acessa recurso protegido
                 └─ Retorna dados se autenticado
```

---

### 🛠️ Componentes Implementados

#### 1. **TokenProvider** (`security/TokenProvider.java`)
- ✅ Gera tokens JWT com HMAC256
- ✅ Valida e decodifica tokens
- ✅ Extrai claims (ID, matrícula, nome)
- ✅ Expiration configurável

**Propriedades:**
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}              # Variável de ambiente
    expiration-minutes: 60             # TTL do token
```

#### 2. **SecurityFilter** (`security/SecurityFilter.java`)
- ✅ Intercepta todas as requisições
- ✅ Extrai token do header `Authorization: Bearer <token>`
- ✅ Valida e configura autenticação no SecurityContext
- ✅ Continua requisição ou deixa handler resolver (401/403)

#### 3. **AuthController** (`controller/AuthController.java`)
- ✅ GET `/login` - Página de login (HTML)
- ✅ POST `/api/auth/login` - Autentica e retorna JWT (JSON)

#### 4. **UserDetailsServiceImpl** (`service/UserDetailsServiceImpl.java`)
- ✅ Busca usuário no banco por matrícula
- ✅ Valida se está ativo
- ✅ Retorna UserDetails para o AuthenticationManager

#### 5. **SecurityConfig** (`config/SecurityConfig.java`)
- ✅ Configura rotas públicas vs protegidas
- ✅ Integra SecurityFilter na chain
- ✅ AuthenticationManager com UserDetailsService + PasswordEncoder
- ✅ BCryptPasswordEncoder para hash de senhas

#### 6. **DataInitializer** (`config/DataInitializer.java`)
- ✅ Popula usuários iniciais ao subir a app
- ✅ Criptografa senhas automaticamente
- ✅ Usuários: `admin` / `admin123`, `mat001` / `senha123`

---

### 🚀 Como Testar

#### Pré-requisitos
```bash
# Subir ambiente
docker compose -f docker/docker-compose.dev.yml up postgres adminer

# Ou em outro terminal:
mvn spring-boot:run
```

Acesse: http://localhost:8080

---

### 📋 Teste 1: Login com cURL (obter token)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "matricula": "admin",
    "senha": "admin123"
  }'
```

**Resposta esperada (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "matricula": "admin",
  "nomeCompleto": "Administrador do Sistema"
}
```

**Erros possíveis:**
- `401 Unauthorized` - Credenciais inválidas (matrícula/senha incorretas)
- `500 Internal Server Error` - Erro no servidor (verificar logs)

---

### 🔑 Teste 2: Acessar recurso protegido com token

```bash
# Salve o token em uma variável
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Acesse um endpoint protegido
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/usuarios
```

**Resposta esperada (200 OK):**
Página HTML com lista de usuários

---

### ❌ Teste 3: Acesso sem token (deve redirecionar para /login)

```bash
curl http://localhost:8080/usuarios
```

**Resposta esperada (302 Redirect):**
```
Location: /login
```

---

### ❌ Teste 4: Token inválido/expirado

```bash
TOKEN="token.invalido.aqui"

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/usuarios
```

**Resposta esperada:**
- Sem autorização (será tratado como não autenticado)
- Redirecionará para /login ou retornará 403

---

### 📱 Teste 5: Via Postman/Insomnia

#### 1. POST `/api/auth/login` (obter token)
- **Method:** POST
- **URL:** http://localhost:8080/api/auth/login
- **Body (JSON):**
```json
{
  "matricula": "admin",
  "senha": "admin123"
}
```
- **Headers:** `Content-Type: application/json`

#### 2. GET `/usuarios` (usar token)
- **Method:** GET
- **URL:** http://localhost:8080/usuarios
- **Headers:** `Authorization: Bearer <token_aqui>`

---

### 🧪 Teste 6: Interface Web (Thymeleaf)

1. Acesse http://localhost:8080/login
2. Formulário de login com:
   - **Matrícula:** admin
   - **Senha:** admin123
3. Clique em "Entrar"
4. Será redirecionado para `/usuarios`

**Nota:** O formulário HTML usa `form-based login` (não JWT direto). 
Para autenticação JWT via API, use POST `/api/auth/login`.

---

### 🔒 Segurança — Boas Práticas Implementadas

✅ **Senhas criptografadas com BCrypt**
- Salt aleatório em cada hash
- Comparação segura (não é texto plano)

✅ **JWT assinado com HMAC256**
- Impossível forjar sem chave secreta
- Claims validados: ID, matrícula, nome

✅ **Variáveis de ambiente**
- `JWT_SECRET` em `.env` (fora do git)
- Diferente em cada ambiente (dev/test/prod)

✅ **Tokens com expiração**
- Padrão: 60 minutos
- Configurável via `JWT_EXPIRATION_MINUTES`

✅ **SecurityContext isolado por requisição**
- Cada request tem seu próprio contexto
- Sem vazamento entre usuários

---

### 📊 Estrutura de Usuários Iniciais

| Matrícula | Nome                        | Senha      | Status  |
|-----------|---------------------------|------------|---------|
| admin     | Administrador do Sistema   | admin123   | Ativo   |
| mat001    | João Silva                | senha123   | Ativo   |

---

### ⚠️ Cuidados em Produção

```env
# Em .env PRODUÇÃO:
JWT_SECRET=gerar-com-openssl-rand-base64-32-MUITO-SEGURA
JWT_EXPIRATION_MINUTES=30
```

**Comando para gerar chave segura:**
```bash
openssl rand -base64 32
```

---

### 🔄 Fluxo da Aplicação

```
┌─────────────┐
│  /login     │  Página de login (GET) — Pública
└──────┬──────┘
       │
       ▼
┌──────────────────────────────┐
│  POST /api/auth/login        │  API de autenticação — Pública
│  (matricula + senha)         │  Retorna JWT
└──────┬───────────────────────┘
       │ Token recebido
       ▼
┌──────────────────────────────┐
│  GET /usuarios               │  Recurso protegido
│  Header: Bearer <token>      │  Requer autenticação
└──────────────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│  SecurityFilter              │  Valida token
│  - Extrai token             │  - Configura autenticação
│  - Valida assinatura        │  - Autoriza acesso
└──────────────────────────────┘
```

---

### 🐛 Debugging

**Verificar token decodificado:**
```bash
# Adicione o token em: https://jwt.io
```

**Logs da aplicação:**
```bash
# Terminal onde mvn spring-boot:run está rodando
# Procure por:
# - "Token válido para usuário"
# - "Autenticação configurada"
# - "Falha de autenticação"
```

**Banco de dados:**
```bash
# Verificar usuários salvos com hash de senha
docker compose -f docker/docker-compose.dev.yml exec postgres psql -U admin -d chamados
> SELECT id, matricula, nome_completo, senha FROM usuario;
```

---

### ✅ Checklist de Testes

- [ ] POST `/api/auth/login` com credenciais válidas → retorna token
- [ ] GET `/usuarios` com token válido → retorna página
- [ ] GET `/usuarios` sem token → redireciona para /login
- [ ] GET `/usuarios` com token inválido → sem acesso
- [ ] POST `/usuarios` sem token → redireciona para /login
- [ ] Verificar hash de senhas no banco (não texto plano)
- [ ] Verificar DataInitializer criou usuários iniciais
- [ ] Verificar token expira após 60 minutos (ou configurado)

---

### 📚 Próximos Passos (Etapa 2+)

- [ ] Implementar roles (ADMIN, USER, SUPPORT)
- [ ] Permissões por endpoint (CustomPermissionEvaluator)
- [ ] Rate limiting com Bucket4j
- [ ] Refresh tokens
- [ ] OAuth2 / OpenID Connect
- [ ] 2FA (Two-Factor Authentication)
- [ ] Auditoria de login (logs)
- [ ] Revogação de tokens (blacklist)

---

**Status:** ✅ JWT Implementado e Testável
**Data:** 03/05/2026
