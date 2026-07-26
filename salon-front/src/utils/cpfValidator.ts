/** Valida CPF de verdade (dígitos verificadores), espelhando CpfValidator do backend. */
export function isValidCpf(cpf: string | null | undefined): boolean {
  if (!cpf) return false;
  const clean = cpf.replace(/\D/g, '');
  if (clean.length !== 11) return false;

  if (/^(\d)\1{10}$/.test(clean)) return false;

  const digits = clean.split('').map(Number);

  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += digits[i] * (10 - i);
  }
  let r1 = 11 - (sum % 11);
  const d1 = r1 === 10 || r1 === 11 ? 0 : r1;
  if (d1 !== digits[9]) return false;

  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += digits[i] * (11 - i);
  }
  let r2 = 11 - (sum % 11);
  const d2 = r2 === 10 || r2 === 11 ? 0 : r2;
  return d2 === digits[10];
}
