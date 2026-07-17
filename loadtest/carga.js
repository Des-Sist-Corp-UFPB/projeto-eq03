import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de Carga e Performance — EQ03
//
// Três cenários sequenciais e independentes. Cada um começa com 1 VU e sobe
// automaticamente até 60 VUs ao longo de 90s (ramp), depois sustenta por 30s.
// A métrica principal é quantas requisições HTTP 2xx o sistema entregou dentro
// do budget de tempo de cada cenário (≤1s, ≤2s, ≤3s).
//
// Rotas exercitadas: todas as rotas protegidas da API (GET, POST, PUT, PATCH,
// DELETE) — relatórios, agendamentos, caixa, funcionárias, produtos, serviços,
// usuários e ping.
//
// Comando:
//   docker run --rm -i --network projeto-eq03_salon-network \
//     -v "%CD%:/app" -w //app --env-file .env \
//     -e BASE_URL=http://salon-app:8080 \
//     grafana/k6 run loadtest/carga.js
// ─────────────────────────────────────────────────────────────────────────────

const BASE_URL       = __ENV.BASE_URL       || 'http://localhost:8080';
const ADMIN_EMAIL    = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

// Duração real de cada cenário: 90s ramp + 30s sustentado = 120s
const SCENARIO_DURATION_S = 120;

const countOk1s = new Counter('count_ok_1s');
const countOk2s = new Counter('count_ok_2s');
const countOk3s = new Counter('count_ok_3s');

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],

  scenarios: {
    // Cenário 1: budget ≤ 1s
    sla_1s: {
      executor:  'ramping-vus',
      startVUs:  1,
      stages: [
        { duration: '90s', target: 60 },
        { duration: '30s', target: 60 },
      ],
      startTime: '0s',
    },
    // Cenário 2: budget ≤ 2s (começa após sla_1s + 10s de gap)
    sla_2s: {
      executor:  'ramping-vus',
      startVUs:  1,
      stages: [
        { duration: '90s', target: 60 },
        { duration: '30s', target: 60 },
      ],
      startTime: '2m10s',
    },
    // Cenário 3: budget ≤ 3s (começa após sla_2s + 10s de gap)
    sla_3s: {
      executor:  'ramping-vus',
      startVUs:  1,
      stages: [
        { duration: '90s', target: 60 },
        { duration: '30s', target: 60 },
      ],
      startTime: '4m20s',
    },
  },

  thresholds: {
    // Taxa de erro HTTP global
    http_req_failed: ['rate<0.01'],

    // Submetrics por cenário — necessários para p(95)/p(99) no relatório
    // (limite de 60s é apenas para forçar a criação da métrica, nunca dispara)
    'http_req_duration{scenario:sla_1s}': ['p(99)<60000'],
    'http_req_duration{scenario:sla_2s}': ['p(99)<60000'],
    'http_req_duration{scenario:sla_3s}': ['p(99)<60000'],

    // Meta principal: > 1000 requisições HTTP-OK dentro do budget
    count_ok_1s: ['count>1000'],
    count_ok_2s: ['count>1000'],
    count_ok_3s: ['count>1000'],

    // Cria submetrics por cenário (necessário para scenarioRps no handleSummary)
    'http_reqs{scenario:sla_1s}': ['count>0'],
    'http_reqs{scenario:sla_2s}': ['count>0'],
    'http_reqs{scenario:sla_3s}': ['count>0'],
  },
};

// Conta requisições 2xx dentro do budget do cenário atual
function trackResponseSla(res) {
  if (!res || !res.timings) return;
  if (res.status < 200 || res.status >= 300) return;
  const d = res.timings.duration;
  const s = exec.scenario.name;
  if (s === 'sla_1s' && d < 1000) countOk1s.add(1);
  if (s === 'sla_2s' && d < 2000) countOk2s.add(1);
  if (s === 'sla_3s' && d < 3000) countOk3s.add(1);
}

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

  // serviceId — endpoint paginado: resposta é {content: [...], totalElements: N, ...}
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

  // testUserId — usuário criado para o PUT /v1/users/{id} (não o admin)
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
      trackResponseSla(res);
      check(res, { 'status 200 - Ping': (v) => v.status === 200 });
    });

  } else if (r < 0.13) {
    // ── GET /v1/reports/financial ─────────────────────────────────────────
    group('GET Financial Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/financial?from=2026-06-01&to=2026-06-30', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Financial Report': (v) => v.status === 200 });
    });

  } else if (r < 0.21) {
    // ── GET /v1/reports/appointments ──────────────────────────────────────
    group('GET Appointments Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/appointments?from=2026-06-01&to=2026-06-30', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Appointments Report': (v) => v.status === 200 });
    });

  } else if (r < 0.25) {
    // ── GET /v1/reports/payroll ───────────────────────────────────────────
    group('GET Payroll Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/payroll?from=2026-06-01&to=2026-06-30', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Payroll Report': (v) => v.status === 200 });
    });

  } else if (r < 0.35) {
    // ── GET /v1/appointments ──────────────────────────────────────────────
    group('GET All Appointments', () => {
      const res = http.get(BASE_URL + '/v1/appointments', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - List Appointments': (v) => v.status === 200 });
    });

  } else if (r < 0.45) {
    // ── GET /v1/cashflow ──────────────────────────────────────────────────
    group('GET CashFlow', () => {
      const res = http.get(BASE_URL + '/v1/cashflow', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - CashFlow': (v) => v.status === 200 });
    });

  } else if (r < 0.52) {
    // ── GET /v1/employees/booking (public, sem role check) ────────────────
    group('GET Employees Booking', () => {
      const res = http.get(BASE_URL + '/v1/employees/booking', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Employees Booking': (v) => v.status === 200 });
    });

  } else if (r < 0.59) {
    // ── GET /v1/products ──────────────────────────────────────────────────
    group('GET Products', () => {
      const res = http.get(BASE_URL + '/v1/products', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Products': (v) => v.status === 200 });
    });

  } else if (r < 0.65) {
    // ── GET /v1/services ──────────────────────────────────────────────────
    group('GET Services', () => {
      const res = http.get(BASE_URL + '/v1/services', { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Services': (v) => v.status === 200 });
    });

  } else if (r < 0.73) {
    // ── POST /v1/appointments ─────────────────────────────────────────────
    group('POST Create Appointment', () => {
      const res = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2026-09-15',
        clientNotes:   'k6 VU' + __VU + ' iter' + __ITER,
      }), { headers: h });
      trackResponseSla(res);
      check(res, { 'status 201 - Create Appointment': (v) => v.status === 201 });
    });

  } else if (r < 0.81) {
    // ── POST /v1/cashflow ─────────────────────────────────────────────────
    group('POST CashFlow Entry', () => {
      const res = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type:        Math.random() < 0.5 ? 'INCOME' : 'EXPENSE',
        amount:      parseFloat((Math.random() * 100 + 1).toFixed(2)),
        description: 'k6 VU' + __VU + ' iter' + __ITER,
        date:        '2026-07-01',
      }), { headers: h });
      trackResponseSla(res);
      check(res, { 'status 201 - CashFlow Entry': (v) => v.status === 201 });
    });

  } else if (r < 0.86) {
    // ── POST + DELETE /v1/cashflow/{id} ───────────────────────────────────
    group('POST+DELETE CashFlow', () => {
      const cr = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type: 'EXPENSE', amount: 1.00,
        description: 'k6 del VU' + __VU, date: '2026-07-01',
      }), { headers: h });
      if (cr.status === 201) {
        const id = cr.json('id');
        const dr = http.del(BASE_URL + '/v1/cashflow/' + id, null, { headers: h });
        trackResponseSla(dr);
        check(dr, { 'status 2xx - Delete CashFlow': (v) => v.status >= 200 && v.status < 300 });
      }
    });

  } else if (r < 0.91) {
    // ── POST + PATCH /v1/appointments/{id}/cancel ─────────────────────────
    group('POST+PATCH Cancel Appointment', () => {
      const cr = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2026-09-20',
        clientNotes:   'k6 cancel VU' + __VU,
      }), { headers: h });
      if (cr.status === 201) {
        const id = cr.json('id');
        const pr = http.patch(BASE_URL + '/v1/appointments/' + id + '/cancel', null, { headers: h });
        trackResponseSla(pr);
        check(pr, { 'status 2xx - Cancel Appointment': (v) => v.status >= 200 && v.status < 300 });
      }
    });

  } else if (r < 0.95) {
    // ── PUT /v1/products/{id} ─────────────────────────────────────────────
    group('PUT Update Product', () => {
      const res = http.put(BASE_URL + '/v1/products/' + data.productId, JSON.stringify({
        name:  'Produto k6 VU' + __VU,
        stock: 9999,
        price: parseFloat((Math.random() * 50 + 10).toFixed(2)),
        active: true,
      }), { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Update Product': (v) => v.status === 200 });
    });

  } else {
    // ── PATCH /v1/users/{id} ──────────────────────────────────────────────
    group('PATCH Update User', () => {
      if (!data.testUserId) return;
      const res = http.patch(BASE_URL + '/v1/users/' + data.testUserId, JSON.stringify({
        phone: '839' + String(Math.floor(Math.random() * 90000000 + 10000000)),
      }), { headers: h });
      trackResponseSla(res);
      check(res, { 'status 200 - Update User': (v) => v.status === 200 });
    });
  }

  sleep(0.1);
}

export function handleSummary(data) {
  const mv  = (key) => (data.metrics[key] ? data.metrics[key].values : null);
  const fmt = (n) => String(Math.round(n)).replace(/\B(?=(\d{3})+(?!\d))/g, '.');

  const ok1s = mv('count_ok_1s') ? mv('count_ok_1s').count : 0;
  const ok2s = mv('count_ok_2s') ? mv('count_ok_2s').count : 0;
  const ok3s = mv('count_ok_3s') ? mv('count_ok_3s').count : 0;

  function scenarioDur(scenario) {
    const d = mv('http_req_duration{scenario:' + scenario + '}');
    return {
      p95: d && d['p(95)'] != null ? d['p(95)'].toFixed(2) : 'N/A',
      p99: d && d['p(99)'] != null ? d['p(99)'].toFixed(2) : 'N/A',
      avg: d && d.avg      != null ? d.avg.toFixed(2)      : 'N/A',
      max: d && d.max      != null ? d.max.toFixed(2)      : 'N/A',
    };
  }

  function scenarioRps(scenario) {
    const r = mv('http_reqs{scenario:' + scenario + '}');
    return r ? (r.count / SCENARIO_DURATION_S).toFixed(2) : '0.00';
  }

  const d1 = scenarioDur('sla_1s');
  const d2 = scenarioDur('sla_2s');
  const d3 = scenarioDur('sla_3s');

  const failRate  = mv('http_req_failed') ? (mv('http_req_failed').rate * 100).toFixed(2) : '0.00';
  const totalReqs = mv('http_reqs')       ? mv('http_reqs').count : 0;

  const icon = (n, threshold) => n >= threshold ? '✅' : '❌';

  const reportContent =
`# Relatório de Carga e Performance — EQ03 (k6)

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | ramping-vus (auto: 1 → 60 VUs por cenário) |
| Duração por cenário | 120 s (90 s ramp + 30 s sustentado) |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /appointments · GET /cashflow · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Resultado Principal — Requisições HTTP-OK por Budget de Tempo

Cada cenário é **independente**: roda com seus próprios VUs e conta apenas
requisições com status HTTP 2xx **e** tempo de resposta dentro do budget.

| Budget | RPS real | Req. OK no Budget | p(95) | p(99) | Meta >1.000 |
|--------|----------|-------------------|-------|-------|-------------|
| **≤ 1 s** | ${scenarioRps('sla_1s')} req/s | **${fmt(ok1s)}** | ${d1.p95} ms | ${d1.p99} ms | ${icon(ok1s, 1000)} |
| **≤ 2 s** | ${scenarioRps('sla_2s')} req/s | **${fmt(ok2s)}** | ${d2.p95} ms | ${d2.p99} ms | ${icon(ok2s, 1000)} |
| **≤ 3 s** | ${scenarioRps('sla_3s')} req/s | **${fmt(ok3s)}** | ${d3.p95} ms | ${d3.p99} ms | ${icon(ok3s, 1000)} |

## Saúde Global
* **Total de requisições (todos os cenários):** ${fmt(totalReqs)}
* **Taxa de falha HTTP:** ${failRate}% (máximo permitido: 1,00%)

## Interpretação
O teste rampa automaticamente de 1 até 60 VUs durante cada cenário.
As colunas "Req. OK no Budget" mostram quantas requisições o sistema entregou
dentro do limite de tempo daquele cenário, independentemente dos outros.

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
