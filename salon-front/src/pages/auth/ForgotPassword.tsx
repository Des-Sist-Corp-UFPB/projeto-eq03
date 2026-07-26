import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import { getApiErrorMessage } from '../../utils/apiError';
import { AlertCircle, ArrowLeft, CheckCircle2 } from 'lucide-react';
import { forgotPasswordSchema } from './forgot-password.schema';
import type { ForgotPasswordFormValues } from './forgot-password.schema';

export const ForgotPassword = () => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({ resolver: zodResolver(forgotPasswordSchema) });
  const [errorMsg, setErrorMsg] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const onSubmit = async (data: ForgotPasswordFormValues) => {
    setErrorMsg('');
    try {
      await api.post('/auth/forgot-password', data);
      // Sempre mostra a mesma mensagem, exista ou não o e-mail — evita que alguém
      // descubra quais e-mails têm conta cadastrada só testando esse formulário.
      setSubmitted(true);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao solicitar redefinição de senha. Tente novamente.');
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
            Esqueci minha senha
          </h2>
          <p className="text-sm text-[#7a7074] mt-2">
            Informe o e-mail da sua conta e enviaremos um link para redefinir sua senha.
          </p>
        </div>

        {errorMsg && (
          <div className="p-4 bg-rose-50 border border-rose-100 rounded-xl text-rose-700 text-sm flex items-start gap-2.5 animate-fadeIn">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        {submitted ? (
          <div className="p-4 bg-emerald-50 border border-emerald-100 rounded-xl text-emerald-700 text-sm flex items-start gap-2.5 animate-fadeIn">
            <CheckCircle2 size={18} className="shrink-0 mt-0.5" />
            <span>
              Se esse e-mail estiver cadastrado, você vai receber um link de redefinição em
              alguns minutos. Verifique também a caixa de spam.
            </span>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
            <div className="space-y-1.5">
              <label className="label-premium">E-mail *</label>
              <input
                type="email"
                placeholder="seuemail@exemplo.com"
                {...register('email')}
                className={`input-premium ${errors.email ? 'border-rose-300 focus:border-rose-500' : ''}`}
              />
              {errors.email && (
                <span className="text-xs text-rose-500 font-semibold">{errors.email.message}</span>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 bg-[#be8a83] hover:bg-[#a1706a] text-[#fcf9f9] font-semibold rounded-xl text-sm transition-all shadow-md shadow-[#be8a83]/10 disabled:opacity-50 disabled:pointer-events-none cursor-pointer flex items-center justify-center gap-2"
            >
              {isSubmitting ? 'Enviando...' : 'Enviar link de redefinição'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
