# 🔐 JWT Implementation Summary

## O que foi adicionado

### ✅ Arquivos Novos

```
src/main/java/br/ufpb/dsc/chamados/
├── security/
│   ├── TokenProvider.java          ← Gera/valida JWT
│   └── SecurityFilter.java         ← Intercepta requisições com token
└── dto/
    ├── LoginRequest.java           ← DTO para login
    └── LoginResponse.java          ← DTO com token
```

### ✅ Arquivos Modificados

```
├── config/
│   ├── SecurityConfig.java         ← Integra SecurityFilter + JWT
│   ├── GlobalModelAttributes.java  (sem mudanças)
│   └── DataInitializer.java        ← ⭐ NOVO: Popula usuários iniciais
├── controller/
│   ├── AuthController.java         ← ⭐ MODIFICADO: Adicionado /api/auth/login
│   └── UsuarioController.java      (sem mudanças)
├── service/
│   ├── UserDetailsServiceImpl.java  ← ⭐ NOVO: Busca usuário no banco
│   └── UsuarioService.java         ← ⭐ MODIFICADO: Criptografa senhas
├── pom.xml                         ← ⭐ MODIFICADO: Adicionado JWT + Lombok
└── application.yml                 ← ⭐ MODIFICADO: Config JWT
```

### ✅ Arquivos de Configuração

```
.env                               ← ⭐ NOVO: Variáveis de ambiente
```

---

## 🔄 Fluxo Completo

### 1️⃣ Inicial — Usuário faz login

```
Browser
  │
  ├─ GET /login
  │  └─ Retorna página de login (Thymeleaf)
  │
  ├─ Preenche formulário
  │  └─ POST /api/auth/login
  │     {
  │       "matricula": "admin",
  │       "senha": "admin123"
  │     }
```

### 2️⃣ Servidor — Valida credenciais

```
AuthController.login()
  │
  ├─ AuthenticationManager.authenticate()
  │  │
  │  ├─ UserDetailsServiceImpl.loadUserByUsername()
  │  │  └─ UsuarioRepository.findByMatricula()
  │  │     └─ Busca no banco
  │  │
  │  └─ PasswordEncoder.matches()
  │     └─ Compara: senha enviada vs hash no banco
  │
  └─ Se OK → TokenProvider.generateToken()
             └─ Retorna LoginResponse com JWT
```

### 3️⃣ Resposta — Token gerado

```
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "matricula": "admin",
  "nomeCompleto": "Administrador do Sistema"
}
```

### 4️⃣ Cliente — Armazena token

```
localStorage.setItem("token", token)

// Ou em header padrão:
Authorization: Bearer eyJhbGciOi...
```

### 5️⃣ Cliente — Acessa recurso protegido

```
GET /usuarios
Header: Authorization: Bearer eyJhbGciOi...
```

### 6️⃣ Servidor — SecurityFilter valida

```
SecurityFilter.doFilterInternal()
  │
  ├─ Extrai token do header
  │
  ├─ TokenProvider.verifyToken()
  │  └─ Valida assinatura HMAC256
  │
  ├─ TokenProvider.getUserIdFromToken()
  │  └─ Extrai ID do usuário
  │
  ├─ UserDetailsServiceImpl.loadUserByUsername()
  │  └─ Carrega usuário do banco
  │
  └─ Configura SecurityContext
     └─ Acesso autorizado → continua
```

### 7️⃣ Resposta — Recurso retornado

```
GET /usuarios → 200 OK
<html>...usuários...</html>
```

---

## 🏗️ Componentes Técnicos

### **TokenProvider** 🎫
```java
generateToken(usuario)        // JWT → token
verifyToken(token)           // Valida assinatura
getUserIdFromToken(token)    // Extrai ID
getMatriculaFromToken(token) // Extrai matrícula
isValidToken(token)          // Verifica validade
```

**Algoritmo:** HMAC256 (SHA256 + chave secreta)
**TTL:** 60 minutos (configurável)
**Secret:** Vem de `app.jwt.secret` (variável de ambiente)

---

### **SecurityFilter** 🛡️
```
OncePerRequestFilter
├─ Intercepta toda requisição
├─ Extrai token do header "Authorization: Bearer ..."
├─ Valida token
├─ Carrega usuário no SecurityContext
└─ Continua processamento
```

**Flow:**
```
Requisição
  ↓
SecurityFilter
  ├─ Tem token? → Valida
  ├─ Válido? → Configura autenticação
  └─ Continua para controller/endpoint
```

---

### **AuthController** 🔑
```
GET  /login               → Página de login (HTML)
POST /api/auth/login      → Autentica (JSON API)
```

**POST `/api/auth/login`**
```
Request:
{
  "matricula": "admin",
  "senha": "admin123"
}

Response (200):
{
  "token": "eyJhbGc...",
  "matricula": "admin",
  "nomeCompleto": "Administrador do Sistema"
}

Response (401):
{
  "token": null,
  "nomeCompleto": "Credenciais inválidas"
}
```

---

### **UserDetailsServiceImpl** 👤
```
Implementa: UserDetailsService (Spring Security)

loadUserByUsername(matricula)
├─ Busca usuário no banco por matrícula
├─ Valida se está ativo
├─ Retorna UserDetails com:
│  ├─ username (matrícula)
│  ├─ password (hash BCrypt)
│  ├─ authorities (ROLE_USER)
│  └─ disabled (true se inativo)
```

---

### **DataInitializer** 📊
```
ApplicationRunner (executa ao subir app)
├─ Verifica se usuário "admin" existe
├─ Se não, cria:
│  ├─ matricula: "admin"
│  ├─ senha: passwordEncoder.encode("admin123")
│  ├─ nome: "Administrador do Sistema"
│  └─ email: "admin@chamados.ufpb.br"
├─ Cria usuário de teste "mat001"
```

---

## 🔐 Segurança — Implementado

| Aspecto | Implementação | Status |
|---------|--------------|--------|
| Hash de Senhas | BCrypt (salt aleatório) | ✅ |
| Token JWT | HMAC256 com secret | ✅ |
| Secret Management | Variável de ambiente | ✅ |
| Token Expiration | 60 minutos (config) | ✅ |
| Request Validation | SecurityFilter + UserDetails | ✅ |
| Password Encoder | BCryptPasswordEncoder | ✅ |
| Authentication Manager | Spring Security 6 | ✅ |
| Session Policy | Stateless (sem cookies) | ✅ |
| CSRF Protection | Desabilitado (JWT em uso) | ✅ |

---

## 📊 Propriedades Configuráveis

**`.env` (variáveis de ambiente)**
```env
JWT_SECRET=sua-chave-secreta-super-segura-32-chars
JWT_EXPIRATION_MINUTES=60
```

**`application.yml`**
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}                    # De .env
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}
```

---

## 📦 Dependências Adicionadas

```xml
<!-- JWT (Auth0) -->
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>

<!-- Lombok (reduz boilerplate) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## 🧪 Testes Implementados

```
POST /api/auth/login
├─ Credenciais válidas → 200 OK + token
├─ Credenciais inválidas → 401 Unauthorized
└─ Usuário inativo → 401 Unauthorized

GET /usuarios (com token válido)
├─ Token correto → 200 OK
├─ Token inválido → Redireciona /login ou 403
└─ Sem token → Redireciona /login ou 403
```

---

## ✅ Checklist — O que foi implementado

- ✅ TokenProvider (gera e valida JWT)
- ✅ SecurityFilter (intercepta e valida tokens)
- ✅ AuthController com endpoint `/api/auth/login`
- ✅ UserDetailsServiceImpl (busca usuário no banco)
- ✅ SecurityConfig (integra tudo)
- ✅ PasswordEncoder (BCrypt)
- ✅ DataInitializer (cria usuários iniciais)
- ✅ `.env` com variáveis de ambiente
- ✅ `application.yml` com config JWT
- ✅ Dependências (JWT + Lombok)
- ✅ Documentação (JWT-GUIDE.md)

---

## 🚀 Como Usar

### 1. Configurar .env
```bash
cp .env.example .env
# Edite com suas configurações
```

### 2. Subir aplicação
```bash
mvn spring-boot:run
# Ou com Docker
docker compose -f docker/docker-compose.dev.yml up
```

### 3. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"matricula":"admin","senha":"admin123"}'
```

### 4. Usar token
```bash
curl -H "Authorization: Bearer <token_aqui>" \
  http://localhost:8080/usuarios
```

---

## 📚 Documentação Adicional

- [JWT-GUIDE.md](JWT-GUIDE.md) — Guia técnico completo
- [TESTING.md](TESTING.md) — Como testar (atualizado)
- [CLAUDE.md](CLAUDE.md) — Memória do projeto (atualizado)

---

## 🎯 Resumo

**O que mudou?**
- ✅ Sistema mudou de **form-based login** para **JWT-based auth**
- ✅ Senhas agora são **criptografadas** (antes eram planas)
- ✅ Autenticação **stateless** (cada requisição é independente)
- ✅ Suporte a **API REST** segura (POST `/api/auth/login`)
- ✅ **DataInitializer** popula usuários automaticamente

**Benefícios:**
- 🔒 Mais seguro (tokens assinados, senhas hashadas)
- 📱 Suporta mobile apps (token em header)
- ⚡ Escalável (sem sessão no servidor)
- 🔑 Pronto para OAuth2/OpenID (futura migração)

---

**Status**: ✅ JWT Completo e Funcional
**Próximas Etapas**: Roles, Permissões, Rate Limiting, Refresh Tokens
