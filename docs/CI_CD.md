# CI/CD

---

# Backend Workflow

Arquivo:

```text
.github/workflows/backend-ci.yml
```

---

## Passos

1. Checkout
2. Configurar Java 21
3. Cache Maven
4. Rodar testes
5. Build
6. Docker build

---

# Frontend Workflow

Arquivo:

```text
.github/workflows/frontend-ci.yml
```

---

## Passos

1. Checkout
2. Configurar Node 20
3. npm ci
4. Lint
5. Testes
6. Build

---

# Deploy

Arquivo:

```text
.github/workflows/deploy.yml
```

---

## Processo

- Docker Compose
- Nginx
- Variáveis de ambiente
- Deploy automatizado

---