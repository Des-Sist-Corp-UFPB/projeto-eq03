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

Testamos com o [k6](https://k6.io/) uma única pergunta: **qual o maior número
de requisições por segundo que o sistema sustenta, com 100% de sucesso e
resposta em até 1 segundo** (p(95) ≤ 1000 ms — o limiar clássico de UX para
"sente como instantâneo")?

Os artefatos completos estão em [`loadtest/`](./loadtest/): as duas fases
exploratórias em `report-fase1-bracket.md` e `report-fase2-busca-fina.md`
(+ seus `resultado-*.json`), e o resultado final confirmado em
`loadtest/report.md` / `loadtest/resultado.json` — os nomes padrão que o
script grava quando você não sobrescreve `REPORT_PATH`/`RESULT_PATH` (ver
seção 1).

---

### 0. Como rodar

Pré-requisitos: projeto rodando localmente via `docker compose up -d` e Docker instalado (roda o k6 via imagem oficial, sem precisar instalar nada extra). Comandos abaixo em Linux/macOS (bash/zsh) — para PowerShell, troque `\` por `` ` `` no fim de cada linha; para cmd.exe, troque por `^` e `${PWD}` por `%cd%`.

O script tem dois modos, escolhidos por `MODE`, com parâmetros ajustáveis via variável de ambiente:

| Variável | Modo | Padrão | Efeito |
|---|---|---|---|
| `MODE` | — | `staircase` | `staircase` (escada) ou `soak` (RPS fixo sustentado) |
| `SLA_MS` | ambos | `1000` | Orçamento de latência (p(95)) que define "OK" |
| `STEP_START` / `STEP_INCREMENT` / `STEP_COUNT` | staircase | `20`/`20`/`10` | Faixa de RPS da escada |
| `STEP_DURATION_S` | staircase | `30` | Duração de cada degrau (segundos) |
| `SOAK_RPS` | soak | *(obrigatório)* | RPS fixo a sustentar |
| `SOAK_DURATION_S` | soak | `180` | Duração do soak (segundos) |
| `REPORT_PATH` / `RESULT_PATH` | ambos | `loadtest/report.md` / `loadtest/resultado.json` | Onde salvar a saída |

**Comandos exatos para reproduzir as 3 fases documentadas abaixo** (rode em sequência; limpe o banco entre uma fase e outra — dados de teste ficam marcados com datas `2099-*`, o `teardown()` do próprio script já cancela/exclui a maior parte, mas agendamentos cancelados continuam ocupando linhas na tabela já que a API não tem `DELETE` para esse recurso):

```bash
# Fase 1 — bracket grosso (~5 min)
docker run --rm -i --network projeto-eq03_salon-network \
  -v "${PWD}:/app" -w /app --env-file .env -e BASE_URL=http://salon-app:8080 \
  -e STEP_START=20 -e STEP_INCREMENT=20 -e STEP_COUNT=10 -e STEP_DURATION_S=25 \
  -e REPORT_PATH=loadtest/report-fase1-bracket.md -e RESULT_PATH=loadtest/resultado-fase1-bracket.json \
  grafana/k6 run loadtest/carga.js

# limpar banco (ver loadtest/README.md do professor para o compose local),
# depois Fase 2 — busca fina na faixa de transição encontrada na fase 1 (~5 min)
docker run --rm -i --network projeto-eq03_salon-network \
  -v "${PWD}:/app" -w /app --env-file .env -e BASE_URL=http://salon-app:8080 \
  -e STEP_START=100 -e STEP_INCREMENT=5 -e STEP_COUNT=9 -e STEP_DURATION_S=30 \
  -e REPORT_PATH=loadtest/report-fase2-busca-fina.md -e RESULT_PATH=loadtest/resultado-fase2-busca-fina.json \
  grafana/k6 run loadtest/carga.js

# limpar banco de novo, depois Fase 3 — soak de confirmação no RPS candidato (~4 min)
# usa os nomes padrão (REPORT_PATH/RESULT_PATH não sobrescritos): esse É o
# resultado final, por isso vira loadtest/report.md e loadtest/resultado.json
docker run --rm -i --network projeto-eq03_salon-network \
  -v "${PWD}:/app" -w /app --env-file .env -e BASE_URL=http://salon-app:8080 \
  -e MODE=soak -e SOAK_RPS=100 -e SOAK_DURATION_S=180 \
  grafana/k6 run loadtest/carga.js
```

> O nome da rede (`projeto-eq03_salon-network`) segue o padrão `<pasta-do-projeto>_<nome-da-rede-no-compose>`. Se o diretório do projeto tiver outro nome, confira com `docker network ls`.

---

### 1. Metodologia

Fizemos 3 execuções manuais em sequência, cada uma refinando a anterior:

1. **Bracket grosso** — escada de passo largo (20 em 20 req/s) para achar
   ENTRE quais degraus o sistema quebra. → [`report-fase1-bracket.md`](./loadtest/report-fase1-bracket.md)
2. **Busca fina** — escada de passo estreito (5 em 5 req/s), só na faixa de
   transição encontrada na fase 1, para localizar o teto com precisão.
   → [`report-fase2-busca-fina.md`](./loadtest/report-fase2-busca-fina.md)
3. **Soak de confirmação** — sustenta o RPS candidato por 3 minutos
   contínuos (`MODE=soak`) para provar que é capacidade real, não sorte de
   uma janela curta. A latência é comparada entre a primeira e a segunda
   metade da janela — um RPS só é considerado confirmado se a segunda
   metade também ficar dentro do SLA (degradação progressiva reprova o
   teste, mesmo que a média geral pareça OK). **Este é o resultado final** —
   → [`report.md`](./loadtest/report.md)

Cada arquivo listado acima é a saída **genuína e não editada** do script
para aquela execução — nenhum resultado foi digitado ou combinado à mão.
O banco foi limpo entre cada execução (dados de teste marcados com datas
exclusivas de 2099, removidos via SQL) para a tabela não crescer de uma
fase para a outra e enviesar a medição.

Distribuição probabilística das **18 rotas** testadas (leitura + escrita, fluxo autenticado via JWT admin):

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

Todos os dados de escrita são isolados por marcadores exclusivos de teste (data `2099-01-01`/`2099-10-15`/`2099-10-20`, usuário de teste dedicado) e removidos automaticamente por um `teardown()` ao final de cada execução — agendamentos são cancelados (a API não expõe `DELETE` para esse recurso); cashflow e usuários de teste são excluídos de fato.

---

### 2. Resultado final

✅ **O sistema sustenta 100 requisições por segundo, com 100% de sucesso e
p(95) ≤ 1000 ms, confirmado por 3 minutos de carga contínua.**

Ao longo dessa janela de confirmação, processou **19.744 requisições com
sucesso** (100%, nenhuma falha), com p(95) de 678 ms.

| Fase | Achado | p(95) | Erro | Arquivo |
|---|---|---|---|---|
| 1 — Bracket grosso (20 em 20, 25s/degrau) | teto indicado: 120 req/s (colapso em 140) | 386 ms | 0,00% | [report-fase1-bracket.md](./loadtest/report-fase1-bracket.md) |
| 2 — Busca fina (5 em 5, 30s/degrau) | teto indicado: 105 req/s (colapso em 110) | 162 ms | 0,00% | [report-fase2-busca-fina.md](./loadtest/report-fase2-busca-fina.md) |
| 3 — Soak (100 req/s, 3 min) | **confirmado** | **678 ms** | 0,00% | [report.md](./loadtest/report.md) |

> **Por que rodar 3 fases em vez de confiar direto na escada:** em execuções
> anteriores desta mesma metodologia, a busca fina chegou a indicar 150 req/s
> como aprovado (p(95)=372 ms em janelas de 30s) — mas ao sustentar esse
> valor por 3 minutos inteiros no soak, a latência subiu para p(95)≈5,9 s.
> A janela curta tinha sido sorte, não capacidade real; o mesmo aconteceu
> depois em 120 req/s (p(95)≈1,8 s no soak). Só 100 req/s se sustentou de
> forma confiável em todas as tentativas. **Isso é evidência direta de por
> que a etapa de confirmação por soak é indispensável** — sem ela, o
> relatório poderia reportar um número bem maior do que o sistema realmente
> aguenta de forma sustentada. A escada por si só serve para *localizar*
> rapidamente a região de interesse; só o soak *confirma*.
>
> Também repare que a fase 2 desta execução refinou o teto para 105 req/s
> (não 120, como a fase 1 sozinha sugeriu) — o ponto exato de colapso variou
> um pouco entre as duas fases (natural, já que estamos bem na margem de um
> recurso pequeno e sensível a variância — o pool de 5 conexões). Isso
> reforça por que o soak de 100 req/s, com folga sob os dois achados, é o
> número que efetivamente reportamos como resultado.

---

### 3. Gargalo identificado

#### 🐘 Pool de conexões do banco de dados (HikariCP) limitado a 5

O perfil `prod` (usado pela imagem Docker padrão) define `hikari.maximum-pool-size: 5` de forma fixa em `application-prod.yaml`, sem variável de ambiente para ajuste. Sob concorrência acima de ~5 requisições simultâneas dependentes do banco, threads ficam bloqueadas aguardando uma conexão livre. Isso explica tanto o colapso abrupto visto nas escadas (entre 105–140 req/s, variando um pouco entre execuções) quanto a razão de o número indicado pela escada não se sustentar sob carga contínua no soak — o pool pequeno absorve rajadas curtas de alguns segundos, mas não aguenta minutos de carga constante, e a margem exata em que ele estoura é sensível a variância (concorrência de outras conexões, GC, etc.), por isso o resultado final reportado (100 req/s) fica com folga abaixo de qualquer teto observado nas escadas.

O perfil `dev` já suporta ajuste via `${DB_POOL_SIZE:5}`, e existe um overlay `docker-compose.performance.yml` com `DB_POOL_SIZE=100` pronto para esse perfil — mas o `prod` (perfil da imagem padrão testada) ainda não tem esse parâmetro exposto.

**Melhoria sugerida:** parametrizar `maximum-pool-size` também em `application-prod.yaml` (ex.: `${DB_POOL_SIZE:20}`) e rodar este mesmo teste de novo para medir o novo teto sustentável — a expectativa é que suba substancialmente, já que CPU e memória não eram o fator limitante em nenhum momento deste teste.

