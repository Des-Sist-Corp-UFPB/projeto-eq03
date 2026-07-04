import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de Carga Constante — Busca Binária Manual da Capacidade Máxima — EQ03
// Uso: k6 run -e VUS=50 -e BASE_URL=... -e ADMIN_EMAIL=... -e ADMIN_PASSWORD=...
// ─────────────────────────────────────────────────────────────────────────────

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

// Métricas de SLA customizadas para encontrar o ponto de quebra
const rateUnder1s = new Rate('rate_under_1s');
const rateUnder2s = new Rate('rate_under_2s');
const rateUnder3s = new Rate('rate_under_3s');

export const options = {
  // Carga Constante — parâmetro via variável de ambiente (busca binária manual)
  // Exemplo de uso para testar 50 VUs: k6 run -e VUS=50 carga.js
  vus: parseInt(__ENV.VUS || '10', 10),
  duration: '1m',

  // Limite estrito de estabilidade: falha se houver mais de 1% de requisições mal-sucedidas
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

// Mede e classifica os tempos de resposta para a análise de SLA
function trackResponseSla(res) {
  if (res && res.timings && res.timings.duration) {
    const duration = res.timings.duration;
    rateUnder1s.add(duration < 1000);
    rateUnder2s.add(duration < 2000);
    rateUnder3s.add(duration < 3000);
  }
}

// Inicializa a autenticação e prepara recursos de teste
export function setup() {
  console.log(`Iniciando Setup do Teste de Capacidade contra ${BASE_URL}...`);

  if (!ADMIN_EMAIL || !ADMIN_PASSWORD) {
    throw new Error('ERRO: As variáveis de ambiente ADMIN_EMAIL e ADMIN_PASSWORD são obrigatórias!');
  }

  // 1. Efetua login
  const loginRes = http.post(`${BASE_URL}/v1/auth/login`, JSON.stringify({
    email: ADMIN_EMAIL,
    password: ADMIN_PASSWORD
  }), { headers: { 'Content-Type': 'application/json' } });

  const token = loginRes.json('accessToken');
  if (!token) {
    throw new Error(`ERRO: Não foi possível obter o token de acesso para ${ADMIN_EMAIL}. Verifique as credenciais.`);
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // 2. Garante a existência de um Serviço
  let serviceId = 1;
  const svcRes = http.get(`${BASE_URL}/v1/services`, { headers });
  if (svcRes.status === 200 && svcRes.json().length > 0) {
    serviceId = svcRes.json()[0].id;
  } else {
    const newSvcRes = http.post(`${BASE_URL}/v1/services`, JSON.stringify({
      name: 'Serviço de Teste Capacidade k6',
      description: 'Criado para o teste de capacidade',
      price: 100.00,
      durationMin: 45,
      active: true
    }), { headers });
    if (newSvcRes.status === 201) {
      serviceId = newSvcRes.json().id;
    }
  }

  // 3. Garante a existência de uma Funcionária
  let employeeId = 1;
  const empRes = http.get(`${BASE_URL}/v1/employees`, { headers });
  if (empRes.status === 200 && empRes.json().length > 0) {
    employeeId = empRes.json()[0].id;
  } else {
    const randomEmail = `k6_capacity_staff_${Math.floor(Math.random() * 1000000)}@teste.com`;
    const regRes = http.post(`${BASE_URL}/v1/users`, JSON.stringify({
      name: 'Funcionária k6 Capacity',
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
        bio: 'Funcionária criada pelo setup de capacidade',
        remunerationType: 'SALARIO_FIXO',
        commissionScope: 'GLOBAL',
        remunerationValue: 2000.00,
        commissionValue: 10.00
      }), { headers });
      if (newEmpRes.status === 201) {
        employeeId = newEmpRes.json().id;
      }
    }
  }

  console.log(`Setup concluído. Usando Service ID: ${serviceId}, Employee ID: ${employeeId}`);
  return { token, employeeId, serviceId };
}

export default function (data) {
  const { token, employeeId, serviceId } = data;
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  const actionSelector = Math.random();

  if (actionSelector < 0.30) {
    // 1. ROTA LEITURA 1: Relatório financeiro (query pesada e filtros de agregação)
    group('GET Financial Report', () => {
      const res = http.get(`${BASE_URL}/v1/reports/financial?from=2026-06-01&to=2026-06-30`, { headers });
      trackResponseSla(res);

      if (res.status !== 200) {
        console.log(`Erro no Relatório: Status ${res.status} | Body: ${res.body}`);
      }

      check(res, {
        'status 200 - Financial Report': (r) => r.status === 200,
      });
    });

  } else if (actionSelector < 0.60) {
    // 2. ROTA LEITURA 2: Listagem geral de agendamentos
    group('GET All Appointments', () => {
      const res = http.get(`${BASE_URL}/v1/appointments`, { headers });
      trackResponseSla(res);
      check(res, {
        'status 200 - List Appointments': (r) => r.status === 200,
      });
    });

  } else if (actionSelector < 0.80) {
    // 3. ROTA ESCRITA 1: Criar novo pedido de agendamento (Escrita)
    // scheduledAt nulo envia como solicitação, evitando conflitos de horários e assegurando 100% de sucesso
    group('POST Create Appointment Request', () => {
      const payload = JSON.stringify({
        employeeId: employeeId,
        serviceId: serviceId,
        preferredDate: '2026-07-20',
        clientNotes: `Teste de capacidade VU ${__VU} - Iteração ${__ITER}`
      });
      const res = http.post(`${BASE_URL}/v1/appointments`, payload, { headers });
      trackResponseSla(res);
      check(res, {
        'status 201 - Create Appointment': (r) => r.status === 201,
      });
    });

  } else {
    // 4. ROTA ESCRITA 2: Criação manual de entrada no Fluxo de Caixa
    group('POST Manual CashFlow Entry', () => {
      const payload = JSON.stringify({
        type: Math.random() < 0.5 ? 'INCOME' : 'EXPENSE',
        amount: parseFloat((Math.random() * 100 + 1).toFixed(2)),
        description: `Lançamento de capacidade VU ${__VU} - Iteração ${__ITER}`,
        date: '2026-07-01'
      });
      const res = http.post(`${BASE_URL}/v1/cashflow`, payload, { headers });
      trackResponseSla(res);
      check(res, {
        'status 201 - CashFlow Entry Created': (r) => r.status === 201,
      });
    });
  }

  sleep(0.1);
}

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

  const vuCount = parseInt(__ENV.VUS || '10', 10);
  const reportContent = `# Relatório de Carga Constante — Busca Binária da Capacidade Máxima (k6)

## Resumo Executivo — Carga Constante
Este teste executa uma carga constante de **${vuCount} VUs** por **1 minuto** para avaliar se o sistema mantém os SLAs em um nível de concorrência específico. Faz parte de uma **Busca Binária Manual** para determinar a capacidade máxima sustentável. Se todos os SLAs estiverem em 100%, aumente os VUs; se houver quebra, reduza e repita.

* **Total de Requisições:** ${totalReqs}
* **Vazão Média (RPS Total):** ${rps.toFixed(2)} req/s
* **Taxa de Falha:** ${failRate}% (Máximo permitido: 1.00%)

## Distribuição dos Tempos de Resposta
| Métrica | Tempo (ms) |
|---|---|
| Mínimo | ${min} ms |
| Médio | ${avg} ms |
| Mediana | ${med} ms |
| **p(95) (95% das Requisições)** | **${p95} ms** |
| p(99) (99% das Requisições) | ${p99} ms |
| Máximo | ${max} ms |

## Análise de SLA e Escabilidade
Abaixo está o detalhamento da capacidade sob diferentes níveis de tolerância de tempo de resposta:

| SLA Alvo | Tempo Limite | Requisições Atendidas | % de Sucesso no SLA | RPS no SLA | Status |
|---|---|---|---|---|---|
| **SLA <= 1s** | 1000 ms | ${count1s} / ${totalReqs} | ${rate1s}% | ${rps1s} req/s | ${parseFloat(rate1s) >= 95 ? '✅ Aprovado' : '⚠️ Alerta'} |
| **SLA <= 2s** | 2000 ms | ${count2s} / ${totalReqs} | ${rate2s}% | ${rps2s} req/s | ${parseFloat(rate2s) >= 95 ? '✅ Aprovado' : '⚠️ Alerta'} |
| **SLA <= 3s** | 3000 ms | ${count3s} / ${totalReqs} | ${rate3s}% | ${rps3s} req/s | ${parseFloat(rate3s) >= 95 ? '✅ Aprovado' : '⚠️ Alerta'} |

---
*Relatório de capacidade gerado automaticamente via k6 em ${new Date().toISOString()}*
`;

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }) + '\n\n' + reportContent,
    'loadtest/report.md': reportContent,
    'loadtest/resultado.json': JSON.stringify(data, null, 2),
  };
}
