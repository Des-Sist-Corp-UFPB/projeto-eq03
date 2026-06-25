/**
 * Ofusca um CPF para privacidade (LGPD).
 * Entrada: "123.456.789-00" ou "12345678900"
 * Saída: "***.***.789-00"
 */
export const maskCPF = (cpf?: string | null): string => {
  if (!cpf) return '';
  const cleanCpf = cpf.replace(/\D/g, '');
  if (cleanCpf.length !== 11) return cpf;

  const part3 = cleanCpf.slice(6, 9);
  const part4 = cleanCpf.slice(9, 11);

  return `***.***.${part3}-${part4}`;
};
