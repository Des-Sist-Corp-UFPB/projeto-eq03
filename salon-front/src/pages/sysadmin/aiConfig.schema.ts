import { z } from 'zod';

export const aiConfigFormSchema = z.object({
  baseUrl: z
    .string()
    .min(1, 'A URL base é obrigatória')
    .refine((v) => /^https:\/\//.test(v), 'A URL deve começar com https://'),
  model: z.string().min(1),
  apiKey: z.string().optional(),
  temperature: z.number().min(0).max(1),
  maxTokens: z.number().min(50).max(4000),
  enabled: z.boolean(),
  dailyCallBudget: z.number().min(1),
});

export type AiConfigFormValues = z.infer<typeof aiConfigFormSchema>;
