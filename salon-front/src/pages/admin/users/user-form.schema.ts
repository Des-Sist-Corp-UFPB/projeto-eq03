import { z } from 'zod';

export const userFormSchema = z
  .object({
    _isEdit: z.boolean().optional(),
    name: z.string().min(1, 'Nome é obrigatório').min(3, 'Mínimo 3 caracteres'),
    email: z
      .string()
      .min(1, 'Email é obrigatório')
      .refine(
        (v) => /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(v),
        'Formato de e-mail inválido'
      ),
    phone: z
      .string()
      .optional()
      .refine(
        (v) => !v || /^\(?\d{2}\)?\s?\d{4,5}-?\d{4}$/.test(v),
        'Formato inválido. Use (XX) XXXXX-XXXX'
      ),
    password: z.string().optional(),
    confirmPassword: z.string().optional(),
    active: z.boolean().optional(),
    roleId: z.number().optional(),
  })
  .superRefine((data, ctx) => {
    const isEdit = !!data._isEdit;
    const hasConfirmPassword = !!data.confirmPassword;

    const isPasswordRequired = !isEdit || hasConfirmPassword;
    if (!data.password) {
      if (isPasswordRequired) {
        ctx.addIssue({ code: 'custom', message: 'Senha é obrigatória', path: ['password'] });
      }
    } else {
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
    }

    const hasPassword = !!data.password;
    const isConfirmRequired = !isEdit || hasPassword;
    if (isConfirmRequired) {
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
    } else if (data.confirmPassword && data.confirmPassword !== data.password) {
      ctx.addIssue({
        code: 'custom',
        message: 'As senhas não coincidem',
        path: ['confirmPassword'],
      });
    }
  });

export type UserFormValues = z.infer<typeof userFormSchema>;
