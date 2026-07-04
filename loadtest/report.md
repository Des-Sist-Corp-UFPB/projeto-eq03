# Relatório de Carga Constante — Busca Binária da Capacidade Máxima (k6)

## Resumo Executivo — Carga Constante
Este teste executa uma carga constante de **12 VUs** por **1 minuto** para avaliar se o sistema mantém os SLAs em um nível de concorrência específico. Faz parte de uma **Busca Binária Manual** para determinar a capacidade máxima sustentável. Se todos os SLAs estiverem em 100%, aumente os VUs; se houver quebra, reduza e repita.

* **Total de Requisições:** 2510
* **Vazão Média (RPS Total):** 41.53 req/s
* **Taxa de Falha:** 0.08% (Máximo permitido: 1.00%)

## Distribuição dos Tempos de Resposta
| Métrica | Tempo (ms) |
|---|---|
| Mínimo | 2.61 ms |
| Médio | 186.64 ms |
| Mediana | 173.84 ms |
| **p(95) (95% das Requisições)** | **453.31 ms** |
| p(99) (99% das Requisições) | 0.00 ms |
| Máximo | 782.55 ms |

## Análise de SLA e Escabilidade
Abaixo está o detalhamento da capacidade sob diferentes níveis de tolerância de tempo de resposta:

| SLA Alvo | Tempo Limite | Requisições Atendidas | % de Sucesso no SLA | RPS no SLA | Status |
|---|---|---|---|---|---|
| **SLA <= 1s** | 1000 ms | 2505 / 2510 | 100.00% | 41.44 req/s | ✅ Aprovado |
| **SLA <= 2s** | 2000 ms | 2505 / 2510 | 100.00% | 41.44 req/s | ✅ Aprovado |
| **SLA <= 3s** | 3000 ms | 2505 / 2510 | 100.00% | 41.44 req/s | ✅ Aprovado |

---
*Relatório de capacidade gerado automaticamente via k6 em 2026-07-04T20:34:48.388Z*
