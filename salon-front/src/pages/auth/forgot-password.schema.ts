import { z } from 'zod';

export const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email é obrigatório').email('Formato de e-mail inválido'),
});

export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
