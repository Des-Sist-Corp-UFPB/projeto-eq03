import { z } from 'zod';

export const cashFlowFormSchema = z.object({
  type: z.enum(['INCOME', 'EXPENSE']),
  amount: z
    .string()
    .min(1, 'Valor é obrigatório')
    .refine((v) => Number(v) >= 0.01, 'Valor inválido'),
  description: z.string().min(1, 'Descrição é obrigatória'),
  date: z.string().min(1, 'Data é obrigatória'),
});

export type CashFlowFormValues = z.infer<typeof cashFlowFormSchema>;
