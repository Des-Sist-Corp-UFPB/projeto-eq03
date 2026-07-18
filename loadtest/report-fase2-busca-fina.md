# Relatório de Carga — Fase Staircase (k6)

## Estratégia
Escada de carga com `constant-arrival-rate`: RPS fixo por degrau
(100, 105, 110, 115, 120, 125, 130, 135, 140 req/s), cada um sustentado por 30 s.
SLA único: p(95) ≤ 1000 ms **e** erro HTTP < 1%. O teto de capacidade é
o maior RPS numa sequência ininterrupta de degraus aprovados desde o
primeiro degrau testado — um degrau que "passa" isoladamente depois de uma
falha anterior não conta (variância transitória, não capacidade real).

## Teto de capacidade (SLA ≤ 1000 ms)
O sistema sustenta **105 req/s** com p(95) de 162 ms e 0.00% de erro — nesse ritmo, entregou **3428 requisições bem-sucedidas em 30 s** de teste.

## Resultado por degrau

| Alvo | RPS real | Req. totais | Req. OK | Erro HTTP | p(95) | p(99) | OK≤1000ms |
|------|----------|-------------|---------|-----------|-------|-------|---------|
| 100 req/s | 110.1 req/s | 3303 | 3303 | 0.00% | 156 ms | 230 ms | ✅ |
| 105 req/s | 114.3 req/s | 3428 | 3428 | 0.00% | 162 ms | 237 ms | ✅ |
| 110 req/s | 119.7 req/s | 3591 | 3591 | 0.00% | 6203 ms | 7359 ms | ❌ |
| 115 req/s | 125.5 req/s | 3766 | 3742 | 0.64% | 5788 ms | 6742 ms | ❌ |
| 120 req/s | 131.6 req/s | 3948 | 3948 | 0.00% | 180 ms | 307 ms | ✅ |
| 125 req/s | 136.8 req/s | 4105 | 4105 | 0.00% | 193 ms | 307 ms | ✅ |
| 130 req/s | 142.4 req/s | 4273 | 4273 | 0.00% | 228 ms | 374 ms | ✅ |
| 135 req/s | 147.4 req/s | 4423 | 4423 | 0.00% | 3912 ms | 4562 ms | ❌ |
| 140 req/s | 151.8 req/s | 4553 | 4548 | 0.11% | 4776 ms | 5545 ms | ❌ |

## Saúde Global
* **Total de requisições:** 35390
* **Requisições OK:** 35361

---
*Gerado em 2026-07-18T02:01:22.929Z*
