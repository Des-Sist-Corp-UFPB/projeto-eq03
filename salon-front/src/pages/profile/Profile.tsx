import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { profileApi } from './services/profile';
import { useAuth } from '../../hooks/useAuth';
import type { UserUpdateRequest } from '../admin/users/services/users';
import { Save, User as UserIcon } from 'lucide-react';
import { useAlert } from '../../hooks/useAlert';
import { getApiErrorMessage } from '../../utils/apiError';

interface ProfileFormData extends UserUpdateRequest {
  cpf?: string;
}

// Aplica máscara ###.###.###-## enquanto o usuário digita
const formatCpf = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  return digits
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
};

export const Profile = () => {
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    setError,
    formState: { errors },
  } = useForm<ProfileFormData>();

  const { error: showError, success: showSuccess } = useAlert();

  useEffect(() => {
    const loadProfile = async () => {
      if (!user?.userId) return;

      try {
        const data = await profileApi.getProfileById(user.userId);
        setValue('name', data.name);
        setValue('email', data.email);
        setValue('phone', data.phone || '');
        if (data.cpf) {
          setValue('cpf', formatCpf(data.cpf));
        }
      } catch (err) {
        const msg = getApiErrorMessage(err, 'Erro ao carregar os dados do perfil.');
        await showError(msg);
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [user, setValue]);

  const onSubmit = async (data: ProfileFormData) => {
    if (!user?.userId) return;

    setIsSaving(true);
    try {
      const updateData: UserUpdateRequest = { ...data };
      if (!updateData.password) {
        delete updateData.password;
      }
      // Remove máscara antes de enviar ao backend (somente dígitos)
      if (updateData.cpf) {
        updateData.cpf = updateData.cpf.replace(/\D/g, '');
      }

      await profileApi.updateProfile(user.userId, updateData);
      await showSuccess('Perfil atualizado com sucesso!');
    } catch (error: any) {
      if (error.response?.status === 400 && error.response.data?.errors) {
        const fieldErrors = error.response.data.errors;
        Object.keys(fieldErrors).forEach((field) => {
          setError(field as any, { type: 'server', message: fieldErrors[field] });
        });
      } else if (error.response?.status === 409) {
        const msg = error.response.data?.message || 'E-mail ou CPF já cadastrado.';
        if (msg.toLowerCase().includes('cpf')) {
          setError('cpf', { type: 'server', message: msg });
        } else {
          setError('email', { type: 'server', message: msg });
        }
      } else {
        const msg = getApiErrorMessage(error, 'Erro ao atualizar perfil.');
        await showError(msg);
      }
    } finally {
      setIsSaving(false);
    }
  };

  const cpfValue = watch('cpf') || '';

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-[#be8a83]"></div>
        <p className="text-sm text-[#3b3036]/60 font-medium">Carregando perfil...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <h2 className="font-heading text-2xl font-bold text-[#3b3036] tracking-wide">Meu Perfil</h2>

      <div className="bg-white rounded-2xl border border-gray-100 p-6 shadow-xs space-y-6">
        <div className="flex items-center gap-4 pb-5 border-b border-gray-100">
          <div className="bg-[#be8a83]/10 text-[#be8a83] rounded-full p-4 shrink-0">
            <UserIcon size={32} />
          </div>
          <div>
            <h4 className="font-semibold text-[#3b3036] text-lg">{user?.email}</h4>
            <p className="text-sm text-[#3b3036]/60">Atualize suas informações pessoais</p>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="label-premium">
                Nome Completo <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register('name', {
                  required: 'Nome é obrigatório',
                  minLength: { value: 3, message: 'Mínimo 3 caracteres' },
                })}
                className={`input-premium ${errors.name ? 'border-rose-300 focus:border-rose-500' : ''}`}
              />
              {errors.name && (
                <span className="text-xs text-rose-500 font-semibold">{errors.name.message}</span>
              )}
            </div>

            <div className="space-y-1.5">
              <label className="label-premium">Telefone</label>
              <input
                type="tel"
                {...register('phone', {
                  pattern: {
                    value: /^$|^\(?\d{2}\)?\s?\d{4,5}-?\d{4}$/,
                    message: 'Formato inválido. Use (XX) XXXXX-XXXX',
                  },
                })}
                placeholder="(11) 99999-9999"
                className={`input-premium ${errors.phone ? 'border-rose-300 focus:border-rose-500' : ''}`}
              />
              {errors.phone && (
                <span className="text-xs text-rose-500 font-semibold">{errors.phone.message}</span>
              )}
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="label-premium">
              E-mail <span className="text-rose-500">*</span>
            </label>
            <input
              type="email"
              {...register('email', { required: true })}
              disabled
              className="input-premium bg-gray-100 text-gray-500 cursor-not-allowed opacity-60"
            />
            <p className="text-xs text-gray-400">
              O email não pode ser alterado, pois é usado para login.
            </p>
          </div>

          {/* CPF — opcional, coletado preferencialmente no momento do PIX */}
          <div className="space-y-1.5">
            <label className="label-premium">
              CPF{' '}
              <span className="text-xs text-[#7a7074] font-normal">(Opcional — necessário para pagamentos via PIX)</span>
            </label>
            <input
              type="text"
              placeholder="000.000.000-00"
              value={cpfValue}
              {...register('cpf', {
                validate: (v) => {
                  if (!v) return true;
                  const digits = v.replace(/\D/g, '');
                  return digits.length === 11 || 'CPF deve ter exatamente 11 dígitos';
                },
              })}
              onChange={(e) => setValue('cpf', formatCpf(e.target.value))}
              className={`input-premium ${errors.cpf ? 'border-rose-300 focus:border-rose-500' : ''}`}
              maxLength={14}
            />
            {errors.cpf && (
              <span className="text-xs text-rose-500 font-semibold">{errors.cpf.message}</span>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="label-premium">Nova Senha</label>
              <input
                type="password"
                {...register('password', {
                  validate: (val) => {
                    if (!val) return true;
                    if (val.length < 8) return 'A senha deve ter no mínimo 8 caracteres';
                    if (!/\d/.test(val)) return 'A senha deve conter pelo menos um número';
                    return true;
                  },
                })}
                placeholder="Deixe em branco para não alterar"
                className={`input-premium ${errors.password ? 'border-rose-300 focus:border-rose-500' : ''}`}
              />
              {errors.password && (
                <span className="text-xs text-rose-500 font-semibold">{errors.password.message}</span>
              )}
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button type="submit" disabled={isSaving} className="btn-premium disabled:opacity-50">
              {isSaving ? (
                <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
              ) : (
                <Save size={18} />
              )}
              Salvar Alterações
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
