import { z } from 'zod';

const businessHourSchema = z
  .object({
    dayOfWeek: z.enum(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']),
    open: z.boolean(),
    openTime: z.string().nullable(),
    closeTime: z.string().nullable(),
  })
  .superRefine((val, ctx) => {
    if (!val.open) return;
    if (!val.openTime) {
      ctx.addIssue({ code: 'custom', message: 'Informe o horário de abertura', path: ['openTime'] });
    }
    if (!val.closeTime) {
      ctx.addIssue({ code: 'custom', message: 'Informe o horário de fechamento', path: ['closeTime'] });
    }
    if (val.openTime && val.closeTime && val.openTime >= val.closeTime) {
      ctx.addIssue({
        code: 'custom',
        message: 'O horário de abertura deve ser antes do fechamento',
        path: ['closeTime'],
      });
    }
  });

export const salonProfileFormSchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório').max(150, 'Máximo de 150 caracteres'),
  description: z.string().max(2000, 'Máximo de 2000 caracteres').optional().or(z.literal('')),
  address: z.string().max(300, 'Máximo de 300 caracteres').optional().or(z.literal('')),
  phone: z.string().max(20, 'Máximo de 20 caracteres').optional().or(z.literal('')),
  instagram: z.string().max(150, 'Máximo de 150 caracteres').optional().or(z.literal('')),
  whatsapp: z.string().max(20, 'Máximo de 20 caracteres').optional().or(z.literal('')),
  businessHours: z.array(businessHourSchema).length(7, 'Os 7 dias da semana precisam estar presentes'),
});

export type SalonProfileFormValues = z.infer<typeof salonProfileFormSchema>;
