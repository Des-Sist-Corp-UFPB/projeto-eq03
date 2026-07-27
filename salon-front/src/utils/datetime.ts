/**
 * Formatação das datas que vêm da API.
 *
 * O backend usa dois formatos, de propósito (ver TimeConfig.java):
 *
 *  - Hora local do salão — `"2026-07-28T22:00:00"`, sem fuso. É hora de relógio de parede:
 *    22h é 22h no salão, e deve ser exibida como 22h para qualquer pessoa, em qualquer lugar.
 *    O JavaScript interpreta esse formato (sem sufixo de fuso) como hora local do navegador,
 *    então exibir direto já dá o resultado certo.
 *
 *  - Instante de máquina — `"2026-07-28T22:00:00Z"`, com o `Z` de UTC. É "quando aconteceu",
 *    e aí sim deve aparecer no fuso de quem está olhando. `new Date()` converte sozinho.
 *
 * Ou seja: os dois casos funcionam com o mesmo código aqui. O que não pode voltar é o
 * deslocamento manual de fuso que existia no backend e fazia 22h virar 19h na tela.
 */

/** Data pura vinda da API (`"2026-07-28"`), sem hora. */
export function formatApiDate(value: string | null | undefined): string {
  if (!value) return '—';
  // O T12:00:00 evita o clássico erro de um dia a menos: uma data pura é interpretada como
  // UTC pelo JavaScript, e em fusos negativos isso cai no dia anterior.
  const date = new Date(`${value}T12:00:00`);
  if (Number.isNaN(date.getTime())) return 'Data inválida';
  return date.toLocaleDateString('pt-BR');
}

/** Data e hora vinda da API, em qualquer um dos dois formatos descritos acima. */
export function formatApiDateTime(
  value: string | number[] | null | undefined
): string {
  if (!value) return '—';

  let date: Date;
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value;
    date = new Date(year, month - 1, day, hour, minute);
  } else {
    date = new Date(value);
  }

  if (Number.isNaN(date.getTime())) return 'Data inválida';

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}
