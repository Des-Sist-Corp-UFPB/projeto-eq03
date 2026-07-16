import { z } from 'zod';

export const salonServiceFormSchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório').min(3, 'Mín. 3 caracteres'),
  description: z.string(),
  price: z.number().optional(),
  durationMin: z.number().optional(),
  durationEstimate: z.string().optional(),
  active: z.boolean(),
});

export type SalonServiceFormValues = z.infer<typeof salonServiceFormSchema>;
