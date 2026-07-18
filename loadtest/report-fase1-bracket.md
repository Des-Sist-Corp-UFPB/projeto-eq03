# Relatório de Carga — Fase Staircase (k6)

## Estratégia
Escada de carga com `constant-arrival-rate`: RPS fixo por degrau
(20, 40, 60, 80, 100, 120, 140, 160, 180, 200 req/s), cada um sustentado por 25 s.
SLA único: p(95) ≤ 1000 ms **e** erro HTTP < 1%. O teto de capacidade é
o maior RPS numa sequência ininterrupta de degraus aprovados desde o
primeiro degrau testado — um degrau que "passa" isoladamente depois de uma
falha anterior não conta (variância transitória, não capacidade real).

## Teto de capacidade (SLA ≤ 1000 ms)
O sistema sustenta **120 req/s** com p(95) de 386 ms e 0.00% de erro — nesse ritmo, entregou **3310 requisições bem-sucedidas em 25 s** de teste.

## Resultado por degrau

| Alvo | RPS real | Req. totais | Req. OK | Erro HTTP | p(95) | p(99) | OK≤1000ms |
|------|----------|-------------|---------|-----------|-------|-------|---------|
| 20 req/s | 22.0 req/s | 549 | 549 | 0.00% | 159 ms | 239 ms | ✅ |
| 40 req/s | 44.3 req/s | 1107 | 1107 | 0.00% | 174 ms | 220 ms | ✅ |
| 60 req/s | 65.8 req/s | 1644 | 1644 | 0.00% | 201 ms | 324 ms | ✅ |
| 80 req/s | 88.4 req/s | 2211 | 2211 | 0.00% | 494 ms | 779 ms | ✅ |
| 100 req/s | 110.4 req/s | 2761 | 2761 | 0.00% | 471 ms | 777 ms | ✅ |
| 120 req/s | 132.4 req/s | 3310 | 3310 | 0.00% | 386 ms | 664 ms | ✅ |
| 140 req/s | 154.0 req/s | 3850 | 3850 | 0.00% | 1455 ms | 2107 ms | ❌ |
| 160 req/s | 176.0 req/s | 4400 | 4400 | 0.00% | 1738 ms | 4763 ms | ❌ |
| 180 req/s | 140.8 req/s | 3520 | 3520 | 0.00% | 11683 ms | 12603 ms | ❌ |
| 200 req/s | 123.6 req/s | 3091 | 3091 | 0.00% | 12399 ms | 13448 ms | ❌ |

## Saúde Global
* **Total de requisições:** 26443
* **Requisições OK:** 26443

---
*Gerado em 2026-07-18T01:38:38.244Z*
