# Relatório de Carga — Fase Soak / Confirmação (k6)

## Estratégia
Sustenta um RPS fixo (SOAK_RPS=100) por 180 s inteiros,
para confirmar que um teto encontrado na escada é capacidade real —
não sorte de alguns segundos. Cada requisição é tagueada pela metade da
janela em que ocorreu (primeira/segunda), para detectar se a latência
degrada com o tempo (fila crescendo) mesmo com RPS constante.

## Veredito
✅ **CONFIRMADO**: 100 req/s é sustentável. Ao longo de 180 s (3.0 min), o sistema processou **19744 requisições com sucesso (100%)**, p(95) geral de 678 ms.

## Estabilidade ao longo da janela
p(95) primeira metade: 1125 ms · p(95) segunda metade: 170 ms · razão: 0.15x (informativo — só reprova o soak se a segunda metade sozinha já ultrapassar o SLA)

## Detalhamento

| Métrica | Valor |
|---|---|
| RPS alvo | 100 req/s |
| RPS real | 109.7 req/s |
| Duração | 180 s (3.0 min) |
| Requisições totais | 19744 |
| Requisições OK | 19744 |
| Taxa de erro HTTP | 0.00% |
| p(95) geral | 678 ms |
| p(99) geral | 1700 ms |

---
*Gerado em 2026-07-18T02:06:42.116Z*
