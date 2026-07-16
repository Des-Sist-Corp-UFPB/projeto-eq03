import { z } from 'zod';

export const pixCpfFormSchema = z.object({
  cpf: z.string().optional(),
});

export type PixCpfFormValues = z.infer<typeof pixCpfFormSchema>;
