// ─────────────────────────────────────────────────────────────────────────────
// Lógica pura de análise de capacidade — compartilhada entre o script k6
// (carga.js, roda no runtime goja do k6) e os scripts Node do orquestrador
// (loadtest/lib/*.mjs, rodam no Node.js real).
//
// Por isso este arquivo NÃO pode usar nenhuma API específica de runtime
// (sem `fs`, sem `http` do k6, sem `__ENV`) — só funções puras sobre
// estruturas de dados simples, para os dois lados calcularem exatamente
// a mesma coisa a partir dos mesmos dados, sem duplicar a regra em dois
// lugares.
// ─────────────────────────────────────────────────────────────────────────────

function metricValues(metrics, key) {
  return metrics[key] ? metrics[key].values : null;
}

/**
 * Extrai {total, ok, errRate, p95, p99, achievedRps} a partir de qualquer
 * expressão de tag k6 (ex.: "scenario:step_40" ou "half:first"). Retorna
 * `null` se a submétrica não existir ou não tiver dados — o que acontece
 * quando o degrau/tag não foi executado, ou quando não há um threshold
 * trivial forçando aquela submétrica a aparecer em `data.metrics`.
 */
export function extractByTag(metrics, tagExpr, durationS) {
  const d = metricValues(metrics, 'http_req_duration{' + tagExpr + '}');
  const f = metricValues(metrics, 'http_req_failed{' + tagExpr + '}');
  const t = metricValues(metrics, 'http_reqs{' + tagExpr + '}');
  if (!d || !f || !t || d['p(95)'] == null || t.count === 0) return null;

  const total = t.count;
  // http_req_failed é uma métrica Rate que soma 1 quando a requisição FALHA.
  // "passes" (Rate contou 1/verdadeiro) = nº de FALHAS; "fails" (contou
  // 0/falso) = nº de requisições OK. Nomenclatura genérica do k6, não
  // confundir com sucesso/falha do teste.
  const ok = f.fails != null ? f.fails : Math.round(total * (1 - f.rate));

  return {
    tag: tagExpr,
    total,
    ok,
    errRate: f.rate,
    p95: d['p(95)'],
    p99: d['p(99)'] != null ? d['p(99)'] : null,
    achievedRps: durationS ? total / durationS : null,
  };
}

/** Varia de `extractByTag` para o caso específico de um degrau `step_<rps>`. */
export function extractStepStats(metrics, rps, durationS) {
  const r = extractByTag(metrics, 'scenario:step_' + rps, durationS);
  return r ? { ...r, rps } : null;
}

/** Extrai as estatísticas de todos os degraus em `stepsList`, na ordem dada. */
export function extractAllSteps(metrics, stepsList, durationS) {
  return stepsList
    .map((rps) => extractStepStats(metrics, rps, durationS))
    .filter(Boolean);
}

/**
 * Varre as chaves de `metrics` (o objeto `data.metrics` de um resumo k6, ou o
 * `metrics` de um resultado.json exportado) procurando submétricas tagueadas
 * por cenário no formato `step_<rps>` e devolve a lista de RPS testados,
 * ordenada crescente. Usado pelos scripts do orquestrador, que não têm de
 * antemão a lista de degraus daquela fase específica.
 */
export function discoverSteps(metrics) {
  const found = new Set();
  const re = /^http_reqs\{scenario:step_(\d+)\}$/;
  for (const key of Object.keys(metrics || {})) {
    const m = re.exec(key);
    if (m) found.add(parseInt(m[1], 10));
  }
  return Array.from(found).sort((a, b) => a - b);
}

/**
 * Encontra o teto de capacidade: o maior RPS em uma sequência ININTERRUPTA
 * de degraus aprovados desde o primeiro degrau testado.
 *
 * Por quê exigir sequência ininterrupta: sob contenção de recursos (ex.: pool
 * de conexões saturado), é comum o sistema falhar num degrau e "se recuperar"
 * num degrau mais alto por variância transitória (GC, pool liberando um
 * instante). Um degrau que passa isoladamente DEPOIS de uma falha anterior
 * não representa capacidade confiável — foi sorte, não sustentação real.
 *
 * `steps` deve estar ordenado crescente por rps.
 */
export function findCeiling(steps, slaMs) {
  let best = null;
  for (const s of steps) {
    if (s.p95 <= slaMs && s.errRate < 0.01) {
      best = s;
    } else {
      break;
    }
  }
  return best;
}

/**
 * Verifica se a latência se manteve estável ao longo de uma janela de soak,
 * comparando o p(95) da primeira metade com o da segunda metade. Se a
 * segunda metade for significativamente mais lenta, a carga não é
 * sustentável de verdade — está degradando com o tempo (fila crescendo).
 *
 * `toleranceRatio` = quanto a segunda metade pode ser mais lenta que a
 * primeira sem ser considerado degradação (1.2 = até 20% mais lento é
 * tolerado como ruído normal).
 */
export function isSoakStable(firstHalfP95, secondHalfP95, toleranceRatio = 1.2) {
  if (firstHalfP95 == null || secondHalfP95 == null) {
    return { stable: null, ratio: null };
  }
  const ratio = firstHalfP95 > 0 ? secondHalfP95 / firstHalfP95 : 1;
  return { stable: ratio <= toleranceRatio, ratio };
}
