import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import { getApiErrorMessage } from '../../utils/apiError';
import { AlertCircle, ArrowLeft } from 'lucide-react';

interface RegisterFormData {
  name: string;
  email: string;
  phone?: string;
  password: string;
}

export const Register = () => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>();
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const images = [
    '/images/salon1.png',
    '/images/salon2.png',
    '/images/salon3.png',
    '/images/salon4.jpg',
  ];
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentImageIndex((prev) => (prev + 1) % images.length);
    }, 5000);
    return () => clearInterval(interval);
  }, []);

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
    <div className="min-h-screen w-full flex flex-col md:flex-row-reverse bg-white overflow-hidden">
      {/* Left Side (in code) / Right Side (visual due to flex-row-reverse): Photo of the business */}
      <div className="hidden md:flex md:w-1/2 h-screen relative flex-col justify-between p-12 text-white md:animate-slide-image-to-right overflow-hidden">
        {/* Background Images Cross-Fade */}
        {images.map((img, index) => (
          <div
            key={img}
            className={`absolute inset-0 bg-center bg-cover transition-opacity duration-1000 ${
              index === currentImageIndex ? 'opacity-100' : 'opacity-0'
            }`}
            style={{ backgroundImage: `url(${img})` }}
          />
        ))}

        {/* Deep obsidian gradient backdrop with high-contrast text overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-[#0b0f17]/95 via-[#0b0f17]/60 to-[#0b0f17]/40 backdrop-blur-[2px] z-10" />

        {/* Logo/Brand Title */}
        <div className="z-20 drop-shadow-lg">
          <Link
            to="/"
            className="font-heading text-3xl font-bold tracking-wider text-white hover:text-[#e5a49c] transition-colors"
          >
            ESPAÇO CRISTIANE MOURA
          </Link>
        </div>

        {/* Brand Tagline */}
        <div className="z-20 space-y-4">
          <h1 className="font-heading text-4xl lg:text-5xl font-light leading-tight drop-shadow-2xl">
            Seja bem-vinda ao seu momento de autocuidado.
          </h1>
          <p className="text-[#fcf9f9]/90 text-sm max-w-md font-sans tracking-wide drop-shadow-lg">
            Crie sua conta para ter acesso rápido a agendamentos, histórico de serviços e promoções
            exclusivas.
          </p>
        </div>

        {/* Footer/Copyright inside image */}
        <div className="z-20 text-xs text-[#fcf9f9]/70 drop-shadow-md">
          © {new Date().getFullYear()} Espaço Cristiane Moura. Todos os direitos reservados.
        </div>
      </div>

      {/* Right Side (in code) / Left Side (visual due to flex-row-reverse): Form */}
      <div className="w-full md:w-1/2 min-h-screen bg-white flex flex-col justify-center px-6 py-12 sm:px-16 lg:px-24 relative overflow-y-auto md:animate-slide-form-to-left">
        {/* Back Button */}
        <Link
          to="/"
          className="absolute top-6 left-6 sm:left-12 flex items-center gap-2 text-sm text-[#7a7074] hover:text-[#3b3036] font-semibold transition-colors group"
        >
          <ArrowLeft
            size={16}
            className="transform group-hover:-translate-x-1 transition-transform"
          />
          Voltar para o início
        </Link>

        <div className="w-full max-w-md mx-auto space-y-8">
          <div>
            <h2 className="font-heading text-3xl font-bold text-[#3b3036] tracking-tight">
              Cadastre-se
            </h2>
            <p className="text-sm text-[#7a7074] mt-2">
              Crie sua conta para agendar seus serviços em poucos cliques.
            </p>
          </div>

          {errorMsg && (
            <div className="p-4 bg-rose-50 border border-rose-100 rounded-xl text-rose-700 text-sm flex items-start gap-2.5 animate-fadeIn">
              <AlertCircle size={18} className="shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <label className="label-premium">Nome Completo</label>
              <input
                type="text"
                placeholder="Seu nome completo"
                {...register('name', {
                  required: 'Nome é obrigatório',
                  minLength: { value: 3, message: 'Mínimo 3 caracteres' },
                })}
                className={`input-premium ${
                  errors.name ? 'border-rose-300 focus:border-rose-500' : ''
                }`}
              />
              {errors.name && (
                <span className="text-xs text-rose-500 font-semibold">{errors.name.message}</span>
              )}
            </div>

            <div className="space-y-1.5">
              <label className="label-premium">E-mail</label>
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
              <label className="label-premium">Telefone (Opcional)</label>
              <input
                type="text"
                placeholder="(83) 99999-9999"
                {...register('phone')}
                className="input-premium"
              />
            </div>

            <div className="space-y-1.5">
              <label className="label-premium">Senha</label>
              <input
                type="password"
                placeholder="Sua senha (mín. 6 caracteres)"
                {...register('password', {
                  required: 'Senha é obrigatória',
                  minLength: { value: 6, message: 'Mínimo 6 caracteres' },
                })}
                className={`input-premium ${
                  errors.password ? 'border-rose-300 focus:border-rose-500' : ''
                }`}
              />
              {errors.password && (
                <span className="text-xs text-rose-500 font-semibold">
                  {errors.password.message}
                </span>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 bg-[#be8a83] hover:bg-[#a1706a] text-white font-semibold rounded-xl text-sm transition-all shadow-md shadow-[#be8a83]/10 disabled:opacity-50 disabled:pointer-events-none cursor-pointer flex items-center justify-center gap-2 mt-6"
            >
              {isLoading ? 'Cadastrando...' : 'Criar minha conta'}
            </button>

            <div className="text-center pt-4 text-sm">
              <span className="text-[#7a7074]">Já tem uma conta? </span>
              <Link to="/login" className="text-[#be8a83] font-semibold hover:underline">
                Entre aqui
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
