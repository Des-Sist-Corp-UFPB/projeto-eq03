import http from 'k6/http';
import { check, group } from 'k6';
import encoding from 'k6/encoding';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { extractAllSteps, extractByTag, findCeiling, isSoakStable } from './lib/capacity.js';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de Carga e Performance — EQ03
//
// Objetivo: descobrir o MAIOR número de requisições por segundo que o sistema
// sustenta respondendo dentro de um único orçamento de latência (SLA_MS,
// padrão 1000 ms = "sente como instantâneo", limiar clássico de UX) com 0%
// de erro — a pergunta "quantas requisições em até 1s o sistema aguenta,
// com tudo dando OK?".
//
// Dois modos, escolhidos por MODE:
//
//   MODE=staircase (padrão) — escada de degraus de RPS fixo
//     (constant-arrival-rate), cada um sustentado por STEP_DURATION_S.
//     Serve tanto para um bracket grosso (poucos degraus, passo largo, achar
//     ONDE o sistema quebra) quanto para uma busca fina (passo estreito,
//     dentro da faixa onde já se sabe que a quebra acontece).
//       STEP_START=20 STEP_INCREMENT=20 STEP_COUNT=10 STEP_DURATION_S=30
//
//   MODE=soak — um único RPS fixo (SOAK_RPS, obrigatório) sustentado por
//     SOAK_DURATION_S (padrão 180s = 3 min). Usado para CONFIRMAR que um
//     teto encontrado na escada é sustentável de verdade, não sorte de
//     30 segundos: cada requisição é tagueada com half=first/second (duas
//     metades da janela) para comparar se a latência se mantém estável ao
//     longo do tempo ou vai degradando (fila crescendo).
//
// Os dois modos usam a MESMA lógica de análise (loadtest/lib/capacity.js),
// então staircase e soak nunca podem discordar sobre o que conta como "OK".
//
// Fluxo recomendado de uso (manual, sem orquestração):
//   1. Rodar MODE=staircase com uma faixa larga (padrão) para achar ONDE o
//      sistema quebra.
//   2. Opcional — rodar de novo com STEP_START/STEP_INCREMENT mais estreitos
//      só na faixa de transição, para precisão maior.
//   3. Opcional — confirmar o teto encontrado com MODE=soak por alguns
//      minutos, para garantir que é capacidade sustentável e não sorte de
//      30 segundos.
//
// Veja loadtest/README.md para os comandos completos (Linux/macOS/Windows).
// ─────────────────────────────────────────────────────────────────────────────

const BASE_URL       = __ENV.BASE_URL       || 'http://localhost:8080';
const ADMIN_EMAIL    = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

const MODE   = __ENV.MODE   || 'staircase';
const SLA_MS = parseInt(__ENV.SLA_MS) || 1000;

const REPORT_PATH = __ENV.REPORT_PATH || 'loadtest/report.md';
const RESULT_PATH = __ENV.RESULT_PATH || 'loadtest/resultado.json';

if (MODE !== 'staircase' && MODE !== 'soak') {
  throw new Error('ERRO: MODE deve ser "staircase" ou "soak" (recebido: ' + MODE + ')');
}

// ── Configuração específica de cada modo ───────────────────────────────────────
const STEP_START      = parseInt(__ENV.STEP_START)      || 20;
const STEP_INCREMENT  = parseInt(__ENV.STEP_INCREMENT)  || 20;
const STEP_COUNT      = parseInt(__ENV.STEP_COUNT)      || 10;
const STEP_DURATION_S = parseInt(__ENV.STEP_DURATION_S) || 30;
const STEPS = Array.from({ length: STEP_COUNT }, (_, i) => STEP_START + i * STEP_INCREMENT);

const SOAK_RPS         = parseInt(__ENV.SOAK_RPS);
const SOAK_DURATION_S  = parseInt(__ENV.SOAK_DURATION_S) || 180;

if (MODE === 'soak' && !(SOAK_RPS > 0)) {
  throw new Error('ERRO: MODE=soak exige SOAK_RPS (RPS fixo a sustentar) — nenhum valor padrão faz sentido aqui.');
}

// ── Opções (scenarios/thresholds) por modo ─────────────────────────────────────
const scenarios  = {};
const thresholds = {};

function vusFor(rps) {
  return { preAllocatedVUs: Math.min(rps * 5, 600), maxVUs: Math.min(rps * 10, 1200) };
}

// Thresholds triviais (sempre verdadeiros) só para forçar o k6 a manter as
// submétricas tagueadas disponíveis em data.metrics no handleSummary — sem
// eles, uma submétrica tagueada não apareceria se não houver nenhum
// threshold referenciando exatamente aquela tag.
function forceSubmetric(tagExpr) {
  thresholds['http_reqs{' + tagExpr + '}']         = ['count>=0'];
  thresholds['http_req_duration{' + tagExpr + '}']  = ['p(99)>=0'];
  thresholds['http_req_failed{' + tagExpr + '}']    = ['rate>=0'];
}

if (MODE === 'staircase') {
  STEPS.forEach((rps, idx) => {
    const name = 'step_' + rps;
    scenarios[name] = {
      executor:  'constant-arrival-rate',
      rate:      rps,
      timeUnit:  '1s',
      duration:  STEP_DURATION_S + 's',
      startTime: (idx * STEP_DURATION_S) + 's',
      ...vusFor(rps),
    };
    forceSubmetric('scenario:' + name);
  });
} else {
  scenarios.soak = {
    executor: 'constant-arrival-rate',
    rate:     SOAK_RPS,
    timeUnit: '1s',
    duration: SOAK_DURATION_S + 's',
    ...vusFor(SOAK_RPS),
  };
  forceSubmetric('scenario:soak');
  forceSubmetric('half:first');
  forceSubmetric('half:second');
}

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios,
  thresholds,
  setupTimeout:    '60s',
  teardownTimeout: '5m',
};

// ── Setup ─────────────────────────────────────────────────────────────────────
export function setup() {
  console.log('Setup contra ' + BASE_URL + ' | modo=' + MODE);

  if (!ADMIN_EMAIL || !ADMIN_PASSWORD) {
    throw new Error('ERRO: ADMIN_EMAIL e ADMIN_PASSWORD são obrigatórios no .env');
  }

  const loginRes = http.post(
    BASE_URL + '/v1/auth/login',
    JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  if (loginRes.status !== 200) {
    throw new Error(
      'ERRO: login falhou (HTTP ' + loginRes.status + ') — verifique BASE_URL (' + BASE_URL +
      ') e as credenciais no .env. Corpo da resposta: ' + loginRes.body
    );
  }
  const token = loginRes.json('accessToken');
  if (!token) throw new Error('ERRO: login respondeu 200 mas sem accessToken — verifique o formato da resposta de /v1/auth/login');

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

  const modeInfo = MODE === 'staircase'
    ? 'degraus: ' + STEPS.join(', ') + ' req/s'
    : 'soak: ' + SOAK_RPS + ' req/s por ' + SOAK_DURATION_S + ' s';

  console.log('Setup OK — serviceId=' + serviceId + ' employeeId=' + employeeId +
              ' productId=' + productId + ' testUserId=' + testUserId +
              ' authUserId=' + authUserId + ' | ' + modeInfo);

  return {
    token, serviceId, employeeId, productId, testUserId, authUserId,
    testStartTime: Date.now(),
  };
}

// ── Default function ──────────────────────────────────────────────────────────
// No modo soak, cada requisição é tagueada com half=first/second (relativo à
// metade da janela de SOAK_DURATION_S) para o handleSummary poder comparar a
// latência entre as duas metades e verificar se ficou estável ou degradou.
// No modo staircase essa tag não é necessária (o k6 já tagueia por cenário).
function buildParams(data, h) {
  if (MODE !== 'soak') return { headers: h };
  const elapsedMs = Date.now() - data.testStartTime;
  const half = elapsedMs < (SOAK_DURATION_S * 1000) / 2 ? 'first' : 'second';
  return { headers: h, tags: { half } };
}

export default function (data) {
  const h = {
    'Content-Type':  'application/json',
    'Authorization': 'Bearer ' + data.token,
  };
  const params = buildParams(data, h);

  const r = Math.random();

  if (r < 0.05) {
    group('GET Ping', () => {
      const res = http.get(BASE_URL + '/ping', params);
      check(res, { 'status 200 - Ping': (v) => v.status === 200 });
    });

  } else if (r < 0.13) {
    group('GET Financial Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/financial?from=2026-06-01&to=2026-06-30', params);
      check(res, { 'status 200 - Financial Report': (v) => v.status === 200 });
    });

  } else if (r < 0.20) {
    group('GET Appointments Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/appointments?from=2026-06-01&to=2026-06-30', params);
      check(res, { 'status 200 - Appointments Report': (v) => v.status === 200 });
    });

  } else if (r < 0.24) {
    group('GET Payroll Report', () => {
      const res = http.get(BASE_URL + '/v1/reports/payroll?from=2026-06-01&to=2026-06-30', params);
      check(res, { 'status 200 - Payroll Report': (v) => v.status === 200 });
    });

  } else if (r < 0.27) {
    group('GET Appointments by Status', () => {
      const status = Math.random() < 0.5 ? 'PENDING' : 'CONFIRMED';
      const res = http.get(BASE_URL + '/v1/appointments?status=' + status + '&page=0&size=20', params);
      check(res, { 'status 200 - Appointments by Status': (v) => v.status === 200 });
    });

  } else if (r < 0.34) {
    group('GET All Appointments', () => {
      const res = http.get(BASE_URL + '/v1/appointments?page=0&size=20', params);
      check(res, { 'status 200 - List Appointments': (v) => v.status === 200 });
    });

  } else if (r < 0.41) {
    group('GET CashFlow', () => {
      const res = http.get(BASE_URL + '/v1/cashflow', params);
      check(res, { 'status 200 - CashFlow': (v) => v.status === 200 });
    });

  } else if (r < 0.45) {
    group('GET Users', () => {
      const res = http.get(BASE_URL + '/v1/users', params);
      check(res, { 'status 200 - Users': (v) => v.status === 200 });
    });

  } else if (r < 0.49) {
    group('GET Clients', () => {
      const res = http.get(BASE_URL + '/v1/clients', params);
      check(res, { 'status 200 - Clients': (v) => v.status === 200 });
    });

  } else if (r < 0.55) {
    group('GET Employees Booking', () => {
      const res = http.get(BASE_URL + '/v1/employees/booking', params);
      check(res, { 'status 200 - Employees Booking': (v) => v.status === 200 });
    });

  } else if (r < 0.60) {
    group('GET Products', () => {
      const res = http.get(BASE_URL + '/v1/products', params);
      check(res, { 'status 200 - Products': (v) => v.status === 200 });
    });

  } else if (r < 0.65) {
    group('GET Services', () => {
      const res = http.get(BASE_URL + '/v1/services', params);
      check(res, { 'status 200 - Services': (v) => v.status === 200 });
    });

  } else if (r < 0.73) {
    group('POST Create Appointment', () => {
      const res = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2099-10-15',
      }), params);
      check(res, { 'status 201 - Create Appointment': (v) => v.status === 201 });
    });

  } else if (r < 0.81) {
    group('POST CashFlow Entry', () => {
      const res = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type:        Math.random() < 0.5 ? 'INCOME' : 'EXPENSE',
        amount:      parseFloat((Math.random() * 100 + 1).toFixed(2)),
        description: 'k6 entry',
        date:        '2099-01-01',
      }), params);
      check(res, { 'status 201 - CashFlow Entry': (v) => v.status === 201 });
    });

  } else if (r < 0.86) {
    group('POST+DELETE CashFlow', () => {
      const cr = http.post(BASE_URL + '/v1/cashflow', JSON.stringify({
        type: 'EXPENSE', amount: 1.00, description: 'k6 del', date: '2099-01-01',
      }), params);
      if (cr.status === 201) {
        http.del(BASE_URL + '/v1/cashflow/' + cr.json('id'), null, params);
      }
    });

  } else if (r < 0.91) {
    group('POST+PATCH Cancel Appointment', () => {
      const cr = http.post(BASE_URL + '/v1/appointments', JSON.stringify({
        employeeId:    data.employeeId,
        serviceId:     data.serviceId,
        preferredDate: '2099-10-20',
      }), params);
      if (cr.status === 201) {
        http.patch(BASE_URL + '/v1/appointments/' + cr.json('id') + '/cancel', null, params);
      }
    });

  } else if (r < 0.95) {
    group('PUT Update Product', () => {
      const res = http.put(BASE_URL + '/v1/products/' + data.productId, JSON.stringify({
        name:   'Produto k6',
        stock:  9999,
        price:  parseFloat((Math.random() * 50 + 10).toFixed(2)),
        active: true,
      }), params);
      check(res, { 'status 200 - Update Product': (v) => v.status === 200 });
    });

  } else {
    group('PATCH Update User', () => {
      if (!data.testUserId) return;
      const res = http.patch(BASE_URL + '/v1/users/' + data.testUserId, JSON.stringify({
        phone: '839' + String(Math.floor(Math.random() * 90000000 + 10000000)),
      }), params);
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
function fmt(n) {
  return n == null ? 'N/A' : String(Math.round(n));
}

function renderStaircaseReport(data) {
  const results = extractAllSteps(data.metrics, STEPS, STEP_DURATION_S);
  const ceiling = findCeiling(results, SLA_MS);

  const ceilingLine = ceiling
    ? `O sistema sustenta **${ceiling.rps} req/s** com p(95) de ${fmt(ceiling.p95)} ms e ` +
      `${(ceiling.errRate * 100).toFixed(2)}% de erro — nesse ritmo, entregou **${fmt(ceiling.ok)} ` +
      `requisições bem-sucedidas em ${STEP_DURATION_S} s** de teste.`
    : `Nenhum degrau testado ficou dentro do SLA de ${SLA_MS} ms com erro < 1%. ` +
      `Reduza STEP_START e rode novamente para encontrar o teto real.`;

  const rows = results.map((s) => {
    const ok = s.p95 <= SLA_MS && s.errRate < 0.01 ? '✅' : '❌';
    return `| ${s.rps} req/s | ${s.achievedRps.toFixed(1)} req/s | ${fmt(s.total)} | ${fmt(s.ok)} | ` +
           `${(s.errRate * 100).toFixed(2)}% | ${fmt(s.p95)} ms | ${s.p99 != null ? fmt(s.p99) : 'N/A'} ms | ${ok} |`;
  }).join('\n');

  const totalReqs = results.reduce((acc, s) => acc + s.total, 0);
  const totalOk    = results.reduce((acc, s) => acc + s.ok, 0);

  const content =
`# Relatório de Carga — Fase Staircase (k6)

## Estratégia
Escada de carga com \`constant-arrival-rate\`: RPS fixo por degrau
(${STEPS.join(', ')} req/s), cada um sustentado por ${STEP_DURATION_S} s.
SLA único: p(95) ≤ ${SLA_MS} ms **e** erro HTTP < 1%. O teto de capacidade é
o maior RPS numa sequência ininterrupta de degraus aprovados desde o
primeiro degrau testado — um degrau que "passa" isoladamente depois de uma
falha anterior não conta (variância transitória, não capacidade real).

## Teto de capacidade (SLA ≤ ${SLA_MS} ms)
${ceilingLine}

## Resultado por degrau

| Alvo | RPS real | Req. totais | Req. OK | Erro HTTP | p(95) | p(99) | OK≤${SLA_MS}ms |
|------|----------|-------------|---------|-----------|-------|-------|---------|
${rows}

## Saúde Global
* **Total de requisições:** ${fmt(totalReqs)}
* **Requisições OK:** ${fmt(totalOk)}

---
*Gerado em ${new Date().toISOString()}*
`;

  return { content, ceiling };
}

function renderSoakReport(data) {
  const overall    = extractByTag(data.metrics, 'scenario:soak', SOAK_DURATION_S);
  const firstHalf   = extractByTag(data.metrics, 'half:first', SOAK_DURATION_S / 2);
  const secondHalf  = extractByTag(data.metrics, 'half:second', SOAK_DURATION_S / 2);
  const stability   = isSoakStable(firstHalf && firstHalf.p95, secondHalf && secondHalf.p95);

  // Critério de aprovação: p(95) geral dentro do SLA, erro desprezível, E a
  // SEGUNDA metade da janela (o "fim" do soak) também dentro do SLA — isso
  // captura degradação progressiva real que ameaçaria o SLA se o teste
  // continuasse. Não usamos a razão relativa (`stability.stable`) como veto
  // direto: um p(95) que cresce de 160ms para 350ms é "2x pior" mas continua
  // seguríssimo frente a um SLA de 1000ms — vetar isso reportaria um falso
  // negativo. A razão relativa fica só como informação de contexto.
  const secondHalfOk = !secondHalf || secondHalf.p95 <= SLA_MS;
  const passed = !!overall && overall.p95 <= SLA_MS && overall.errRate < 0.01 && secondHalfOk;

  const verdictLine = !overall
    ? 'Nenhum dado coletado — verifique se SOAK_RPS/SOAK_DURATION_S estão corretos.'
    : passed
      ? `✅ **CONFIRMADO**: ${SOAK_RPS} req/s é sustentável. Ao longo de ${SOAK_DURATION_S} s ` +
        `(${(SOAK_DURATION_S / 60).toFixed(1)} min), o sistema processou **${fmt(overall.ok)} requisições ` +
        `com sucesso (100%)**, p(95) geral de ${fmt(overall.p95)} ms.`
      : `❌ **NÃO CONFIRMADO**: ${SOAK_RPS} req/s não se sustentou pelos ${SOAK_DURATION_S} s inteiros ` +
        `(p(95)=${fmt(overall.p95)} ms, erro=${(overall.errRate * 100).toFixed(2)}%` +
        `${!secondHalfOk ? ', a segunda metade da janela já ultrapassou o SLA' : ''}).`;

  const stabilityLine = stability.stable == null
    ? 'Não foi possível comparar as duas metades da janela (dados insuficientes).'
    : `p(95) primeira metade: ${fmt(firstHalf && firstHalf.p95)} ms · p(95) segunda metade: ` +
      `${fmt(secondHalf && secondHalf.p95)} ms · razão: ${stability.ratio.toFixed(2)}x ` +
      `(informativo — só reprova o soak se a segunda metade sozinha já ultrapassar o SLA)`;

  const content =
`# Relatório de Carga — Fase Soak / Confirmação (k6)

## Estratégia
Sustenta um RPS fixo (SOAK_RPS=${SOAK_RPS}) por ${SOAK_DURATION_S} s inteiros,
para confirmar que um teto encontrado na escada é capacidade real —
não sorte de alguns segundos. Cada requisição é tagueada pela metade da
janela em que ocorreu (primeira/segunda), para detectar se a latência
degrada com o tempo (fila crescendo) mesmo com RPS constante.

## Veredito
${verdictLine}

## Estabilidade ao longo da janela
${stabilityLine}

## Detalhamento

| Métrica | Valor |
|---|---|
| RPS alvo | ${SOAK_RPS} req/s |
| RPS real | ${overall ? overall.achievedRps.toFixed(1) : 'N/A'} req/s |
| Duração | ${SOAK_DURATION_S} s (${(SOAK_DURATION_S / 60).toFixed(1)} min) |
| Requisições totais | ${overall ? fmt(overall.total) : 'N/A'} |
| Requisições OK | ${overall ? fmt(overall.ok) : 'N/A'} |
| Taxa de erro HTTP | ${overall ? (overall.errRate * 100).toFixed(2) + '%' : 'N/A'} |
| p(95) geral | ${overall ? fmt(overall.p95) + ' ms' : 'N/A'} |
| p(99) geral | ${overall && overall.p99 != null ? fmt(overall.p99) + ' ms' : 'N/A'} |

---
*Gerado em ${new Date().toISOString()}*
`;

  return { content, passed, overall };
}

export function handleSummary(data) {
  const safeData = JSON.parse(JSON.stringify(data));
  if (safeData.setup_data) safeData.setup_data.token = '[REDACTED]';

  const { content } = MODE === 'staircase' ? renderStaircaseReport(data) : renderSoakReport(data);

  return {
    stdout:        textSummary(data, { indent: ' ', enableColors: true }) + '\n\n' + content,
    [REPORT_PATH]: content,
    [RESULT_PATH]: JSON.stringify(safeData, null, 2),
  };
}
