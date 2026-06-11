import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { profileApi } from './services/profile';
import { useAuth } from '../../hooks/useAuth';
import type { UserUpdateRequest } from '../admin/users/services/users';
import { Save, User as UserIcon } from 'lucide-react';
import { useAlert } from '../../hooks/useAlert';
import { getApiErrorMessage } from '../../utils/apiError';

export const Profile = () => {
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const { register, handleSubmit, setValue } = useForm<UserUpdateRequest>();

  const { error: showError, success: showSuccess } = useAlert();

  useEffect(() => {
    const loadProfile = async () => {
      if (!user?.userId) return;

      try {
        const data = await profileApi.getProfileById(user.userId);
        setValue('name', data.name);
        setValue('email', data.email);
        setValue('phone', data.phone || '');
      } catch (err) {
        const msg = getApiErrorMessage(err, 'Erro ao carregar os dados do perfil.');
        await showError(msg);
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [user, setValue]);

  const onSubmit = async (data: UserUpdateRequest) => {
    if (!user?.userId) return;

    setIsSaving(true);
    try {
      const updateData = { ...data };
      if (!updateData.password) {
        delete updateData.password;
      }

      await profileApi.updateProfile(user.userId, updateData);
      await showSuccess('Perfil atualizado com sucesso!');
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao atualizar perfil.');
      await showError(msg);
    } finally {
      setIsSaving(false);
    }
  };

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
              <label className="label-premium">Nome Completo</label>
              <input
                type="text"
                {...register('name', { required: true })}
                className="input-premium"
              />
            </div>

            <div className="space-y-1.5">
              <label className="label-premium">Telefone</label>
              <input
                type="tel"
                {...register('phone')}
                placeholder="(11) 99999-9999"
                className="input-premium"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="label-premium">E-mail</label>
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

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="label-premium">Nova Senha</label>
              <input
                type="password"
                {...register('password')}
                placeholder="Deixe em branco para não alterar"
                className="input-premium"
              />
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
