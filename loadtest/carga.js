import http from 'k6/http';
import { check, group } from 'k6';
import encoding from 'k6/encoding';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de Carga e Performance — EQ03
//
// Objetivo: descobrir, para cada orçamento de tempo (1s, 2s, 3s), o MAIOR
// número de requisições por segundo que o sistema sustenta respondendo dentro
// desse tempo com taxa de erro desprezível (< 1%).
//
// Estratégia: ESCADA DE CARGA (staircase) — uma sequência de degraus, cada um
// com uma taxa de requisições fixa (constant-arrival-rate) sustentada por
// STEP_DURATION_S segundos. Ex.: 20 req/s por 30s, depois 40 req/s por 30s,
// depois 60 req/s por 30s... Cada degrau é um "scenario" nomeado (step_20,
// step_40, ...), o que faz o k6 taguear http_req_duration/http_reqs/
// http_req_failed por degrau automaticamente — sem precisar de métricas
// customizadas.
//
// Ao final, o handleSummary varre todos os degraus e encontra, para cada
// orçamento (1s/2s/3s), o degrau de MAIOR RPS cujo p(95) ficou dentro do
// orçamento E cuja taxa de erro ficou abaixo de 1%. Esse RPS é o "teto" —
// a resposta à pergunta "quanto o sistema aguenta até Xs".
//
// Ajuste a escada via variáveis de ambiente:
//   STEP_START=20 STEP_INCREMENT=20 STEP_COUNT=10 STEP_DURATION_S=30
//   (padrão: degraus de 20, 40, 60 ... até 200 req/s, 30s cada — ~5 min total)
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

const STEP_START      = parseInt(__ENV.STEP_START)      || 20;
const STEP_INCREMENT  = parseInt(__ENV.STEP_INCREMENT)  || 20;
const STEP_COUNT      = parseInt(__ENV.STEP_COUNT)      || 10;
const STEP_DURATION_S = parseInt(__ENV.STEP_DURATION_S) || 30;

const STEPS = Array.from({ length: STEP_COUNT }, (_, i) => STEP_START + i * STEP_INCREMENT);

// ── Opções ────────────────────────────────────────────────────────────────────
const scenarios   = {};
const thresholds  = {};

STEPS.forEach((rps, idx) => {
  const name = 'step_' + rps;
  scenarios[name] = {
    executor:        'constant-arrival-rate',
    rate:            rps,
    timeUnit:        '1s',
    duration:        STEP_DURATION_S + 's',
    preAllocatedVUs: Math.min(rps * 5, 600),
    maxVUs:          Math.min(rps * 10, 1200),
    startTime:       (idx * STEP_DURATION_S) + 's',
  };
  // Thresholds triviais (sempre verdadeiros) apenas para forçar o k6 a manter
  // as submétricas tagueadas por cenário disponíveis em data.metrics no
  // handleSummary — sem eles, http_reqs{scenario:step_X} não apareceria.
  thresholds['http_reqs{scenario:' + name + '}']        = ['count>=0'];
  thresholds['http_req_duration{scenario:' + name + '}'] = ['p(99)>=0'];
  thresholds['http_req_failed{scenario:' + name + '}']   = ['rate>=0'];
});

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios,
  thresholds,
  setupTimeout:    '60s',
  teardownTimeout: '5m',
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

  // testUserId — usuário separado para PATCH /v1/users/{id}.
  // roleId=4 (CLIENTE) — id fixo dado pela ordem de seed das roles neste
  // projeto (1=ADMIN, 2=GERENTE_DE_ATENDIMENTO, 3=FUNCIONARIA, 4=CLIENTE,
  // 5=SYSADMIN). Só serve como alvo do PATCH (nunca autentica), mas o role
  // correto evita criar sem querer uma conta com permissão administrativa.
  let testUserId = null;
  const regU = http.post(BASE_URL + '/v1/users', JSON.stringify({
    name: 'User k6 Update', email: 'k6_upd_' + Date.now() + '@teste.com',
    password: 'UpdateUser@123', phone: '83922220000', active: true, roleId: 4,
  }), { headers: h });
  if (regU.status === 200 || regU.status === 201) testUserId = regU.json('id');

  // authUserId — ID do usuário autenticado, extraído direto do payload do JWT
  // (claim "userId"). Necessário porque o SYSADMIN é oculto de /v1/users por
  // design (RoleService: acesso hardcoded, nunca listado). A API cria os
  // agendamentos com client=currentUser quando o papel do chamador não é
  // ADMIN/GERENTE — o teardown usa esse ID para cancelar os agendamentos
  // criados durante o teste.
  let authUserId = null;
  try {
    const payload = JSON.parse(encoding.b64decode(token.split('.')[1], 'rawurl', 's'));
    authUserId = payload.userId;
  } catch (e) {
    console.error('Não foi possível decodificar o JWT para extrair userId: ' + e);
  }

  console.log('Setup OK — serviceId=' + serviceId + ' employeeId=' + employeeId +
              ' productId=' + productId + ' testUserId=' + testUserId +
              ' authUserId=' + authUserId + ' | degraus: ' + STEPS.join(', ') + ' req/s');
  return { token, serviceId, employeeId, productId, testUserId, authUserId };
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
    });

  } else if (r < 0.13) {
    group('GET Financial Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/financial?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Financial Report': (v) => v.status === 200 });
    });

  } else if (r < 0.20) {
    group('GET Appointments Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/appointments?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Appointments Report': (v) => v.status === 200 });
    });

  } else if (r < 0.24) {
    group('GET Payroll Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/payroll?from=2026-06-01&to=2026-06-30', { headers: h });
      check(res, { 'status 200 - Payroll Report': (v) => v.status === 200 });
    });

  } else if (r < 0.27) {
    group('GET Appointments by Status', () => {
      const status = Math.random() < 0.5 ? 'PENDING' : 'CONFIRMED';
      const res = http.get(BASE_URL + '/v1/appointments?status=' + status + '&page=0&size=20', { headers: h });
      check(res, { 'status 200 - Appointments by Status': (v) => v.status === 200 });
    });

  } else if (r < 0.34) {
    group('GET All Appointments', () => {
      const res = http.get(BASE_URL + '/v1/appointments?page=0&size=20', { headers: h });
      check(res, { 'status 200 - List Appointments': (v) => v.status === 200 });
    });

  } else if (r < 0.41) {
    group('GET CashFlow', () => {
      const res = http.get(BASE_URL + '/v1/cashflow', { headers: h });
      check(res, { 'status 200 - CashFlow': (v) => v.status === 200 });
    });

  } else if (r < 0.45) {
    group('GET Users', () => {
      const res = http.get(BASE_URL + '/v1/users', { headers: h });
      check(res, { 'status 200 - Users': (v) => v.status === 200 });
    });

  } else if (r < 0.49) {
    group('GET Clients', () => {
      const res = http.get(BASE_URL + '/v1/clients', { headers: h });
      check(res, { 'status 200 - Clients': (v) => v.status === 200 });
    });

  } else if (r < 0.55) {
    group('GET Employees Booking', () => {
      const res = http.get(BASE_URL + '/v1/employees/booking', { headers: h });
      check(res, { 'status 200 - Employees Booking': (v) => v.status === 200 });
    });

  } else if (r < 0.60) {
    group('GET Products', () => {
      const res = http.get(BASE_URL + '/v1/products', { headers: h });
      check(res, { 'status 200 - Products': (v) => v.status === 200 });
    });

  } else if (r < 0.65) {
    group('GET Services', () => {
      const res = http.get(BASE_URL + '/v1/services', { headers: h });
      check(res, { 'status 200 - Services': (v) => v.status === 200 });
    });

  } else if (r < 0.73) {
    group('POST Create Appointment', () => {
      const res = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2099-10-15',
      }), { headers: h });
      check(res, { 'status 201 - Create Appointment': (v) => v.status === 201 });
    });

  } else if (r < 0.81) {
    group('POST CashFlow Entry', () => {
      const res = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type:        Math.random() < 0.5 ? 'INCOME' : 'EXPENSE',
        amount:      parseFloat((Math.random() * 100 + 1).toFixed(2)),
        description: 'k6 entry',
        date:        '2099-01-01',
      }), { headers: h });
      check(res, { 'status 201 - CashFlow Entry': (v) => v.status === 201 });
    });

  } else if (r < 0.86) {
    group('POST+DELETE CashFlow', () => {
      const cr = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type: 'EXPENSE', amount: 1.00, description: 'k6 del', date: '2099-01-01',
      }), { headers: h });
      if (cr.status === 201) {
        http.del(BASE_URL + '/v1/cashflow/' + cr.json('id'), null, { headers: h });
      }
    });

  } else if (r < 0.91) {
    group('POST+PATCH Cancel Appointment', () => {
      const cr = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2099-10-20',
      }), { headers: h });
      if (cr.status === 201) {
        http.patch(BASE_URL + '/v1/appointments/' + cr.json('id') + '/cancel', null, { headers: h });
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
    });

  } else {
    group('PATCH Update User', () => {
      if (!data.testUserId) return;
      const res = http.patch(BASE_URL + '/v1/users/' + data.testUserId, JSON.stringify({
        phone: '839' + String(Math.floor(Math.random() * 90000000 + 10000000)),
      }), { headers: h });
      check(res, { 'status 200 - Update User': (v) => v.status === 200 });
    });
  }
}

// ── Teardown ──────────────────────────────────────────────────────────────────
// Limpa todos os dados criados pelo teste para não degradar execuções futuras.
//
// Estratégia de isolamento:
//   • Agendamentos: a API os cria com client=usuário autenticado (authUserId),
//     pois o sysadmin não é reconhecido como staff (isStaff só aceita ADMIN e
//     GERENTE). O teardown cancela todos os agendamentos com status REQUESTED
//     pertencentes ao authUserId. REQUESTED é o status exclusivo do fluxo de
//     cliente — nunca gerado por operações administrativas normais.
//   • Cashflow: criados com date=2099-01-01 (data impossível em produção)
//     → teardown filtra por período e deleta todos.
//   • testUserId: deletado ao final.
export function teardown(data) {
  if (!data || !data.token) return;
  const h = { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + data.token };

  // 1. Cancelar todos os agendamentos REQUESTED do usuário autenticado.
  // Sempre relê page=0: cada cancelamento tira o item do filtro
  // status=REQUESTED, então o conjunto "encolhe" a cada volta — incrementar
  // a página aqui pularia itens (bug corrigido).
  let cancelados = 0;
  if (data.authUserId) {
    for (;;) {
      const res = http.get(
        BASE_URL + '/v1/appointments?clientId=' + data.authUserId +
        '&status=REQUESTED&size=100&page=0',
        { headers: h }
      );
      if (res.status !== 200) break;
      const body = res.json();
      const list = Array.isArray(body) ? body : (body.content || []);
      if (list.length === 0) break;
      for (const appt of list) {
        const cr = http.patch(BASE_URL + '/v1/appointments/' + appt.id + '/cancel', null, { headers: h });
        if (cr.status >= 200 && cr.status < 300) cancelados++;
      }
    }
  }
  console.log('Teardown: ' + cancelados + ' agendamentos k6 cancelados');

  // 2. Deletar entradas de cashflow criadas no período exclusivo do teste (2099)
  let deletados = 0;
  const cfRes = http.get(BASE_URL + '/v1/cashflow?from=2099-01-01&to=2099-12-31', { headers: h });
  if (cfRes.status === 200) {
    const entries = cfRes.json();
    const list = Array.isArray(entries) ? entries : (entries.content || []);
    for (const entry of list) {
      const dr = http.del(BASE_URL + '/v1/cashflow/' + entry.id, null, { headers: h });
      if (dr.status >= 200 && dr.status < 300) deletados++;
    }
  }
  console.log('Teardown: ' + deletados + ' entradas de cashflow k6 deletadas');

  // 3. Deletar o usuário de teste criado no setup
  if (data.testUserId) {
    const r = http.del(BASE_URL + '/v1/users/' + data.testUserId, null, { headers: h });
    console.log('Teardown: DELETE /v1/users/' + data.testUserId + ' → ' + r.status);
  }
}

// ── handleSummary ─────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const mv  = (key) => (data.metrics[key] ? data.metrics[key].values : null);
  const fmt = (n)   => (n == null ? 'N/A' : String(Math.round(n)));

  function stepStats(rps) {
    const scenario = 'step_' + rps;
    const d = mv('http_req_duration{scenario:' + scenario + '}');
    const f = mv('http_req_failed{scenario:' + scenario + '}');
    const t = mv('http_reqs{scenario:' + scenario + '}');
    if (!d || !f || !t || d['p(95)'] == null || t.count === 0) return null;
    const total = t.count;
    // http_req_failed é uma métrica Rate que soma 1 quando a requisição FALHA.
    // Logo "passes" (o Rate contou 1/verdadeiro) = nº de FALHAS, e "fails"
    // (contou 0/falso) = nº de requisições OK. Nomenclatura genérica do k6,
    // não confundir com sucesso/falha do teste.
    const ok = f.fails != null ? f.fails : Math.round(total * (1 - f.rate));
    return {
      rps, total, ok,
      errRate:      f.rate,
      p95:          d['p(95)'],
      p99:          d['p(99)'] != null ? d['p(99)'] : null,
      achievedRps:  total / STEP_DURATION_S,
    };
  }

  const results = STEPS.map(stepStats).filter(Boolean);

  // Exige sequência ININTERRUPTA de degraus aprovados desde o primeiro degrau.
  // Se o sistema falhar em um degrau e "se recuperar" num degrau mais alto
  // (comum sob contenção de recursos — variância de GC, pool de conexões
  // liberando momentaneamente), esse degrau posterior NÃO conta como
  // capacidade confiável: foi sorte, não sustentação real.
  function findCeiling(budgetMs) {
    let best = null;
    for (const s of results) {
      if (s.p95 <= budgetMs && s.errRate < 0.01) {
        best = s;
      } else {
        break;
      }
    }
    return best;
  }

  const ceiling1 = findCeiling(1000);
  const ceiling2 = findCeiling(2000);
  const ceiling3 = findCeiling(3000);

  function ceilingLine(label, budgetMs, ceiling) {
    if (!ceiling) {
      return `- **${label}:** nenhum degrau testado ficou dentro desse orçamento com erro < 1%. ` +
             `Reduza STEP_START/STEP_INCREMENT e rode novamente para encontrar o teto real.`;
    }
    return `- **${label}:** o sistema sustenta **${ceiling.rps} req/s** com p(95) de ${fmt(ceiling.p95)} ms ` +
           `e ${(ceiling.errRate * 100).toFixed(2)}% de erro — nesse ritmo, entregou **${fmt(ceiling.ok)} requisições ` +
           `bem-sucedidas em ${STEP_DURATION_S} s** de teste sustentado.`;
  }

  const stepsTableRows = results.map((s) => {
    const check1 = s.p95 <= 1000 && s.errRate < 0.01 ? '✅' : '❌';
    const check2 = s.p95 <= 2000 && s.errRate < 0.01 ? '✅' : '❌';
    const check3 = s.p95 <= 3000 && s.errRate < 0.01 ? '✅' : '❌';
    return `| ${s.rps} req/s | ${s.achievedRps.toFixed(1)} req/s | ${fmt(s.total)} | ${fmt(s.ok)} | ` +
           `${(s.errRate * 100).toFixed(2)}% | ${fmt(s.p95)} ms | ${s.p99 != null ? fmt(s.p99) : 'N/A'} ms | ` +
           `${check1} | ${check2} | ${check3} |`;
  }).join('\n');

  const totalReqsAll = results.reduce((acc, s) => acc + s.total, 0);
  const totalOkAll    = results.reduce((acc, s) => acc + s.ok, 0);

  const safeData = JSON.parse(JSON.stringify(data));
  if (safeData.setup_data) safeData.setup_data.token = '[REDACTED]';

  const reportContent =
`# Relatório de Carga e Performance — EQ03 (k6)

## Estratégia
**Escada de carga (staircase) com constant-arrival-rate**: o teste sobe em
degraus de RPS fixo (${STEPS.join(', ')} req/s), cada um sustentado por
${STEP_DURATION_S} s, exercitando um mix realista de rotas autenticadas.
Cada degrau é medido isoladamente (p(95), taxa de erro, total de requisições).
Ao final, para cada orçamento de tempo (1s, 2s, 3s) o relatório identifica o
**maior RPS sustentado** cujo p(95) ficou dentro do orçamento **e** cuja taxa
de erro HTTP ficou abaixo de 1% — esse é o "teto de capacidade" do sistema
para aquele tempo de resposta.

## Configuração
| Parâmetro | Valor |
|-----------|-------|
| Ferramenta | k6 |
| Executor | constant-arrival-rate (degraus fixos e sequenciais) |
| Degraus testados | ${STEPS.join(', ')} req/s |
| Duração por degrau | ${STEP_DURATION_S} s |
| Duração total | ${STEPS.length * STEP_DURATION_S} s |
| Rotas testadas | GET /ping · GET /reports/financial · GET /reports/appointments · GET /reports/payroll · GET /appointments?status=PENDING/CONFIRMED · GET /appointments · GET /cashflow · GET /users · GET /clients · GET /employees/booking · GET /products · GET /services · POST /appointments · POST /cashflow · POST+DELETE /cashflow · POST+PATCH /appointments/cancel · PUT /products · PATCH /users |

## Teto de capacidade por orçamento de tempo
${ceilingLine('Até 1 s', 1000, ceiling1)}
${ceilingLine('Até 2 s', 2000, ceiling2)}
${ceilingLine('Até 3 s', 3000, ceiling3)}

## Resultado por degrau

| Alvo | RPS real | Req. totais | Req. OK | Erro HTTP | p(95) | p(99) | ≤1s | ≤2s | ≤3s |
|------|----------|-------------|---------|-----------|-------|-------|-----|-----|-----|
${stepsTableRows}

> **Req. OK** = requisições sem falha HTTP (status < 400) no degrau.
> **≤1s/≤2s/≤3s** = ✅ se p(95) do degrau ficou dentro do orçamento **e** erro < 1%.

## Gargalos Identificados

1. **Pool de conexões com o banco limitado a 5 conexões**
   (\`application-dev.yaml\` e \`application-prod.yaml\`:
   \`hikari.maximum-pool-size: 5\`). Sob concorrência, qualquer carga acima de
   ~5 requisições simultâneas dependentes do banco enfileira, o que explica o
   ponto em que a latência começa a crescer nos degraus mais altos da escada.
   **Correção sugerida:** aumentar o pool (ex.: 20–30) e monitorar uso de
   conexões em produção antes de fixar um valor definitivo.

## Saúde Global
* **Total de requisições (todos os degraus):** ${fmt(totalReqsAll)}
* **Requisições OK somadas:** ${fmt(totalOkAll)}

---
*Relatório gerado automaticamente via k6 em ${new Date().toISOString()}*
`;

  return {
    'stdout':                  textSummary(data, { indent: ' ', enableColors: true }) + '\n\n' + reportContent,
    'loadtest/report.md':      reportContent,
    'loadtest/resultado.json': JSON.stringify(safeData, null, 2),
  };
}
