# Relatório de Carga e Performance — EQ03 (k6)

## Estratégia
O teste usa **ramping-arrival-rate**: o k6 controla diretamente o RPS
(requisições por segundo) e descobre quantos VUs precisa. O threshold
`p(95) < Xs` responde objetivamente se o sistema sustenta aquela taxa
dentro do budget de tempo.

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | ramping-arrival-rate (1 → alvo RPS em 90s + 30s sustentado) |
| Duração por cenário | 120 s |
| Alvo sla_1s | 60 req/s |
| Alvo sla_2s | 100 req/s |
| Alvo sla_3s | 150 req/s |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /reports/financial/employees/{id} · GET /appointments · GET /cashflow · GET /users · GET /clients · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Resultado por Budget de Tempo

| Budget | RPS médio | p(95) | p(99) | Máx | SLA |
|--------|-----------|-------|-------|-----|-----|
| **≤ 1 s** | 41.6 req/s | **990 ms** | 2308 ms | 3876 ms | ✅ PASSOU |
| **≤ 2 s** | 51.6 req/s | **11761 ms** | 14319 ms | 20072 ms | ❌ FALHOU |
| **≤ 3 s** | 57.0 req/s | **16516 ms** | 19719 ms | 24685 ms | ❌ FALHOU |

## Saúde Global
* **Total de requisições (todos os cenários):** 18031
* **Taxa de falha HTTP:** 2.76% (máximo permitido: 1,00%)

## Interpretação
- **SLA PASSOU**: o sistema sustenta o RPS alvo com 95% das respostas dentro do budget.
- **SLA FALHOU**: o sistema satura antes de atingir o RPS alvo para esse budget;
  o máximo real está em algum ponto durante a rampa onde p(95) ainda estava abaixo do limite.

---
*Relatório gerado automaticamente via k6 em 2026-07-17T02:42:17.420Z*
