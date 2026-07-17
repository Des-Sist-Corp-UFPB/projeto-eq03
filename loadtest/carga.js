import http from 'k6/http';
import { check, group } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de Carga e Performance — EQ03
//
// Estratégia: ramping-arrival-rate — o k6 controla diretamente o RPS (requisições
// por segundo), não o número de VUs. Cada cenário aumenta o RPS até o alvo e o
// threshold p(95)<Xs revela se o sistema aguenta aquela taxa dentro do budget.
//
// Isso responde objetivamente: "qual é o máximo de RPS que o sistema entrega
// com 95% das respostas dentro de 1s / 2s / 3s?"
//
// Três cenários independentes e sequenciais:
//   sla_1s — sobe até MAX_RPS_1S req/s; passa se p(95) < 1 000 ms
//   sla_2s — sobe até MAX_RPS_2S req/s; passa se p(95) < 2 000 ms
//   sla_3s — sobe até MAX_RPS_3S req/s; passa se p(95) < 3 000 ms
//
// Rotas: GET /ping, relatórios financeiro/agendamentos/folha/por-profissional,
//        agendamentos, cashflow, usuários, clientes, employees/booking, produtos,
//        serviços + escritas: POST /appointments, POST/DELETE cashflow,
//        POST+PATCH cancel, PUT products, PATCH users.
//
// Comando local:
//   docker run --rm -i --network projeto-eq03_salon-network \
//     -v "${PWD}:/app" -w /app --env-file .env \
//     -e BASE_URL=http://salon-app:8080 \
//     grafana/k6 run loadtest/carga.js
// ─────────────────────────────────────────────────────────────────────────────

const BASE_URL       = __ENV.BASE_URL       || 'http://localhost:8080';
const ADMIN_EMAIL    = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

// RPS máximo que cada cenário tenta atingir na rampa.
// Se o sistema aguentar com p(95) dentro do budget → threshold passa.
// Se o sistema saturar antes → threshold falha e o relatório mostra onde quebrou.
const MAX_RPS_1S = parseInt(__ENV.MAX_RPS_1S) || 60;
const MAX_RPS_2S = parseInt(__ENV.MAX_RPS_2S) || 100;
const MAX_RPS_3S = parseInt(__ENV.MAX_RPS_3S) || 150;

// Duração real de cada cenário: 90s ramp + 30s sustentado = 120s
const SCENARIO_DURATION_S = 120;

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],

  scenarios: {
    // Cenário 1: descobre o máximo de RPS com p(95) ≤ 1s
    sla_1s: {
      executor:        'ramping-arrival-rate',
      startRate:       1,
      timeUnit:        '1s',
      preAllocatedVUs: 50,
      maxVUs:          300,
      stages: [
        { duration: '90s', target: MAX_RPS_1S },
        { duration: '30s', target: MAX_RPS_1S },
      ],
      startTime: '0s',
    },
    // Cenário 2: descobre o máximo de RPS com p(95) ≤ 2s
    sla_2s: {
      executor:        'ramping-arrival-rate',
      startRate:       1,
      timeUnit:        '1s',
      preAllocatedVUs: 100,
      maxVUs:          500,
      stages: [
        { duration: '90s', target: MAX_RPS_2S },
        { duration: '30s', target: MAX_RPS_2S },
      ],
      startTime: '2m10s',
    },
    // Cenário 3: descobre o máximo de RPS com p(95) ≤ 3s
    sla_3s: {
      executor:        'ramping-arrival-rate',
      startRate:       1,
      timeUnit:        '1s',
      preAllocatedVUs: 150,
      maxVUs:          700,
      stages: [
        { duration: '90s', target: MAX_RPS_3S },
        { duration: '30s', target: MAX_RPS_3S },
      ],
      startTime: '4m20s',
    },
  },

  thresholds: {
    // Taxa de erro global
    http_req_failed: ['rate<0.01'],

    // Estes são os thresholds reais de SLA — passam ou falham conforme a carga
    'http_req_duration{scenario:sla_1s}': ['p(95)<1000'],
    'http_req_duration{scenario:sla_2s}': ['p(95)<2000'],
    'http_req_duration{scenario:sla_3s}': ['p(95)<3000'],

    // Garante submetrics no relatório
    'http_reqs{scenario:sla_1s}': ['count>0'],
    'http_reqs{scenario:sla_2s}': ['count>0'],
    'http_reqs{scenario:sla_3s}': ['count>0'],
  },
};

export function setup() {
  console.log('Setup contra ' + BASE_URL);

  if (!ADMIN_EMAIL || !ADMIN_PASSWORD) {
    throw new Error('ERRO: ADMIN_EMAIL e ADMIN_PASSWORD são obrigatórios no .env');
  }

  const loginRes = http.post(
    BASE_URL + '/v1/auth/login',
    JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  const token = loginRes.json('accessToken');
  if (!token) throw new Error('ERRO: login falhou — verifique as credenciais no .env');

  const h = { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token };

  // serviceId — endpoint paginado: {content: [...], ...}
  let serviceId = 1;
  const svcRes = http.get(BASE_URL + '/v1/services', { headers: h });
  if (svcRes.status === 200) {
    const resp = svcRes.json();
    const list = Array.isArray(resp) ? resp : (resp.content || []);
    if (list.length > 0) {
      serviceId = list[0].id;
    } else {
      const r = http.post(BASE_URL + '/v1/services', JSON.stringify({
        name: 'Serviço k6', description: 'Criado pelo setup de carga',
        price: 100.00, durationMin: 45, active: true,
      }), { headers: h });
      if (r.status === 201) serviceId = r.json('id');
    }
  }

  // employeeId — endpoint paginado
  let employeeId = 1;
  const empRes = http.get(BASE_URL + '/v1/employees', { headers: h });
  if (empRes.status === 200) {
    const resp = empRes.json();
    const list = Array.isArray(resp) ? resp : (resp.content || []);
    if (list.length > 0) {
      employeeId = list[0].id;
    } else {
      const reg = http.post(BASE_URL + '/v1/users', JSON.stringify({
        name: 'Funcionária k6', email: 'k6_emp_' + Date.now() + '@teste.com',
        password: 'Employee@123', phone: '83911110000', active: true, roleId: 3,
      }), { headers: h });
      if (reg.status === 200 || reg.status === 201) {
        const r = http.post(BASE_URL + '/v1/employees', JSON.stringify({
          userId: reg.json('id'), bio: 'k6 load test', remunerationType: 'SALARIO_FIXO',
          commissionScope: 'GLOBAL', remunerationValue: 2000, commissionValue: 10,
        }), { headers: h });
        if (r.status === 201) employeeId = r.json('id');
      }
    }
  }

  // productId — endpoint paginado
  let productId = 1;
  const prodRes = http.get(BASE_URL + '/v1/products', { headers: h });
  if (prodRes.status === 200) {
    const resp = prodRes.json();
    const list = Array.isArray(resp) ? resp : (resp.content || []);
    if (list.length > 0) {
      productId = list[0].id;
    } else {
      const r = http.post(BASE_URL + '/v1/products', JSON.stringify({
        name: 'Produto k6', stock: 9999, price: 25.00, active: true,
      }), { headers: h });
      if (r.status === 201) productId = r.json('id');
    }
  }

  // testUserId — usuário separado para PATCH /v1/users/{id}
  let testUserId = null;
  const regU = http.post(BASE_URL + '/v1/users', JSON.stringify({
    name: 'User k6 Update', email: 'k6_upd_' + Date.now() + '@teste.com',
    password: 'UpdateUser@123', phone: '83922220000', active: true, roleId: 2,
  }), { headers: h });
  if (regU.status === 200 || regU.status === 201) testUserId = regU.json('id');

  console.log('Setup OK — serviceId=' + serviceId + ' employeeId=' + employeeId +
              ' productId=' + productId + ' testUserId=' + testUserId);
  return { token, serviceId, employeeId, productId, testUserId };
}

export default function (data) {
  const h = {
    'Content-Type':  'application/json',
    'Authorization': 'Bearer ' + data.token,
  };

  const r = Math.random();

  if (r < 0.05) {
    // ── GET /ping ─────────────────────────────────────────────────────────
    group('GET Ping', () => {
      const res = http.get(BASE_URL + '/ping', { headers: h });
      check(res, { 'status 200 - Ping': (v) => v.status === 200 });
    });

  } else if (r < 0.13) {
    // ── GET /v1/reports/financial ─────────────────────────────────────────
    group('GET Financial Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/financial?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Financial Report': (v) => v.status === 200 });
    });

  } else if (r < 0.20) {
    // ── GET /v1/reports/appointments ──────────────────────────────────────
    group('GET Appointments Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/appointments?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Appointments Report': (v) => v.status === 200 });
    });

  } else if (r < 0.24) {
    // ── GET /v1/reports/payroll ───────────────────────────────────────────
    group('GET Payroll Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/payroll?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Payroll Report': (v) => v.status === 200 });
    });

  } else if (r < 0.27) {
    // ── GET /v1/reports/financial/employees/{id} ──────────────────────────
    group('GET Financial Report by Employee', () => {
      const res = http.get(BASE_URL + '/v1/reports/financial/employees/' + data.employeeId + '?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Financial Report by Employee': (v) => v.status === 200 });
    });

  } else if (r < 0.34) {
    // ── GET /v1/appointments ──────────────────────────────────────────────
    group('GET All Appointments', () => {
      const res = http.get(BASE_URL + '/v1/appointments', { headers: h });
      check(res, { 'status 200 - List Appointments': (v) => v.status === 200 });
    });

  } else if (r < 0.41) {
    // ── GET /v1/cashflow ──────────────────────────────────────────────────
    group('GET CashFlow', () => {
      const res = http.get(BASE_URL + '/v1/cashflow', { headers: h });
      check(res, { 'status 200 - CashFlow': (v) => v.status === 200 });
    });

  } else if (r < 0.45) {
    // ── GET /v1/users ─────────────────────────────────────────────────────
    group('GET Users', () => {
      const res = http.get(BASE_URL + '/v1/users', { headers: h });
      check(res, { 'status 200 - Users': (v) => v.status === 200 });
    });

  } else if (r < 0.49) {
    // ── GET /v1/clients ───────────────────────────────────────────────────
    group('GET Clients', () => {
      const res = http.get(BASE_URL + '/v1/clients', { headers: h });
      check(res, { 'status 200 - Clients': (v) => v.status === 200 });
    });

  } else if (r < 0.55) {
    // ── GET /v1/employees/booking ─────────────────────────────────────────
    group('GET Employees Booking', () => {
      const res = http.get(BASE_URL + '/v1/employees/booking', { headers: h });
      check(res, { 'status 200 - Employees Booking': (v) => v.status === 200 });
    });

  } else if (r < 0.60) {
    // ── GET /v1/products ──────────────────────────────────────────────────
    group('GET Products', () => {
      const res = http.get(BASE_URL + '/v1/products', { headers: h });
      check(res, { 'status 200 - Products': (v) => v.status === 200 });
    });

  } else if (r < 0.65) {
    // ── GET /v1/services ──────────────────────────────────────────────────
    group('GET Services', () => {
      const res = http.get(BASE_URL + '/v1/services', { headers: h });
      check(res, { 'status 200 - Services': (v) => v.status === 200 });
    });

  } else if (r < 0.73) {
    // ── POST /v1/appointments ─────────────────────────────────────────────
    group('POST Create Appointment', () => {
      const res = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2026-10-15',
        clientNotes:   'k6 iter',
      }), { headers: h });
      check(res, { 'status 201 - Create Appointment': (v) => v.status === 201 });
    });

  } else if (r < 0.81) {
    // ── POST /v1/cashflow ─────────────────────────────────────────────────
    group('POST CashFlow Entry', () => {
      const res = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type:        Math.random() < 0.5 ? 'INCOME' : 'EXPENSE',
        amount:      parseFloat((Math.random() * 100 + 1).toFixed(2)),
        description: 'k6 entry',
        date:        '2026-07-01',
      }), { headers: h });
      check(res, { 'status 201 - CashFlow Entry': (v) => v.status === 201 });
    });

  } else if (r < 0.86) {
    // ── POST + DELETE /v1/cashflow/{id} ───────────────────────────────────
    group('POST+DELETE CashFlow', () => {
      const cr = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type: 'EXPENSE', amount: 1.00, description: 'k6 del', date: '2026-07-01',
      }), { headers: h });
      if (cr.status === 201) {
        http.del(BASE_URL + '/v1/cashflow/' + cr.json('id'), null, { headers: h });
      }
    });

  } else if (r < 0.91) {
    // ── POST + PATCH /v1/appointments/{id}/cancel ─────────────────────────
    group('POST+PATCH Cancel Appointment', () => {
      const cr = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2026-10-20',
        clientNotes:   'k6 cancel',
      }), { headers: h });
      if (cr.status === 201) {
        http.patch(BASE_URL + '/v1/appointments/' + cr.json('id') + '/cancel', null, { headers: h });
      }
    });

  } else if (r < 0.95) {
    // ── PUT /v1/products/{id} ─────────────────────────────────────────────
    group('PUT Update Product', () => {
      const res = http.put(BASE_URL + '/v1/products/' + data.productId, JSON.stringify({
        name:  'Produto k6',
        stock: 9999,
        price: parseFloat((Math.random() * 50 + 10).toFixed(2)),
        active: true,
      }), { headers: h });
      check(res, { 'status 200 - Update Product': (v) => v.status === 200 });
    });

  } else {
    // ── PATCH /v1/users/{id} ──────────────────────────────────────────────
    group('PATCH Update User', () => {
      if (!data.testUserId) return;
      const res = http.patch(BASE_URL + '/v1/users/' + data.testUserId, JSON.stringify({
        phone: '839' + String(Math.floor(Math.random() * 90000000 + 10000000)),
      }), { headers: h });
      check(res, { 'status 200 - Update User': (v) => v.status === 200 });
    });
  }
}

export function handleSummary(data) {
  const mv  = (key) => (data.metrics[key] ? data.metrics[key].values : null);
  const fmt = (n)   => (n == null ? 'N/A' : String(Math.round(n)));

  function scenarioDur(scenario) {
    const d = mv('http_req_duration{scenario:' + scenario + '}');
    if (!d) return { p95: 'N/A', p99: 'N/A', avg: 'N/A', max: 'N/A' };
    return {
      p95: d['p(95)'] != null ? d['p(95)'].toFixed(0) : 'N/A',
      p99: d['p(99)'] != null ? d['p(99)'].toFixed(0) : 'N/A',
      avg: d.avg      != null ? d.avg.toFixed(0)      : 'N/A',
      max: d.max      != null ? d.max.toFixed(0)      : 'N/A',
    };
  }

  function scenarioRps(scenario) {
    const r = mv('http_reqs{scenario:' + scenario + '}');
    return r ? (r.count / SCENARIO_DURATION_S).toFixed(1) : '0.0';
  }

  function slaResult(scenario, budgetMs) {
    const d = mv('http_req_duration{scenario:' + scenario + '}');
    if (!d || d['p(95)'] == null) return '❓';
    return d['p(95)'] <= budgetMs ? '✅ PASSOU' : '❌ FALHOU';
  }

  const d1 = scenarioDur('sla_1s');
  const d2 = scenarioDur('sla_2s');
  const d3 = scenarioDur('sla_3s');

  const failRate  = mv('http_req_failed') ? (mv('http_req_failed').rate * 100).toFixed(2) : '0.00';
  const totalReqs = mv('http_reqs')       ? fmt(mv('http_reqs').count) : '0';

  const reportContent =
`# Relatório de Carga e Performance — EQ03 (k6)

## Estratégia
O teste usa **ramping-arrival-rate**: o k6 controla diretamente o RPS
(requisições por segundo) e descobre quantos VUs precisa. O threshold
\`p(95) < Xs\` responde objetivamente se o sistema sustenta aquela taxa
dentro do budget de tempo.

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | ramping-arrival-rate (1 → alvo RPS em 90s + 30s sustentado) |
| Duração por cenário | 120 s |
| Alvo sla_1s | ${MAX_RPS_1S} req/s |
| Alvo sla_2s | ${MAX_RPS_2S} req/s |
| Alvo sla_3s | ${MAX_RPS_3S} req/s |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /reports/financial/employees/{id} · GET /appointments · GET /cashflow · GET /users · GET /clients · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Resultado por Budget de Tempo

| Budget | RPS médio | p(95) | p(99) | Máx | SLA |
|--------|-----------|-------|-------|-----|-----|
| **≤ 1 s** | ${scenarioRps('sla_1s')} req/s | **${d1.p95} ms** | ${d1.p99} ms | ${d1.max} ms | ${slaResult('sla_1s', 1000)} |
| **≤ 2 s** | ${scenarioRps('sla_2s')} req/s | **${d2.p95} ms** | ${d2.p99} ms | ${d2.max} ms | ${slaResult('sla_2s', 2000)} |
| **≤ 3 s** | ${scenarioRps('sla_3s')} req/s | **${d3.p95} ms** | ${d3.p99} ms | ${d3.max} ms | ${slaResult('sla_3s', 3000)} |

## Saúde Global
* **Total de requisições (todos os cenários):** ${totalReqs}
* **Taxa de falha HTTP:** ${failRate}% (máximo permitido: 1,00%)

## Interpretação
- **SLA PASSOU**: o sistema sustenta o RPS alvo com 95% das respostas dentro do budget.
- **SLA FALHOU**: o sistema satura antes de atingir o RPS alvo para esse budget;
  o máximo real está em algum ponto durante a rampa onde p(95) ainda estava abaixo do limite.

---
*Relatório gerado automaticamente via k6 em ${new Date().toISOString()}*
`;

  const safeData = JSON.parse(JSON.stringify(data));
  if (safeData.setup_data) safeData.setup_data.token = '[REDACTED]';

  return {
    'stdout':                  textSummary(data, { indent: ' ', enableColors: true }) + '\n\n' + reportContent,
    'loadtest/report.md':      reportContent,
    'loadtest/resultado.json': JSON.stringify(safeData, null, 2),
  };
}
