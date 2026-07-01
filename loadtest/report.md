# Relatório de Performance e Teste de Estresse (SLA Analysis)
  
## Resumo de Carga (Simultânea e Concorrente)
Este teste executa todas as rotas (Leituras de Relatórios, Listagens, Criações e Webhooks) **simultaneamente** de forma paralela concorrente, simulando o uso real em ambiente de pico.

* **Total de Requisições:** 5063
* **Requisições por Segundo (RPS Total):** 111.97 req/s
* **Taxa de Falhas (Erros de Rede/Banco):** 0.00%

## Tempos de Resposta (Duração das Requisições)
| Métrica | Tempo (ms) |
|---|---|
| Mínimo | 5.62 ms |
| Médio | 2324.72 ms |
| Mediana | 1370.85 ms |
| p(95) - 95% das requisições | 5885.90 ms |
| p(99) - 99% das requisições | 0.00 ms |
| Máximo | 9515.71 ms |

## Análise de Vazão e SLAs de Performance (Quanto o sistema aguenta)
Abaixo estão as métricas de capacidade real do sistema para cada patamar de tempo limite aceitável:

| SLA Alvo | Tempo Limite | Requisições Atendidas no SLA | % de Sucesso | RPS Efetivo (Vazão no SLA) | Status (Meta: > 95%) |
|---|---|---|---|---|---|
| **SLA <= 1s** | 1000 ms | 2307 / 5063 | 45.59% | 51.02 req/s | ⚠️ Alerta |
| **SLA <= 2s** | 2000 ms | 2718 / 5063 | 53.72% | 60.11 req/s | ⚠️ Alerta |
| **SLA <= 3s** | 3000 ms | 3052 / 5063 | 60.32% | 67.49 req/s | ⚠️ Alerta |

---
*Relatório gerado automaticamente via k6 custom summary em 2026-07-01T13:03:30.086Z*
