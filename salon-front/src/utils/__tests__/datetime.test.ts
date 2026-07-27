import { describe, it, expect } from 'vitest';
import { formatApiDate, formatApiDateTime } from '../datetime';

describe('formatApiDateTime', () => {
  it('exibe a hora local do salão exatamente como veio, sem deslocar fuso', () => {
    // Regressão do bug relatado: escolher 22h na tela administrativa e a listagem mostrar 19h.
    // O formato sem sufixo de fuso é hora de parede — 22h tem que continuar 22h.
    expect(formatApiDateTime('2026-07-28T22:00:00')).toMatch(/^28\/07\/2026,? 22:00$/);
  });

  it('não desloca horários próximos da virada do dia', () => {
    // 23h era o caso em que o deslocamento de -3h jogava a data para o dia anterior.
    expect(formatApiDateTime('2026-07-28T23:30:00')).toMatch(/^28\/07\/2026,? 23:30$/);
    expect(formatApiDateTime('2026-07-28T00:15:00')).toMatch(/^28\/07\/2026,? 00:15$/);
  });

  it('aceita o formato de array que algumas respostas ainda usam', () => {
    expect(formatApiDateTime([2026, 7, 28, 22, 0])).toMatch(/^28\/07\/2026,? 22:00$/);
  });

  it('devolve travessão para valor ausente e aviso para valor inválido', () => {
    expect(formatApiDateTime(null)).toBe('—');
    expect(formatApiDateTime(undefined)).toBe('—');
    expect(formatApiDateTime('nao-e-data')).toBe('Data inválida');
  });
});

describe('formatApiDate', () => {
  it('não perde um dia ao formatar data pura', () => {
    // Sem o meio-dia fixado, o JavaScript lê "2026-07-28" como UTC e em fuso negativo
    // (Recife é UTC-3) isso exibiria 27/07.
    expect(formatApiDate('2026-07-28')).toBe('28/07/2026');
  });

  it('trata começo e fim de mês corretamente', () => {
    expect(formatApiDate('2026-01-01')).toBe('01/01/2026');
    expect(formatApiDate('2026-12-31')).toBe('31/12/2026');
  });

  it('devolve travessão para valor ausente', () => {
    expect(formatApiDate(null)).toBe('—');
  });
});
