# 💇‍♀️ Salon - Espaço Cristiane Moura

**Serviço de Gestão e Agendamento para Salão de Beleza**

Sistema para gerenciamento de salão de beleza. Abrange operações administrativas, agendamento online, gestão de equipe e relatórios financeiros.

---

## 🚀 Status da Entrega 1 (Requisitos Acadêmicos)

O sistema cumpre todos os requisitos exigidos utilizando padrões modernos de desenvolvimento:

- **Log de Auditoria:**
  - **Backend:** Interceptação de requisições usando a anotação `@Auditable` e filtro HTTP. Captura IP real, User-Agent e mascara dados sensíveis (senhas, cartões) antes de salvar no banco.
  - **Frontend:** Console administrativo com filtros combinados e leitor de JSON com _syntax highlighting_.
- **Integração com Serviços Externos (Resend API & Mercado Pago):**
  - **E-mails Transacionais (Resend API):** Envio de confirmações e cancelamentos em segundo plano (`@Async`) usando templates Thymeleaf e o `RestClient` do Spring.
  - **Pagamentos via PIX (Mercado Pago API):** Geração de QR Code e Pix Copia e Cola (Checkout Transparente) com coleta JIT (Just-In-Time) de CPF (validação por Módulo 11) e Webhooks protegidos por assinatura de segurança (`x-signature` via HMAC-SHA256) para conciliação automática.
- **Suporte a PWA (Progressive Web App):**
  - O frontend foi construído como um PWA (Progressive Web App) utilizando `vite-plugin-pwa`. Isso permite a instalação local do aplicativo, cache inteligente de recursos estáticos com Workbox e funcionamento offline-first da interface, com estratégias *NetworkFirst* de cache para rotas públicas e feature flags.
- **Testes de Qualidade e Cobertura Comprovável:**
  - **Backend (JaCoCo - Linhas: 92.78% | Instruções: 93.56% | Branches: 78.61%):** Testes unitários/integração com JUnit 5 e Mockito. Relatório de cobertura disponível em [cobertura/backend/index.html](./cobertura/backend/index.html).
  - **Frontend (Vitest - Linhas: 99.15% | Branches: 91.87%):** Relatório de cobertura disponível em [cobertura/frontend/index.html](./cobertura/frontend/index.html).

---

## 📝 Log de Auditoria

O sistema de auditoria registra ações críticas de gravação ou autenticação realizadas no sistema.

- **O que é auditado:**
  - Operações de escrita em entidades de negócio (criação, edição e exclusão de produtos, serviços, funcionários, clientes e agendamentos).
  - Controle de acesso (login de usuários, geração de tokens, atualização/inativação de contas).
  - Alterações de configurações de infraestrutura (estados das Feature Flags).
  - Execução de pagamentos via PIX.
- **Onde fica armazenado:**
  - Os logs são persistidos na tabela relacional `tb_audit_log` no banco de dados.
  - **Principais campos:** `id` (PK), `user_email` (e-mail do operador ou `GUEST`), `action` (descrição da ação/HTTP método e endpoint), `entity_type` (nome da classe/entidade afetada), `entity_id` (identificador do registro modificado), `status` (resultado da operação: `SUCCESS` ou `FAILURE`), `ip_address` (IP de origem resolvendo cabeçalhos como `X-Forwarded-For`), `user_agent` (navegador e OS do cliente), `created_at` (timestamp local de America/Recife).
- **Como foi implementado:**
  - **Programação Orientada a Aspectos (AOP):** Utilização da anotação customizada `@Auditable` em métodos de escrita nos controladores. O aspecto `AuditAspect` intercepta a execução, resolve o resultado (se houve sucesso ou se uma exceção foi disparada) e grava o log de forma assíncrona.
  - **Filtro HTTP (Spring Security Filter):** O filtro `AuditRequestFilter` intercepta todas as requisições HTTP para registrar acessos a endpoints e mapear dados da requisição (IP, User-Agent, cabeçalhos).
- **Classes e arquivos participantes:**
  - [AuditAspect.java](./salon-back/src/main/java/com/cristiane/salon/aspect/AuditAspect.java) (Aspecto interceptor)
  - [Auditable.java](./salon-back/src/main/java/com/cristiane/salon/aspect/Auditable.java) (Anotação de controle)
  - [AuditRequestFilter.java](./salon-back/src/main/java/com/cristiane/salon/security/AuditRequestFilter.java) (Filtro de requisição HTTP)
  - [AuditLogService.java](./salon-back/src/main/java/com/cristiane/salon/models/audit/AuditLogService.java) (Serviço de negócio)
  - [AuditLog.java](./salon-back/src/main/java/com/cristiane/salon/models/audit/AuditLog.java) (Entidade JPA)

---

## 🔗 Integração com Serviço Externo

O sistema integra-se com serviços de e-mail e gateways de pagamento em produção.

- **Serviços Externos Utilizados:**
  1. **Resend API:** Envio de e-mails transacionais (solicitações, confirmações e cancelamentos de agendamento).
  2. **Mercado Pago API (PIX):** Checkout transparente para geração JIT de chaves PIX (cópia e cola) e QR Codes, além de recepção de Webhooks para atualização automatizada do status da reserva.
- **Para que são usados:**
  - O **Resend** envia notificações automáticas em segundo plano aos clientes e administradores em eventos chave da agenda.
  - O **Mercado Pago** gerencia a cobrança de reservas, garantindo conciliação bancária imediata e conciliação segura via Webhooks.
- **Como são configurados (Variáveis de Ambiente):**
  - `MAIL_API_URL` e `MAIL_PASSWORD` (API Key do Resend).
  - `MP_ACCESS_TOKEN` (Access Token da conta do Mercado Pago).
  - `MP_WEBHOOK_SECRET` (Chave de criptografia secreta usada para validar a assinatura `x-signature` das requisições via HMAC-SHA256).
- **Classes e arquivos participantes:**
  - [EmailService.java](./salon-back/src/main/java/com/cristiane/salon/integrations/email/service/EmailService.java) (Integração Resend)
  - [MercadoPagoPaymentService.java](./salon-back/src/main/java/com/cristiane/salon/integrations/payment/service/MercadoPagoPaymentService.java) (Comunicação com SDK Mercado Pago e assinatura digital)
  - [MercadoPagoWebhookController.java](./salon-back/src/main/java/com/cristiane/salon/integrations/payment/controller/MercadoPagoWebhookController.java) (Endpoint de retorno e conciliação do PIX)

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

---

## 📊 Avaliação de Performance (k6)

Realizamos testes de carga automatizados com o [k6](https://k6.io/) para medir quantas requisições o sistema consegue atender dentro de orçamentos de tempo de 1s, 2s e 3s, sob concorrência crescente.

Os artefatos completos do teste (script, relatório e saída JSON) estão em [`loadtest/`](./loadtest/).

---

### 1. Estratégia e rotas testadas

Adotamos **3 cenários independentes com `ramping-arrival-rate`** — o k6 controla diretamente o RPS (requisições por segundo) e aloca os VUs necessários automaticamente. Cada cenário tem um alvo de RPS crescente e dois critérios de aprovação: `p(95) < orçamento` **e** `taxa de erro HTTP < 1%`. O **contador `Req. OK no budget`** registra apenas as requisições que retornaram 2xx _e_ cuja latência ficou dentro do orçamento do cenário.

| Cenário | Alvo RPS | Threshold latência | Threshold erro | Duração |
|---|---|---|---|---|
| `sla_1s` | 20 req/s | p(95) < 1 000 ms | taxa < 1% | 120 s |
| `sla_2s` | 25 req/s | p(95) < 2 000 ms | taxa < 1% | 120 s |
| `sla_3s` | 30 req/s | p(95) < 3 000 ms | taxa < 1% | 120 s |

Simulamos o **caminho crítico da aplicação** com fluxo autenticado (JWT via admin) misturando leitura e escrita em **18 rotas**, com distribuição probabilística:

| Rota | Método | Tipo | Aprox. |
|---|---|---|---|
| `GET /v1/reports/financial` | GET | Relatório financeiro (agregação SQL) | 8% |
| `GET /v1/reports/appointments` | GET | Relatório de agendamentos | 7% |
| `GET /v1/reports/payroll` | GET | Relatório de folha | 4% |
| `GET /v1/reports/financial/employees/{id}` | GET | Histórico financeiro por profissional | 3% |
| `GET /v1/appointments` | GET | Listagem de agendamentos (paginada) | 7% |
| `GET /v1/cashflow` | GET | Extrato de caixa | 7% |
| `GET /v1/users` | GET | Listagem de usuários | 4% |
| `GET /v1/clients` | GET | Listagem de clientes | 4% |
| `GET /v1/employees/booking` | GET | Funcionários disponíveis p/ agendamento | 6% |
| `GET /v1/products` | GET | Listagem de produtos | 5% |
| `GET /v1/services` | GET | Listagem de serviços | 5% |
| `GET /ping` | GET | Health check | 5% |
| `POST /v1/appointments` | POST | Criação de agendamento | 8% |
| `POST /v1/cashflow` | POST | Lançamento de caixa | 7% |
| `POST+DELETE /v1/cashflow/{id}` | POST+DELETE | Lançamento e exclusão de caixa | 5% |
| `POST+PATCH /v1/appointments/{id}/cancel` | POST+PATCH | Criação e cancelamento de agendamento | 5% |
| `PUT /v1/products/{id}` | PUT | Atualização de produto | 5% |
| `PATCH /v1/users/{id}` | PATCH | Atualização de usuário | 5% |

---

### 2. Resultados — Requisições OK dentro do orçamento de tempo

| Cenário | Alvo | RPS real | **Req. OK no budget** | Req. totais | p(95) | p(99) | Erro HTTP | SLA |
|---|---|---|---|---|---|---|---|---|
| `sla_1s` (≤ 1 s) | 20 req/s | 14,1 req/s | **1.690** | 1.690 | 377 ms | 464 ms | 0,00% | ✅ PASSOU |
| `sla_2s` (≤ 2 s) | 25 req/s | 17,8 req/s | **2.138** | 2.138 | 394 ms | 522 ms | 0,00% | ✅ PASSOU |
| `sla_3s` (≤ 3 s) | 30 req/s | 21,1 req/s | **2.532** | 2.532 | 396 ms | 526 ms | 0,00% | ✅ PASSOU |

> **Req. OK no budget** = requisições com status HTTP 2xx **e** latência dentro do orçamento do cenário. Como os 3 SLAs passaram, `Req. totais = Req. OK` (100% de aproveitamento).

**Saúde global (todos os cenários somados):**

| Métrica | Resultado | Status |
|---|---|---|
| Total de requisições | 6.360 | — |
| Requisições OK no budget | 6.360 | ✅ 100% |
| Taxa de erro HTTP | 0,00% | ✅ Abaixo de 1% |

> **Interpretação:** Com os alvos de 20/25/30 req/s e banco de dados com dados acumulados de múltiplos testes, o sistema entregou **1.690 requisições em ≤1s**, **2.138 em ≤2s** e **2.532 em ≤3s** — todos com erro zero. Os targets podem ser aumentados via variáveis de ambiente (`MAX_RPS_1S`, `MAX_RPS_2S`, `MAX_RPS_3S`) em um banco mais limpo.

---

### 3. Gargalos identificados e melhorias aplicadas

#### 🐘 Gargalo 1 — Pool de Conexões do Banco de Dados (HikariCP)

Ao escalarmos os VUs para além de 30 usuários simultâneos, observamos que o `p(95)` disparava para **mais de 4 segundos**, enquanto a CPU e a memória da aplicação permaneciam estáveis. Diagnosticamos que o gargalo não era computacional, mas sim uma **fila de espera no pool de conexões do HikariCP**, que por padrão era limitado a `maximum-pool-size: 5`. Com muitos VUs ativos, threads ficavam bloqueadas aguardando uma conexão disponível.

**O que fizemos:** Tornamos o tamanho do pool configurável via variável de ambiente (`${DB_POOL_SIZE:5}`) no `application-dev.yaml` e injetamos `DB_POOL_SIZE=100` no perfil de performance via `docker-compose.performance.yml`. A latência caiu imediatamente após a mudança.

#### 🔐 Gargalo 2 — Autorização incorreta no endpoint de Relatório Financeiro (HTTP 403)

Durante a análise dos logs do k6, identificamos que a rota `GET /v1/reports/financial` respondia com **status 403 Forbidden** para o usuário admin. A causa raiz foi que o `ReportController` utilizava a anotação `@PreAuthorize("hasAnyRole('ADMIN')")`, a expressão padrão do Spring Security, que ignorava completamente o validador customizado da equipe (`VerifyUserPermissions`).

**O que fizemos:** Padronizamos as anotações do `ReportController` para `@PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")`, alinhando-as à arquitetura de segurança do projeto. A correção eliminou todos os erros 403.

