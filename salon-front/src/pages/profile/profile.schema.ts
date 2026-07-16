import { z } from 'zod';

export const profileFormSchema = z
  .object({
    name: z.string().min(1, 'Nome é obrigatório').min(3, 'Mínimo 3 caracteres'),
    email: z.string().optional(),
    phone: z
      .string()
      .optional()
      .refine(
        (v) => !v || /^\(?\d{2}\)?\s?\d{4,5}-?\d{4}$/.test(v),
        'Formato inválido. Use (XX) XXXXX-XXXX'
      ),
    cpf: z
      .string()
      .optional()
      .refine(
        (v) => !v || v.replace(/\D/g, '').length === 11,
        'CPF deve ter exatamente 11 dígitos'
      ),
    password: z.string().optional(),
    confirmPassword: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.password) {
      if (data.password.length < 8) {
        ctx.addIssue({
          code: 'custom',
          message: 'A senha deve ter no mínimo 8 caracteres',
          path: ['password'],
        });
      } else if (!/\d/.test(data.password)) {
        ctx.addIssue({
          code: 'custom',
          message: 'A senha deve conter pelo menos um número',
          path: ['password'],
        });
      }
      if (!data.confirmPassword) {
        ctx.addIssue({
          code: 'custom',
          message: 'Confirmação de senha é obrigatória',
          path: ['confirmPassword'],
        });
      } else if (data.confirmPassword !== data.password) {
        ctx.addIssue({
          code: 'custom',
          message: 'As senhas não coincidem',
          path: ['confirmPassword'],
        });
      }
    }
  });

export type ProfileFormValues = z.infer<typeof profileFormSchema>;
