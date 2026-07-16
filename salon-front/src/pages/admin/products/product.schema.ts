import { z } from 'zod';

export const productFormSchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório').min(3, 'Mín. 3 caracteres'),
  stock: z
    .string()
    .min(1, 'Estoque é obrigatório')
    .refine((v) => Number(v) >= 0, 'Não pode ser negativo'),
  price: z
    .string()
    .min(1, 'Preço é obrigatório')
    .refine((v) => Number(v) >= 0, 'Não pode ser negativo'),
  active: z.boolean(),
});

export type ProductFormValues = z.infer<typeof productFormSchema>;
