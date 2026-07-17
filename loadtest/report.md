# Relatório de Carga e Performance — EQ03 (k6)

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | ramping-vus (auto: 1 → 60 VUs por cenário) |
| Duração por cenário | 120 s (90 s ramp + 30 s sustentado) |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /reports/financial/employees/{id} · GET /appointments · GET /cashflow · GET /users · GET /clients · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Resultado Principal — Requisições HTTP-OK por Budget de Tempo

Cada cenário é **independente**: roda com seus próprios VUs e conta apenas
requisições com status HTTP 2xx **e** tempo de resposta dentro do budget.

| Budget | RPS real | Req. OK no Budget | p(95) | p(99) | Meta >1.000 |
|--------|----------|-------------------|-------|-------|-------------|
| **≤ 1 s** | 53.73 req/s | **4.479** | 2988.16 ms | 4335.05 ms | ✅ |
| **≤ 2 s** | 49.00 req/s | **4.572** | 3135.26 ms | 4601.53 ms | ✅ |
| **≤ 3 s** | 50.12 req/s | **5.056** | 3173.12 ms | 4533.05 ms | ✅ |

## Saúde Global
* **Total de requisições (todos os cenários):** 18.346
* **Taxa de falha HTTP:** 0.01% (máximo permitido: 1,00%)

## Interpretação
O teste rampa automaticamente de 1 até 60 VUs durante cada cenário.
As colunas "Req. OK no Budget" mostram quantas requisições o sistema entregou
dentro do limite de tempo daquele cenário, independentemente dos outros.

---
*Relatório gerado automaticamente via k6 em 2026-07-07T15:28:17.263Z*
