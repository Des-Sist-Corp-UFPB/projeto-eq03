import { z } from 'zod';

export const loginFormSchema = z.object({
  email: z.string().min(1, 'Email é obrigatório'),
  password: z.string().min(1, 'Senha é obrigatória'),
});

export type LoginFormValues = z.infer<typeof loginFormSchema>;
