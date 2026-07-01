import http from 'k6/http';
import { check, sleep, group } from 'k6';
import crypto from 'k6/crypto';
import { Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de Estresse Profissional e Spike (k6) — Projeto EQ03
// ─────────────────────────────────────────────────────────────────────────────

// Configuração de URL base (sem padrão fixo de porta para evitar conflitos)
const BASE_URL = __ENV.BASE_URL || 'http://localhost';

// Carrega variáveis confidenciais estritamente do ambiente
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@salao.com';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;
const WEBHOOK_SECRET = __ENV.MERCADO_PAGO_WEBHOOK_SECRET;

// Métricas customizadas para análise refinada de SLA (Tempo de resposta limite)
const rateUnder1s = new Rate('rate_under_1s');
const rateUnder2s = new Rate('rate_under_2s');
const rateUnder3s = new Rate('rate_under_3s');

export const options = {
  // Cenário de Carga Extrema (Spike/Pico Concorrente)
  stages: [
    { duration: '10s', target: 100 },  // Rampa de subida rápida até 100 VUs
    { duration: '10s', target: 100 },  // Mantém estável em 100 VUs
    { duration: '0s', target: 500 },  // Pico instantâneo de concorrência para 500 VUs
    { duration: '15s', target: 500 },  // Segura o pico de 500 VUs
    { duration: '10s', target: 0 },    // Rampa de descida até desaquecer
  ],

  // Limites gerais e metas de erro/duração sob carga
  thresholds: {
    http_req_failed: ['rate<0.05'], // Permitir no máximo 5% de falhas no estresse máximo
    http_req_duration: ['p(95)<3000'], // O tempo p(95) idealmente deve estar abaixo de 3s
  },
};

// Auxiliar para registrar o tempo de resposta nas métricas de SLA
function trackResponseSla(res) {
  if (res && res.timings && res.timings.duration) {
    const duration = res.timings.duration;
    rateUnder1s.add(duration < 1000);
    rateUnder2s.add(duration < 2000);
    rateUnder3s.add(duration < 3000);
  }
}

// Executado uma única vez no início do teste
export function setup() {
  console.log(`Iniciando Setup do Teste de Estresse contra ${BASE_URL}...`);

  // Validação preventiva para não rodar sem credenciais informadas no ambiente
  if (!ADMIN_PASSWORD) {
    throw new Error('ERRO: A variável de ambiente ADMIN_PASSWORD é obrigatória para o login! Informe-a rodando com "-e ADMIN_PASSWORD=sua_senha".');
  }

  // 1. Efetua login seguro
  const loginRes = http.post(`${BASE_URL}/v1/auth/login`, JSON.stringify({
    email: ADMIN_EMAIL,
    password: ADMIN_PASSWORD
  }), { headers: { 'Content-Type': 'application/json' } });

  const token = loginRes.json('accessToken');
  if (!token) {
    throw new Error(`ERRO: Não foi possível obter o token de acesso para o usuário ${ADMIN_EMAIL}. Verifique as credenciais.`);
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // 2. Garante a presença de um Serviço para o teste de criação
  let serviceId = 1;
  const svcRes = http.get(`${BASE_URL}/v1/services`, { headers });
  if (svcRes.status === 200 && svcRes.json().length > 0) {
    serviceId = svcRes.json()[0].id;
  } else {
    const newSvcRes = http.post(`${BASE_URL}/v1/services`, JSON.stringify({
      name: 'Serviço de Teste Estresse k6',
      description: 'Criado dinamicamente para o teste de performance',
      price: 50.00,
      durationMin: 30,
      active: true
    }), { headers });
    if (newSvcRes.status === 201) {
      serviceId = newSvcRes.json().id;
    } else {
      console.error(`ERRO ao criar serviço no setup: Código ${newSvcRes.status} | Resposta: ${newSvcRes.body}`);
    }
  }

  // 3. Garante a presença de uma Funcionária para o teste de criação (Usa listagem Admin completa)
  let employeeId = 1;
  const empRes = http.get(`${BASE_URL}/v1/employees`, { headers });
  if (empRes.status === 200 && empRes.json().length > 0) {
    employeeId = empRes.json()[0].id;
  } else {
    // Registra usuário diretamente com perfil FUNCIONARIA (roleId = 3) via Admin
    const randomEmail = `k6_stress_staff_${Math.floor(Math.random() * 1000000)}@teste.com`;
    const regRes = http.post(`${BASE_URL}/v1/users`, JSON.stringify({
      name: 'Funcionária k6 Stress',
      email: randomEmail,
      password: 'EmployeePassword@123',
      phone: '83999999999',
      active: true,
      roleId: 3 // FUNCIONARIA
    }), { headers });

    if (regRes.status === 200 || regRes.status === 201) {
      const userId = regRes.json().id;
      const newEmpRes = http.post(`${BASE_URL}/v1/employees`, JSON.stringify({
        userId: userId,
        bio: 'Funcionária criada temporariamente pelo setup de estresse',
        remunerationType: 'SALARIO_FIXO',
        commissionScope: 'GLOBAL',
        remunerationValue: 1500.00,
        commissionValue: 5.00
      }), { headers });
      if (newEmpRes.status === 201) {
        employeeId = newEmpRes.json().id;
      } else {
        console.error(`ERRO ao criar funcionária no setup: Código ${newEmpRes.status} | Resposta: ${newEmpRes.body}`);
      }
    } else {
      console.error(`ERRO ao criar usuário para funcionária no setup: Código ${regRes.status} | Resposta: ${regRes.body}`);
    }
  }

  console.log(`Setup concluído. Usando Service ID: ${serviceId}, Employee ID: ${employeeId}`);
  return { token, employeeId, serviceId };
}

// Loop principal executado concorrentemente pelas VUs
export default function (data) {
  const { token, employeeId, serviceId } = data;
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  const actionSelector = Math.random();

  if (actionSelector < 0.30) {
    // ROTA LEITURA 1: Relatório financeiro (agregação e queries pesadas no banco)
    group('GET Financial Report', () => {
      const res = http.get(`${BASE_URL}/v1/reports/financial?from=2026-06-01&to=2026-06-30`, { headers });
      trackResponseSla(res);
      check(res, {
        'status 200 - Financial Report': (r) => r.status === 200,
      });
    });

  } else if (actionSelector < 0.60) {
    // ROTA LEITURA 2: Listagem geral de agendamentos
    group('GET All Appointments', () => {
      const res = http.get(`${BASE_URL}/v1/appointments`, { headers });
      trackResponseSla(res);
      check(res, {
        'status 200 - List Appointments': (r) => r.status === 200,
      });
    });

  } else if (actionSelector < 0.75) {
    // ROTA ESCRITA 1: Criar novo pedido de agendamento (Escrita no PostgreSQL)
    // scheduledAt nulo envia como solicitação, evitando erros de choque de horário e focando no stress de gravação
    group('POST Create Appointment Request', () => {
      const payload = JSON.stringify({
        employeeId: employeeId,
        serviceId: serviceId,
        preferredDate: '2026-07-20',
        clientNotes: `Estresse de concorrência concorrente VU ${__VU} - Iteração ${__ITER}`
      });
      const res = http.post(`${BASE_URL}/v1/appointments`, payload, { headers });
      trackResponseSla(res);
      check(res, {
        'status 201 - Create Appointment': (r) => r.status === 201,
      });
    });

  } else if (actionSelector < 0.90) {
    // ROTA ESCRITA 2: Criação manual de entrada no Fluxo de Caixa (Lançamento financeiro)
    group('POST Manual CashFlow Entry', () => {
      const payload = JSON.stringify({
        type: Math.random() < 0.5 ? 'INCOME' : 'EXPENSE',
        amount: parseFloat((Math.random() * 150).toFixed(2)),
        description: `Lançamento de estresse VU ${__VU} - Iteração ${__ITER}`,
        date: '2026-07-01'
      });
      const res = http.post(`${BASE_URL}/v1/cashflow`, payload, { headers });
      trackResponseSla(res);
      check(res, {
        'status 201 - CashFlow Entry Created': (r) => r.status === 201,
      });
    });

  } else {
    // ROTA MISTA/WEBHOOK: Simula o webhook de pagamento do Mercado Pago
    // Só é executado se a variável WEBHOOK_SECRET for passada no ambiente (evitando furos de segredo)
    if (!WEBHOOK_SECRET) {
      // Fallback para listagem de agendamentos para não desperdiçar iteração
      const res = http.get(`${BASE_URL}/v1/appointments`, { headers });
      trackResponseSla(res);
      check(res, { 'status 200 - List Appointments (Fallback)': (r) => r.status === 200 });
      return;
    }

    group('POST Webhook Mercado Pago', () => {
      const dataId = String(Math.floor(1000000000 + Math.random() * 9000000000));
      const requestId = `req_${Math.floor(Math.random() * 1000000)}`;
      const ts = String(Math.floor(Date.now() / 1000));

      const manifest = `id:${dataId};request-id:${requestId};ts:${ts};`;
      const v1 = crypto.hmac('sha256', WEBHOOK_SECRET, manifest, 'hex');
      const signature = `ts=${ts},v1=${v1}`;

      const payload = JSON.stringify({
        type: 'payment',
        data: {
          id: dataId
        }
      });

      const params = {
        headers: {
          'Content-Type': 'application/json',
          'x-signature': signature,
          'x-request-id': requestId
        }
      };

      const res = http.post(`${BASE_URL}/v1/webhooks/mercadopago?data.id=${dataId}`, payload, params);
      trackResponseSla(res);
      check(res, {
        'status 200 - Webhook Received': (r) => r.status === 200,
      });
    });
  }

  sleep(0.1);
}

// Hook do k6 executado ao final do teste para gerar os relatórios e estatísticas
export function handleSummary(data) {
  const reqDuration = data.metrics.http_req_duration ? data.metrics.http_req_duration.values : null;
  const reqFailed = data.metrics.http_req_failed ? data.metrics.http_req_failed.values : null;
  const totalReqs = data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0;
  const rps = data.metrics.http_reqs ? data.metrics.http_reqs.values.rate : 0;

  const min = (reqDuration && reqDuration.min) ? reqDuration.min.toFixed(2) : '0.00';
  const avg = (reqDuration && reqDuration.avg) ? reqDuration.avg.toFixed(2) : '0.00';
  const med = (reqDuration && reqDuration.med) ? reqDuration.med.toFixed(2) : '0.00';
  const p95 = (reqDuration && reqDuration['p(95)']) ? reqDuration['p(95)'].toFixed(2) : '0.00';
  const p99 = (reqDuration && reqDuration['p(99)']) ? reqDuration['p(99)'].toFixed(2) : '0.00';
  const max = (reqDuration && reqDuration.max) ? reqDuration.max.toFixed(2) : '0.00';

  const failRate = (reqFailed && reqFailed.rate !== undefined) ? (reqFailed.rate * 100).toFixed(2) : '0.00';

  const rate1s = data.metrics.rate_under_1s ? (data.metrics.rate_under_1s.values.rate * 100).toFixed(2) : '0.00';
  const rate2s = data.metrics.rate_under_2s ? (data.metrics.rate_under_2s.values.rate * 100).toFixed(2) : '0.00';
  const rate3s = data.metrics.rate_under_3s ? (data.metrics.rate_under_3s.values.rate * 100).toFixed(2) : '0.00';

  const count1s = data.metrics.rate_under_1s ? data.metrics.rate_under_1s.values.passes : 0;
  const count2s = data.metrics.rate_under_2s ? data.metrics.rate_under_2s.values.passes : 0;
  const count3s = data.metrics.rate_under_3s ? data.metrics.rate_under_3s.values.passes : 0;

  const rps1s = totalReqs > 0 ? (rps * (count1s / totalReqs)).toFixed(2) : '0.00';
  const rps2s = totalReqs > 0 ? (rps * (count2s / totalReqs)).toFixed(2) : '0.00';
  const rps3s = totalReqs > 0 ? (rps * (count3s / totalReqs)).toFixed(2) : '0.00';

  const reportContent = `# Relatório de Performance e Teste de Estresse (SLA Analysis)
  
## Resumo de Carga (Simultânea e Concorrente)
Este teste executa todas as rotas (Leituras de Relatórios, Listagens, Criações e Webhooks) **simultaneamente** de forma paralela concorrente, simulando o uso real em ambiente de pico.

* **Total de Requisições:** ${totalReqs}
* **Requisições por Segundo (RPS Total):** ${rps.toFixed(2)} req/s
* **Taxa de Falhas (Erros de Rede/Banco):** ${failRate}%

## Tempos de Resposta (Duração das Requisições)
| Métrica | Tempo (ms) |
|---|---|
| Mínimo | ${min} ms |
| Médio | ${avg} ms |
| Mediana | ${med} ms |
| p(95) - 95% das requisições | ${p95} ms |
| p(99) - 99% das requisições | ${p99} ms |
| Máximo | ${max} ms |

## Análise de Vazão e SLAs de Performance (Quanto o sistema aguenta)
Abaixo estão as métricas de capacidade real do sistema para cada patamar de tempo limite aceitável:

| SLA Alvo | Tempo Limite | Requisições Atendidas no SLA | % de Sucesso | RPS Efetivo (Vazão no SLA) | Status (Meta: > 95%) |
|---|---|---|---|---|---|
| **SLA <= 1s** | 1000 ms | ${count1s} / ${totalReqs} | ${rate1s}% | ${rps1s} req/s | ${parseFloat(rate1s) >= 95 ? '✅ Aprovado' : '⚠️ Alerta'} |
| **SLA <= 2s** | 2000 ms | ${count2s} / ${totalReqs} | ${rate2s}% | ${rps2s} req/s | ${parseFloat(rate2s) >= 95 ? '✅ Aprovado' : '⚠️ Alerta'} |
| **SLA <= 3s** | 3000 ms | ${count3s} / ${totalReqs} | ${rate3s}% | ${rps3s} req/s | ${parseFloat(rate3s) >= 95 ? '✅ Aprovado' : '⚠️ Alerta'} |

---
*Relatório gerado automaticamente via k6 custom summary em ${new Date().toISOString()}*
`;

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }) + '\n\n' + reportContent,
    'loadtest/report.md': reportContent,
    'loadtest/resultado.json': JSON.stringify(data, null, 2),
  };
}
