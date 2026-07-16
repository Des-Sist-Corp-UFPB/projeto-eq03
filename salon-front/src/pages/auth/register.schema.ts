import { z } from 'zod';

export const registerFormSchema = z
  .object({
    name: z.string().min(1, 'Nome é obrigatório').min(3, 'Mínimo 3 caracteres'),
    email: z
      .string()
      .min(1, 'Email é obrigatório')
      .email('Formato de e-mail inválido'),
    phone: z
      .string()
      .optional()
      .refine(
        (v) => !v || /^\(?\d{2}\)?\s?\d{4,5}-?\d{4}$/.test(v),
        'Formato inválido. Use (XX) XXXXX-XXXX ou (XX) XXXX-XXXX'
      ),
    password: z
      .string()
      .min(1, 'Senha é obrigatória')
      .min(8, 'A senha deve ter no mínimo 8 caracteres')
      .regex(/\d/, 'A senha deve conter pelo menos um número'),
    confirmPassword: z.string().min(1, 'Confirmação de senha é obrigatória'),
  })
  .superRefine((data, ctx) => {
    if (data.confirmPassword !== data.password) {
      ctx.addIssue({
        code: 'custom',
        message: 'As senhas não coincidem',
        path: ['confirmPassword'],
      });
    }
  });

export type RegisterFormValues = z.infer<typeof registerFormSchema>;
