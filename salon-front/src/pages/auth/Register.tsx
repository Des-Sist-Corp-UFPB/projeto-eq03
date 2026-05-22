import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import { getApiErrorMessage } from '../../utils/apiError';
import { AlertCircle } from 'lucide-react';

interface RegisterFormData {
  name: string;
  email: string;
  phone?: string;
  password: string;
}

export const Register = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<RegisterFormData>();
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    setErrorMsg('');
    try {
      const response = await api.post('/auth/register', data);
      login(response.data.accessToken, response.data.refreshToken);
      navigate('/');
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao realizar cadastro. Tente novamente.');
      setErrorMsg(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center bg-gradient-to-br from-[#fcf9f9] to-[#f7ebeb] px-4 py-8">
      <div className="bg-white rounded-3xl border border-gray-100 shadow-xl overflow-hidden w-full max-w-4xl flex">
        {/* Left Side: Form */}
        <div className="w-full lg:w-1/2 px-6 py-12 sm:px-12 flex flex-col justify-center max-w-md mx-auto">
          <h2 className="font-heading text-3xl font-extrabold text-[#3b3036] tracking-tight">
            Cadastre-se
          </h2>
          <p className="text-sm text-[#3b3036]/60 mt-2 mb-8">
            Crie sua conta para agendar seus serviços.
          </p>

          {errorMsg && (
            <div className="mb-6 p-4 bg-rose-50 border border-rose-100 rounded-xl text-rose-700 text-sm flex items-start gap-2.5 animate-fadeIn">
              <AlertCircle size={18} className="shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <label className="label-premium">
                Nome Completo
              </label>
              <input
                type="text"
                placeholder="Seu nome completo"
                {...register('name', { required: 'Nome é obrigatório', minLength: { value: 3, message: 'Mínimo 3 caracteres'} })}
                className={`input-premium ${
                  errors.name ? 'border-rose-300 focus:border-rose-500' : ''
                }`}
              />
              {errors.name && (
                <span className="text-xs text-rose-500 font-semibold">{errors.name.message}</span>
              )}
            </div>

            <div className="space-y-1.5">
              <label className="label-premium">
                E-mail
              </label>
              <input
                type="email"
                placeholder="seuemail@exemplo.com"
                {...register('email', { required: 'Email é obrigatório' })}
                className={`input-premium ${
                  errors.email ? 'border-rose-300 focus:border-rose-500' : ''
                }`}
              />
              {errors.email && (
                <span className="text-xs text-rose-500 font-semibold">{errors.email.message}</span>
              )}
            </div>

            <div className="space-y-1.5">
              <label className="label-premium">
                Telefone (Opcional)
              </label>
              <input
                type="text"
                placeholder="(83) 99999-9999"
                {...register('phone')}
                className="input-premium"
              />
            </div>

            <div className="space-y-1.5">
              <label className="label-premium">
                Senha
              </label>
              <input
                type="password"
                placeholder="Sua senha (mín. 6 caracteres)"
                {...register('password', { required: 'Senha é obrigatória', minLength: { value: 6, message: 'Mínimo 6 caracteres'} })}
                className={`input-premium ${
                  errors.password ? 'border-rose-300 focus:border-rose-500' : ''
                }`}
              />
              {errors.password && (
                <span className="text-xs text-rose-500 font-semibold">{errors.password.message}</span>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="btn-premium w-full py-3 mt-6"
            >
              {isLoading ? 'Cadastrando...' : 'Criar minha conta'}
            </button>
            
            <div className="text-center pt-4 text-sm">
              <span className="text-gray-500">Já tem uma conta? </span>
              <Link to="/login" className="text-[#be8a83] font-semibold hover:underline">
                Entre aqui
              </Link>
            </div>
          </form>
        </div>

        {/* Right Side: Image (large screens only) */}
        <div className="hidden lg:block lg:w-1/2 bg-[url('https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1000&auto=format&fit=crop')] bg-center bg-cover"></div>
      </div>
    </div>
  );
};
