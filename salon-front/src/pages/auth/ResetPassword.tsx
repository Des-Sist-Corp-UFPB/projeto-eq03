import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../../services/api';
import { getApiErrorMessage } from '../../utils/apiError';
import { AlertCircle, ArrowLeft, Eye, EyeOff } from 'lucide-react';
import { resetPasswordSchema } from './reset-password.schema';
import type { ResetPasswordFormValues } from './reset-password.schema';

export const ResetPassword = () => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({ resolver: zodResolver(resetPasswordSchema) });
  const [errorMsg, setErrorMsg] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const onSubmit = async (data: ResetPasswordFormValues) => {
    setErrorMsg('');
    if (!token) {
      setErrorMsg('Link de redefinição inválido. Solicite um novo link.');
      return;
    }
    try {
      await api.post('/auth/reset-password', { token, newPassword: data.password });
      navigate('/login', { state: { passwordResetSuccess: true } });
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Link de redefinição inválido ou expirado. Solicite um novo link.');
      setErrorMsg(msg);
    }
  };

  return (
    <div className="min-h-screen w-full bg-white flex flex-col justify-center px-6 py-12 sm:px-16 lg:px-24 relative">
      <Link
        to="/login"
        className="absolute top-6 left-6 sm:left-12 flex items-center gap-2 text-sm text-[#7a7074] hover:text-[#3b3036] font-semibold transition-colors group"
      >
        <ArrowLeft size={16} className="transform group-hover:-translate-x-1 transition-transform" />
        Voltar para o login
      </Link>

      <div className="w-full max-w-md mx-auto space-y-8">
        <div>
          <h2 className="font-heading text-3xl font-bold text-[#3b3036] tracking-tight">
            Redefinir senha
          </h2>
          <p className="text-sm text-[#7a7074] mt-2">Escolha uma nova senha para sua conta.</p>
        </div>

        {!token && (
          <div className="p-4 bg-amber-50 border border-amber-100 rounded-xl text-amber-700 text-sm flex items-start gap-2.5">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <span>
              Link de redefinição inválido ou incompleto.{' '}
              <Link to="/forgot-password" className="font-semibold hover:underline">
                Solicite um novo link
              </Link>
              .
            </span>
          </div>
        )}

        {errorMsg && (
          <div className="p-4 bg-rose-50 border border-rose-100 rounded-xl text-rose-700 text-sm flex items-start gap-2.5 animate-fadeIn">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <label className="label-premium">Nova Senha *</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Mínimo 8 caracteres com 1 número"
                {...register('password')}
                className={`input-premium pr-10 ${errors.password ? 'border-rose-300 focus:border-rose-500' : ''}`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none cursor-pointer flex items-center"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {errors.password && (
              <span className="text-xs text-rose-500 font-semibold">{errors.password.message}</span>
            )}
          </div>

          <div className="space-y-1.5">
            <label className="label-premium">Confirmar Nova Senha *</label>
            <div className="relative">
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Confirme sua nova senha"
                {...register('confirmPassword')}
                className={`input-premium pr-10 ${errors.confirmPassword ? 'border-rose-300 focus:border-rose-500' : ''}`}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none cursor-pointer flex items-center"
              >
                {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {errors.confirmPassword && (
              <span className="text-xs text-rose-500 font-semibold">
                {errors.confirmPassword.message}
              </span>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting || !token}
            className="w-full py-3 bg-[#be8a83] hover:bg-[#a1706a] text-[#fcf9f9] font-semibold rounded-xl text-sm transition-all shadow-md shadow-[#be8a83]/10 disabled:opacity-50 disabled:pointer-events-none cursor-pointer flex items-center justify-center gap-2 mt-6"
          >
            {isSubmitting ? 'Salvando...' : 'Redefinir senha'}
          </button>
        </form>
      </div>
    </div>
  );
};
