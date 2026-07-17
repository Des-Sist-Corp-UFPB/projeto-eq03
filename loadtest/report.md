# Relatório de Carga e Performance — EQ03 (k6)

## Estratégia
**ramping-arrival-rate**: o k6 controla diretamente o RPS (requisições por
segundo) e aloca VUs conforme necessário. Cada cenário tem um alvo de RPS e
dois critérios de aprovação: latência p(95) dentro do orçamento **e** taxa de
erro HTTP < 1%. O contador "Req. OK no budget" registra apenas as requisições
que retornaram 2xx **e** cuja latência ficou dentro do orçamento do cenário.

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | ramping-arrival-rate (1 → alvo RPS em 90 s + 30 s sustentado) |
| Duração por cenário | 120 s |
| Alvo sla_1s | 20 req/s |
| Alvo sla_2s | 25 req/s |
| Alvo sla_3s | 30 req/s |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /appointments?status=PENDING/CONFIRMED · GET /appointments · GET /cashflow · GET /users · GET /clients · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Resultado — Requisições OK dentro do orçamento de tempo

| Budget | Alvo | RPS real | Req. OK no budget | Req. totais | p(95) | p(99) | Erro HTTP | SLA |
|--------|------|----------|--------------------|-------------|-------|-------|-----------|-----|
| **≤ 1 s** | 20 req/s | 14.1 req/s | **1690** | 1690 | 377 ms | 464 ms | 0.00% | ✅ PASSOU |
| **≤ 2 s** | 25 req/s | 17.8 req/s | **2138** | 2138 | 394 ms | 522 ms | 0.00% | ✅ PASSOU |
| **≤ 3 s** | 30 req/s | 21.1 req/s | **2532** | 2532 | 396 ms | 526 ms | 0.00% | ✅ PASSOU |

> **Req. OK no budget** = requisições com status HTTP 2xx e latência ≤ orçamento do cenário.
> **Req. totais** = todas as requisições disparadas no cenário (incluindo as mais lentas).
> Se SLA FALHOU, o alvo de RPS estava além da capacidade do sistema para esse orçamento
> — reduza MAX_RPS_Xs e rode novamente para encontrar o ponto de operação estável.

## Saúde Global
* **Total de requisições (todos os cenários):** 6360
* **Requisições OK no budget somadas:** 6360

---
*Relatório gerado automaticamente via k6 em 2026-07-17T18:13:33.420Z*
