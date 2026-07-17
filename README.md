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

Realizamos um teste de carga automatizado com o [k6](https://k6.io/) para descobrir, para cada orçamento de tempo (1s, 2s e 3s), o **maior número de requisições por segundo que o sistema sustenta respondendo dentro desse tempo com taxa de erro desprezível**.

Os artefatos completos do teste (script, relatório e saída JSON) estão em [`loadtest/`](./loadtest/).

---

### 0. Como rodar

Pré-requisitos: projeto rodando localmente via `docker compose up -d` e Docker instalado (para rodar o k6 via imagem oficial, sem precisar instalar nada extra).

**Linux / macOS (bash ou zsh):**

```bash
docker run --rm -i --network projeto-eq03_salon-network \
  -v "${PWD}:/app" -w /app --env-file .env \
  -e BASE_URL=http://salon-app:8080 \
  grafana/k6 run loadtest/carga.js
```

**Windows (PowerShell):**

```powershell
docker run --rm -i --network projeto-eq03_salon-network `
  -v "${PWD}:/app" -w /app --env-file .env `
  -e BASE_URL=http://salon-app:8080 `
  grafana/k6 run loadtest/carga.js
```

**Windows (cmd.exe):**

```cmd
docker run --rm -i --network projeto-eq03_salon-network ^
  -v "%cd%:/app" -w /app --env-file .env ^
  -e BASE_URL=http://salon-app:8080 ^
  grafana/k6 run loadtest/carga.js
```

> O nome da rede (`projeto-eq03_salon-network`) segue o padrão `<pasta-do-projeto>_<nome-da-rede-no-compose>`. Se o diretório do projeto tiver outro nome, confira com `docker network ls`.

Os degraus de carga são ajustáveis via variáveis de ambiente (opcional):

```bash
# Linux/macOS: adicione -e antes de cada var; Windows PowerShell: mesma sintaxe -e
-e STEP_START=20 -e STEP_INCREMENT=20 -e STEP_COUNT=10 -e STEP_DURATION_S=30
```

Ao final, o script sobrescreve automaticamente `loadtest/report.md` e `loadtest/resultado.json` com os resultados da execução.

---

### 1. Estratégia e rotas testadas

Adotamos uma **escada de carga (staircase) com `constant-arrival-rate`**: o RPS sobe em degraus fixos (20, 40, 60, 80, 100, 120, 140, 160, 180, 200 req/s), cada um sustentado por 30 s, exercitando um mix realista de rotas autenticadas (JWT via admin). Cada degrau é medido isoladamente — p(95), taxa de erro, total de requisições. Ao final, para cada orçamento de tempo o relatório encontra o **maior RPS sustentado em sequência ininterrupta desde o primeiro degrau** cujo p(95) ficou dentro do orçamento **e** cuja taxa de erro ficou abaixo de 1% — esse é o teto de capacidade real (um degrau que "passa" isoladamente após uma falha anterior não conta, pois reflete variância transitória, não capacidade sustentável).

Distribuição probabilística das **18 rotas** testadas (leitura + escrita):

| Rota | Método | Tipo | Aprox. |
|---|---|---|---|
| `GET /v1/reports/financial` | GET | Relatório financeiro (agregação SQL) | 8% |
| `GET /v1/reports/appointments` | GET | Relatório de agendamentos | 7% |
| `GET /v1/reports/payroll` | GET | Relatório de folha | 4% |
| `GET /v1/appointments?status=` | GET | Agendamentos filtrados por status | 3% |
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

Todos os dados de escrita são isolados por marcadores exclusivos de teste (data `2099-01-01`/`2099-10-15`/`2099-10-20`, usuário de teste dedicado) e removidos automaticamente por um `teardown()` ao final da execução — nenhum dado do teste permanece no banco (agendamentos são cancelados, já que a API não expõe `DELETE` para esse recurso; cashflow e usuários de teste são excluídos de fato).

---

### 2. Resultados — teto de capacidade por orçamento de tempo

| Orçamento | Teto sustentado | p(95) no teto | Erro HTTP | Req. OK no degrau (30 s) |
|---|---|---|---|---|
| ≤ 1 s | **120 req/s** | 211 ms | 0,00% | 3.949 |
| ≤ 2 s | **120 req/s** | 211 ms | 0,00% | 3.949 |
| ≤ 3 s | **120 req/s** | 211 ms | 0,00% | 3.949 |

Resultado por degrau (curva completa):

| RPS alvo | RPS real | Req. totais | Req. OK | Erro HTTP | p(95) | p(99) |
|---|---|---|---|---|---|---|
| 20 | 21,8 | 654 | 654 | 0,00% | 182 ms | 213 ms |
| 40 | 44,0 | 1.319 | 1.319 | 0,00% | 175 ms | 215 ms |
| 60 | 65,9 | 1.978 | 1.978 | 0,00% | 43 ms | 196 ms |
| 80 | 87,1 | 2.613 | 2.613 | 0,00% | 162 ms | 227 ms |
| 100 | 109,3 | 3.278 | 3.278 | 0,00% | 151 ms | 224 ms |
| **120** | **131,6** | **3.949** | **3.949** | **0,00%** | **211 ms** | **379 ms** |
| 140 | 151,3 | 4.540 | 4.540 | 0,00% | **7.805 ms** | 8.964 ms |
| 160 | 152,3 | 4.569 | 4.569 | 0,00% | 8.968 ms | 9.959 ms |
| 180 | 177,2 | 5.317 | 5.317 | 0,00% | 8.043 ms | 8.846 ms |
| 200 | 167,2 | 5.015 | 5.015 | 0,00% | 8.524 ms | 9.153 ms |

> **Interpretação:** o sistema entrega 100% de sucesso com latência baixa até 120 req/s. Entre 120 e 140 req/s há um **colapso abrupto** — o p(95) salta de 211 ms para quase 8 segundos, ultrapassando de uma vez os três orçamentos (1s, 2s e 3s). Por isso o teto é o mesmo para os três tempos: não existe uma faixa intermediária em que o sistema responde, por exemplo, em 1,5s — ou está rápido, ou já rompeu os 3 segundos. Essa assinatura (sucesso estável seguido de colapso repentino, não degradação gradual) é característica de esgotamento de um recurso de tamanho fixo, como um pool de conexões.

---

### 3. Gargalo identificado

#### 🐘 Pool de conexões do banco de dados (HikariCP) limitado a 5

O perfil `prod` (usado pela imagem Docker padrão) define `hikari.maximum-pool-size: 5` de forma fixa em `application-prod.yaml`, sem variável de ambiente para ajuste. Sob concorrência acima de ~5 requisições simultâneas dependentes do banco, threads ficam bloqueadas aguardando uma conexão livre — o que bate exatamente com o ponto de colapso observado entre 120 e 140 req/s (a essa taxa, com múltiplas rotas concorrentes por operação, a demanda de conexões simultâneas facilmente ultrapassa 5).

O perfil `dev` já suporta ajuste via `${DB_POOL_SIZE:5}`, e existe um overlay `docker-compose.performance.yml` com `DB_POOL_SIZE=100` pronto para testes de performance sob esse perfil — mas o `prod` (perfil da imagem padrão testada) ainda não tem esse parâmetro exposto.

**Melhoria sugerida:** parametrizar `maximum-pool-size` também em `application-prod.yaml` (ex.: `${DB_POOL_SIZE:20}`) e validar o novo teto de capacidade com um pool maior — a expectativa é que o teto suba substancialmente acima de 120 req/s, já que CPU e memória não eram o limitante neste teste.

