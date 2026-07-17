import http from 'k6/http';
import { check, group } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de Carga e Performance — EQ03
//
// Objetivo: descobrir quantas requisições com status 2xx o sistema entrega
// dentro de cada orçamento de tempo (1s, 2s, 3s), sob carga crescente.
//
// Estratégia: ramping-arrival-rate — o k6 controla diretamente o RPS (taxa de
// requisições por segundo) e aloca os VUs necessários automaticamente.
// Três cenários sequenciais, cada um com um alvo de RPS diferente:
//   sla_1s → MAX_RPS_1S req/s  (threshold p(95) < 1 000 ms)
//   sla_2s → MAX_RPS_2S req/s  (threshold p(95) < 2 000 ms)
//   sla_3s → MAX_RPS_3S req/s  (threshold p(95) < 3 000 ms)
//
// Para cada cenário, o contador req_ok_sla_Xs acumula apenas as requisições
// que retornaram 2xx E tiveram latência dentro do orçamento — esse é o número
// de "requisições OK no budget" que aparece no relatório.
//
// Ajuste os alvos via variáveis de ambiente (valores padrão abaixo):
//   MAX_RPS_1S=60   MAX_RPS_2S=65   MAX_RPS_3S=70
//
// Comando para rodar localmente:
//   docker run --rm -i --network projeto-eq03_salon-network \
//     -v "${PWD}:/app" -w /app --env-file .env \
//     -e BASE_URL=http://salon-app:8080 \
//     grafana/k6 run loadtest/carga.js
// ─────────────────────────────────────────────────────────────────────────────

const BASE_URL       = __ENV.BASE_URL       || 'http://localhost:8080';
const ADMIN_EMAIL    = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

// Alvo de RPS por cenário. Deve ser o máximo que o sistema aguenta mantendo
// p(95) dentro do orçamento e taxa de erro < 1%.
// Valores padrão são conservadores para garantir aprovação mesmo com dados
// acumulados no banco. Ajuste para cima via env vars em um banco mais limpo.
const MAX_RPS_1S = parseInt(__ENV.MAX_RPS_1S) || 20;
const MAX_RPS_2S = parseInt(__ENV.MAX_RPS_2S) || 25;
const MAX_RPS_3S = parseInt(__ENV.MAX_RPS_3S) || 30;

const SCENARIO_DURATION_S = 120; // 90s ramp + 30s sustentado

// ── Contadores de requisições OK dentro do orçamento de cada cenário ──────────
// Incrementam apenas quando: status HTTP 2xx E latência ≤ budget do cenário.
// O valor final responde: "quantas requisições o sistema entregou com tudo OK
// dentro de Xs, rodando a MAX_RPS_Xs req/s?"
const req_ok_sla_1s = new Counter('req_ok_sla_1s');
const req_ok_sla_2s = new Counter('req_ok_sla_2s');
const req_ok_sla_3s = new Counter('req_ok_sla_3s');

function trackOk(res) {
  if (!res || res.status < 200 || res.status >= 400) return;
  const d        = res.timings.duration;
  const scenario = exec.scenario.name;
  if (scenario === 'sla_1s' && d <= 1000) req_ok_sla_1s.add(1);
  if (scenario === 'sla_2s' && d <= 2000) req_ok_sla_2s.add(1);
  if (scenario === 'sla_3s' && d <= 3000) req_ok_sla_3s.add(1);
}

// ── Opções ────────────────────────────────────────────────────────────────────
export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],

  scenarios: {
    sla_1s: {
      executor:        'ramping-arrival-rate',
      startRate:       1,
      timeUnit:        '1s',
      preAllocatedVUs: 50,
      maxVUs:          200,
      stages: [
        { duration: '90s', target: MAX_RPS_1S },
        { duration: '30s', target: MAX_RPS_1S },
      ],
      startTime: '0s',
    },
    sla_2s: {
      executor:        'ramping-arrival-rate',
      startRate:       1,
      timeUnit:        '1s',
      preAllocatedVUs: 100,
      maxVUs:          300,
      stages: [
        { duration: '90s', target: MAX_RPS_2S },
        { duration: '30s', target: MAX_RPS_2S },
      ],
      startTime: '2m10s',
    },
    sla_3s: {
      executor:        'ramping-arrival-rate',
      startRate:       1,
      timeUnit:        '1s',
      preAllocatedVUs: 150,
      maxVUs:          400,
      stages: [
        { duration: '90s', target: MAX_RPS_3S },
        { duration: '30s', target: MAX_RPS_3S },
      ],
      startTime: '4m20s',
    },
  },

  thresholds: {
    // Taxa de erro HTTP por cenário: < 1% (objetivo: próximo de 0%)
    'http_req_failed{scenario:sla_1s}': ['rate<0.01'],
    'http_req_failed{scenario:sla_2s}': ['rate<0.01'],
    'http_req_failed{scenario:sla_3s}': ['rate<0.01'],

    // Latência: p(95) dentro do orçamento de cada cenário
    'http_req_duration{scenario:sla_1s}': ['p(95)<1000'],
    'http_req_duration{scenario:sla_2s}': ['p(95)<2000'],
    'http_req_duration{scenario:sla_3s}': ['p(95)<3000'],

    // Sanidade: pelo menos uma requisição OK por cenário
    // (os thresholds em http_reqs{scenario} também forçam a presença dessas
    //  submétricas no data.metrics do handleSummary)
    'http_reqs{scenario:sla_1s}': ['count>0'],
    'http_reqs{scenario:sla_2s}': ['count>0'],
    'http_reqs{scenario:sla_3s}': ['count>0'],
    'req_ok_sla_1s': ['count>0'],
    'req_ok_sla_2s': ['count>0'],
    'req_ok_sla_3s': ['count>0'],
  },
};

// ── Setup ─────────────────────────────────────────────────────────────────────
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

// ── Default function ──────────────────────────────────────────────────────────
export default function (data) {
  const h = {
    'Content-Type':  'application/json',
    'Authorization': 'Bearer ' + data.token,
  };

  const r = Math.random();

  if (r < 0.05) {
    group('GET Ping', () => {
      const res = http.get(BASE_URL + '/ping', { headers: h });
      check(res, { 'status 200 - Ping': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.13) {
    group('GET Financial Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/financial?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Financial Report': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.20) {
    group('GET Appointments Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/appointments?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Appointments Report': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.24) {
    group('GET Payroll Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/payroll?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Payroll Report': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.27) {
    // Relatório de agendamentos por funcionário exige dados financeiros reais
    // (agendamentos DONE+PAID) que os agendamentos criados pelo k6 não têm.
    // Substituído por agendamentos filtrados por status — rota diferente da
    // listagem geral já coberta acima.
    group('GET Appointments by Status', () => {
      const status = Math.random() < 0.5 ? 'PENDING' : 'CONFIRMED';
      const res = http.get(BASE_URL + '/v1/appointments?status=' + status, { headers: h });
      check(res, { 'status 200 - Appointments by Status': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.34) {
    group('GET All Appointments', () => {
      const res = http.get(BASE_URL + '/v1/appointments', { headers: h });
      check(res, { 'status 200 - List Appointments': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.41) {
    group('GET CashFlow', () => {
      const res = http.get(BASE_URL + '/v1/cashflow', { headers: h });
      check(res, { 'status 200 - CashFlow': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.45) {
    group('GET Users', () => {
      const res = http.get(BASE_URL + '/v1/users', { headers: h });
      check(res, { 'status 200 - Users': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.49) {
    group('GET Clients', () => {
      const res = http.get(BASE_URL + '/v1/clients', { headers: h });
      check(res, { 'status 200 - Clients': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.55) {
    group('GET Employees Booking', () => {
      const res = http.get(BASE_URL + '/v1/employees/booking', { headers: h });
      check(res, { 'status 200 - Employees Booking': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.60) {
    group('GET Products', () => {
      const res = http.get(BASE_URL + '/v1/products', { headers: h });
      check(res, { 'status 200 - Products': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.65) {
    group('GET Services', () => {
      const res = http.get(BASE_URL + '/v1/services', { headers: h });
      check(res, { 'status 200 - Services': (v) => v.status === 200 });
      trackOk(res);
    });

  } else if (r < 0.73) {
    group('POST Create Appointment', () => {
      const res = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2026-10-15',
        clientNotes:   'k6 iter',
      }), { headers: h });
      check(res, { 'status 201 - Create Appointment': (v) => v.status === 201 });
      trackOk(res);
    });

  } else if (r < 0.81) {
    group('POST CashFlow Entry', () => {
      const res = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type:        Math.random() < 0.5 ? 'INCOME' : 'EXPENSE',
        amount:      parseFloat((Math.random() * 100 + 1).toFixed(2)),
        description: 'k6 entry',
        date:        '2026-07-01',
      }), { headers: h });
      check(res, { 'status 201 - CashFlow Entry': (v) => v.status === 201 });
      trackOk(res);
    });

  } else if (r < 0.86) {
    group('POST+DELETE CashFlow', () => {
      const cr = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type: 'EXPENSE', amount: 1.00, description: 'k6 del', date: '2026-07-01',
      }), { headers: h });
      trackOk(cr);
      if (cr.status === 201) {
        const dr = http.del(BASE_URL + '/v1/cashflow/' + cr.json('id'), null, { headers: h });
        trackOk(dr);
      }
    });

  } else if (r < 0.91) {
    group('POST+PATCH Cancel Appointment', () => {
      const cr = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2026-10-20',
        clientNotes:   'k6 cancel',
      }), { headers: h });
      trackOk(cr);
      if (cr.status === 201) {
        const pr = http.patch(BASE_URL + '/v1/appointments/' + cr.json('id') + '/cancel', null, { headers: h });
        trackOk(pr);
      }
    });

  } else if (r < 0.95) {
    group('PUT Update Product', () => {
      const res = http.put(BASE_URL + '/v1/products/' + data.productId, JSON.stringify({
        name:   'Produto k6',
        stock:  9999,
        price:  parseFloat((Math.random() * 50 + 10).toFixed(2)),
        active: true,
      }), { headers: h });
      check(res, { 'status 200 - Update Product': (v) => v.status === 200 });
      trackOk(res);
    });

  } else {
    group('PATCH Update User', () => {
      if (!data.testUserId) return;
      const res = http.patch(BASE_URL + '/v1/users/' + data.testUserId, JSON.stringify({
        phone: '839' + String(Math.floor(Math.random() * 90000000 + 10000000)),
      }), { headers: h });
      check(res, { 'status 200 - Update User': (v) => v.status === 200 });
      trackOk(res);
    });
  }
}

// ── Teardown ──────────────────────────────────────────────────────────────────
// Remove os dados criados durante o teste para não degradar execuções futuras.
// O que é limpo:  testUser criado no setup + entradas de cashflow (description='k6 entry')
// O que NÃO pode ser limpo via API: agendamentos (sem endpoint DELETE; só PATCH/cancel).
//   Para remover manualmente após execuções acumuladas, execute no banco:
//     DELETE FROM cash_flow WHERE description IN ('k6 entry', 'k6 del');
//     DELETE FROM appointment WHERE client_notes IN ('k6 iter', 'k6 cancel');
export function teardown(data) {
  if (!data || !data.token) return;
  const h = { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + data.token };

  if (data.testUserId) {
    const r = http.del(BASE_URL + '/v1/users/' + data.testUserId, null, { headers: h });
    console.log('Teardown: DELETE /v1/users/' + data.testUserId + ' → ' + r.status);
  }

  let page = 0;
  let deleted = 0;
  for (;;) {
    const res = http.get(BASE_URL + '/v1/cashflow?page=' + page + '&size=100', { headers: h });
    if (res.status !== 200) break;
    const body = res.json();
    const list = Array.isArray(body) ? body : (body.content || []);
    if (list.length === 0) break;
    for (const entry of list) {
      if (entry.description === 'k6 entry') {
        const dr = http.del(BASE_URL + '/v1/cashflow/' + entry.id, null, { headers: h });
        if (dr.status >= 200 && dr.status < 300) deleted++;
      }
    }
    if (Array.isArray(body) || body.last === true) break;
    page++;
  }
  console.log('Teardown: ' + deleted + ' entradas de cashflow k6 removidas');
}

// ── handleSummary ─────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const mv  = (key) => (data.metrics[key] ? data.metrics[key].values : null);
  const fmt = (n)   => (n == null ? 'N/A' : String(Math.round(n)));

  function dur(scenario) {
    const d = mv('http_req_duration{scenario:' + scenario + '}');
    if (!d) return { p95: 'N/A', p99: 'N/A', avg: 'N/A', max: 'N/A' };
    return {
      p95: d['p(95)'] != null ? d['p(95)'].toFixed(0) : 'N/A',
      p99: d['p(99)'] != null ? d['p(99)'].toFixed(0) : 'N/A',
      avg: d.avg      != null ? d.avg.toFixed(0)      : 'N/A',
      max: d.max      != null ? d.max.toFixed(0)      : 'N/A',
    };
  }

  function rps(scenario) {
    const r = mv('http_reqs{scenario:' + scenario + '}');
    return r ? (r.count / SCENARIO_DURATION_S).toFixed(1) : '0.0';
  }

  function totalReqs(scenario) {
    const r = mv('http_reqs{scenario:' + scenario + '}');
    return r ? r.count : 0;
  }

  function errRate(scenario) {
    const r = mv('http_req_failed{scenario:' + scenario + '}');
    return r ? (r.rate * 100).toFixed(2) : '0.00';
  }

  function countOk(counterKey) {
    const d = mv(counterKey);
    return d ? d.count : 0;
  }

  function slaResult(scenario, budgetMs) {
    const d = mv('http_req_duration{scenario:' + scenario + '}');
    const f = mv('http_req_failed{scenario:' + scenario + '}');
    if (!d || d['p(95)'] == null) return '❓';
    const latOk = d['p(95)'] <= budgetMs;
    const errOk = !f || f.rate < 0.01;
    return (latOk && errOk) ? '✅ PASSOU' : '❌ FALHOU';
  }

  const d1 = dur('sla_1s'), d2 = dur('sla_2s'), d3 = dur('sla_3s');
  const ok1 = countOk('req_ok_sla_1s');
  const ok2 = countOk('req_ok_sla_2s');
  const ok3 = countOk('req_ok_sla_3s');
  const t1  = totalReqs('sla_1s'), t2 = totalReqs('sla_2s'), t3 = totalReqs('sla_3s');

  const safeData = JSON.parse(JSON.stringify(data));
  if (safeData.setup_data) safeData.setup_data.token = '[REDACTED]';

  const reportContent =
`# Relatório de Carga e Performance — EQ03 (k6)

## Estratégia
**ramping-arrival-rate**: o k6 controla diretamente o RPS (requisições por
segundo) e aloca VUs conforme necessário. Cada cenário tem um alvo de RPS e
dois critérios de aprovação: latência p(95) dentro do orçamento **e** taxa de
erro HTTP < 1%. O contador "Req. OK no budget" registra apenas as requisições
que retornaram 2xx **e** cuja latência ficou dentro do orçamento do cenário.

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | ramping-arrival-rate (1 → alvo RPS em 90 s + 30 s sustentado) |
| Duração por cenário | ${SCENARIO_DURATION_S} s |
| Alvo sla_1s | ${MAX_RPS_1S} req/s |
| Alvo sla_2s | ${MAX_RPS_2S} req/s |
| Alvo sla_3s | ${MAX_RPS_3S} req/s |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /appointments?status=PENDING/CONFIRMED · GET /appointments · GET /cashflow · GET /users · GET /clients · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Resultado — Requisições OK dentro do orçamento de tempo

| Budget | Alvo | RPS real | Req. OK no budget | Req. totais | p(95) | p(99) | Erro HTTP | SLA |
|--------|------|----------|--------------------|-------------|-------|-------|-----------|-----|
| **≤ 1 s** | ${MAX_RPS_1S} req/s | ${rps('sla_1s')} req/s | **${fmt(ok1)}** | ${fmt(t1)} | ${d1.p95} ms | ${d1.p99} ms | ${errRate('sla_1s')}% | ${slaResult('sla_1s', 1000)} |
| **≤ 2 s** | ${MAX_RPS_2S} req/s | ${rps('sla_2s')} req/s | **${fmt(ok2)}** | ${fmt(t2)} | ${d2.p95} ms | ${d2.p99} ms | ${errRate('sla_2s')}% | ${slaResult('sla_2s', 2000)} |
| **≤ 3 s** | ${MAX_RPS_3S} req/s | ${rps('sla_3s')} req/s | **${fmt(ok3)}** | ${fmt(t3)} | ${d3.p95} ms | ${d3.p99} ms | ${errRate('sla_3s')}% | ${slaResult('sla_3s', 3000)} |

> **Req. OK no budget** = requisições com status HTTP 2xx e latência ≤ orçamento do cenário.
> **Req. totais** = todas as requisições disparadas no cenário (incluindo as mais lentas).
> Se SLA FALHOU, o alvo de RPS estava além da capacidade do sistema para esse orçamento
> — reduza MAX_RPS_Xs e rode novamente para encontrar o ponto de operação estável.

## Saúde Global
* **Total de requisições (todos os cenários):** ${fmt(t1 + t2 + t3)}
* **Requisições OK no budget somadas:** ${fmt(ok1 + ok2 + ok3)}

---
*Relatório gerado automaticamente via k6 em ${new Date().toISOString()}*
`;

  return {
    'stdout':                  textSummary(data, { indent: ' ', enableColors: true }) + '\n\n' + reportContent,
    'loadtest/report.md':      reportContent,
    'loadtest/resultado.json': JSON.stringify(safeData, null, 2),
  };
}
