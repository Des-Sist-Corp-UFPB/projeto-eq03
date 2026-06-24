# 💇‍♀️ Salon - Espaço Cristiane Moura

**Serviço de Gestão e Agendamento para Salão de Beleza**

Sistema para gerenciamento de salão de beleza. Abrange operações administrativas, agendamento online, gestão de equipe e relatórios financeiros.

---

## 🚀 Status da Entrega 1 (Requisitos Acadêmicos)

O sistema cumpre os requisitos exigidos utilizando padrões modernos de desenvolvimento:

- **Log de Auditoria:**
  - **Backend:** Interceptação de requisições usando a anotação `@Auditable`. Captura IP real, User-Agent e mascara dados sensíveis (senhas, cartões) antes de salvar no banco.
  - **Frontend:** Console administrativo com filtros combinados e leitor de JSON com _syntax highlighting_.
- **Integração com Serviços Externos (Resend API & Mercado Pago):**
  - **E-mails Transacionais (Resend API):** Envio de confirmações e cancelamentos em segundo plano (`@Async`) usando templates Thymeleaf e o `RestClient` do Spring.
  - **Pagamentos via PIX (Mercado Pago API):** Geração de QR Code e Pix Copia e Cola (Checkout Transparente) com coleta JIT (Just-In-Time) de CPF (validação por Módulo 11) e Webhooks protegidos por assinatura de segurança (`x-signature` via HMAC-SHA256) para conciliação automática.
- **Testes de Qualidade e Cobertura Comprovável:**
  - **Backend (JaCoCo - ~91%):** Testes unitários/integração com JUnit 5 e Mockito. O Maven está configurado para **barrar o build** se a cobertura cair abaixo de 85%.
  - **Frontend (Vitest - ~99%):** Cobertura quase total de rotas, contextos e UI utilizando React Testing Library.

---

## 🛠️ Stack Tecnológica

- **Backend:** Java 21 · Spring Boot 4.0.6 · Spring Security · JWT · Spring Data JPA · PostgreSQL · Flyway · JaCoCo · Lombok · Springdoc OpenAPI
- **Frontend:** React 19 · TypeScript · Vite · Vitest · Tailwind CSS v4.0 · Axios · React Router v7 · React Hook Form · Recharts · jsPDF · PWA
- **Infra:** Docker · Docker Compose · GitHub Actions · Nginx

---

## 💎 Padrões de Arquitetura

- **Soft-Deletes:** Exclusão lógica (campo `active`) para produtos e usuários, não quebrando o histórico financeiro do banco.
- **Global Exception Handler:** Centralização de erros (`@RestControllerAdvice`). Oculta metadados do banco em falhas e formata respostas de erro de forma amigável.
- **Igualdade Referencial:** Uso de `useCallback` e `useMemo` no React para evitar re-renderizações e travamentos na interface.
- **Feature Flags Dinâmicas:** Toggles no banco de dados (`CLIENT_BOOKING`, `EMAIL_NOTIFICATIONS`) que ligam/desligam funcionalidades no sistema em tempo real, sem precisar de novo deploy.

---

## 📁 Estrutura do Monorepo

```text
projeto-eq03/
├── salon-back/      # API Spring Boot (Java 21)
├── salon-front/     # SPA React (PWA)
├── docs/            # Documentação detalhada e diagramas
├── docker-compose.yml
└── README.md
```

---

## ⚙️ Pré-requisitos e Execução Local

**Requisitos:** Java 21, Maven 3.9+, Node.js 22+, PostgreSQL 16+ (ou Docker).

```bash
# 1. Clonar o repositório
git clone https://github.com/Des-Sist-Corp-UFPB/projeto-eq03.git
cd projeto-eq03

# 2. Subir banco de dados e SMTP (via Docker ou serviços locais/IDE)
docker compose up db mailpit -d

# 3. Iniciar a API Backend
cd salon-back
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Iniciar o Frontend
cd ../salon-front
npm install
npm run dev
```

| Serviço      | URL Local                               | Descrição                  |
| :----------- | :-------------------------------------- | :------------------------- |
| **Frontend** | `http://localhost:5173`                 | Interface SPA / PWA        |
| **Backend**  | `http://localhost:8080`                 | API REST                   |
| **Swagger**  | `http://localhost:8080/swagger-ui.html` | Documentação dos Endpoints |

---

## 📜 Convenções de Código

- **DTOs:** Utilização de Java `record` (ex: `UserCreateRequest` / `UserResponse`, evitando o sufixo genérico `DTO`).
- **Entidades:** Mapeamento JPA com tabelas (`tb_`), usando Lombok apenas onde necessário.
- **Versionamento:** Endpoints versionados na base da URL (ex: `/v1/users`).
- **Segurança:** Autenticação via JWT com roles e controle de autoridade granular por endpoint e método HTTP.

---

## 📖 Documentação Adicional

Mais detalhes sobre a arquitetura do projeto, APIs, testes e diretrizes de desenvolvimento/agentes de IA estão disponíveis no diretório [`docs/`](./docs/):

- [ARCHITECTURE.md](./docs/ARCHITECTURE.md) - Estrutura de pacotes do Monorepo e padrões arquiteturais.
- [API.md](./docs/API.md) - Referência de Endpoints REST e Esquema de Banco de Dados.
- [TESTING.md](./docs/TESTING.md) - Estratégia de testes de qualidade no Backend e Frontend.
- [SECURITY.md](./docs/SECURITY.md) - Detalhes do controle de autenticação e autorização por papéis.
