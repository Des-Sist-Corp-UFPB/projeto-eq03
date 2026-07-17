# Relatório de Carga e Performance — EQ03 (k6)

## Estratégia
**Escada de carga (staircase) com constant-arrival-rate**: o teste sobe em
degraus de RPS fixo (20, 40, 60, 80, 100, 120, 140, 160, 180, 200 req/s), cada um sustentado por
30 s, exercitando um mix realista de rotas autenticadas.
Cada degrau é medido isoladamente (p(95), taxa de erro, total de requisições).
Ao final, para cada orçamento de tempo (1s, 2s, 3s) o relatório identifica o
**maior RPS sustentado** cujo p(95) ficou dentro do orçamento **e** cuja taxa
de erro HTTP ficou abaixo de 1% — esse é o "teto de capacidade" do sistema
para aquele tempo de resposta.

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | constant-arrival-rate (degraus fixos e sequenciais) |
| Degraus testados | 20, 40, 60, 80, 100, 120, 140, 160, 180, 200 req/s |
| Duração por degrau | 30 s |
| Duração total | 300 s |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /appointments?status=PENDING/CONFIRMED · GET /appointments · GET /cashflow · GET /users · GET /clients · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Teto de capacidade por orçamento de tempo
- **Até 1 s:** o sistema sustenta **120 req/s** com p(95) de 211 ms e 0.00% de erro — nesse ritmo, entregou **3949 requisições bem-sucedidas em 30 s** de teste sustentado.
- **Até 2 s:** o sistema sustenta **120 req/s** com p(95) de 211 ms e 0.00% de erro — nesse ritmo, entregou **3949 requisições bem-sucedidas em 30 s** de teste sustentado.
- **Até 3 s:** o sistema sustenta **120 req/s** com p(95) de 211 ms e 0.00% de erro — nesse ritmo, entregou **3949 requisições bem-sucedidas em 30 s** de teste sustentado.

## Resultado por degrau

| Alvo | RPS real | Req. totais | Req. OK | Erro HTTP | p(95) | p(99) | ≤1s | ≤2s | ≤3s |
|------|----------|-------------|---------|-----------|-------|-------|-----|-----|-----|
| 20 req/s | 21.8 req/s | 654 | 654 | 0.00% | 182 ms | 213 ms | ✅ | ✅ | ✅ |
| 40 req/s | 44.0 req/s | 1319 | 1319 | 0.00% | 175 ms | 215 ms | ✅ | ✅ | ✅ |
| 60 req/s | 65.9 req/s | 1978 | 1978 | 0.00% | 43 ms | 196 ms | ✅ | ✅ | ✅ |
| 80 req/s | 87.1 req/s | 2613 | 2613 | 0.00% | 162 ms | 227 ms | ✅ | ✅ | ✅ |
| 100 req/s | 109.3 req/s | 3278 | 3278 | 0.00% | 151 ms | 224 ms | ✅ | ✅ | ✅ |
| 120 req/s | 131.6 req/s | 3949 | 3949 | 0.00% | 211 ms | 379 ms | ✅ | ✅ | ✅ |
| 140 req/s | 151.3 req/s | 4540 | 4540 | 0.00% | 7805 ms | 8964 ms | ❌ | ❌ | ❌ |
| 160 req/s | 152.3 req/s | 4569 | 4569 | 0.00% | 8968 ms | 9959 ms | ❌ | ❌ | ❌ |
| 180 req/s | 177.2 req/s | 5317 | 5317 | 0.00% | 8043 ms | 8846 ms | ❌ | ❌ | ❌ |
| 200 req/s | 167.2 req/s | 5015 | 5015 | 0.00% | 8524 ms | 9153 ms | ❌ | ❌ | ❌ |

> **Req. OK** = requisições sem falha HTTP (status < 400) no degrau.
> **≤1s/≤2s/≤3s** = ✅ se p(95) do degrau ficou dentro do orçamento **e** erro < 1%.

## Gargalos Identificados

1. **Pool de conexões com o banco limitado a 5 conexões**
   (`application-dev.yaml` e `application-prod.yaml`:
   `hikari.maximum-pool-size: 5`). Sob concorrência, qualquer carga acima de
   ~5 requisições simultâneas dependentes do banco enfileira, o que explica o
   ponto em que a latência começa a crescer nos degraus mais altos da escada.
   **Correção sugerida:** aumentar o pool (ex.: 20–30) e monitorar uso de
   conexões em produção antes de fixar um valor definitivo.

## Saúde Global
* **Total de requisições (todos os degraus):** 33232
* **Requisições OK somadas:** 33232

---
*Relatório gerado automaticamente via k6 em 2026-07-17T20:11:00.917Z*
