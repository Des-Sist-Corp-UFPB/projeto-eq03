import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import { getApiErrorMessage } from '../../utils/apiError';
import { AlertCircle, ArrowLeft } from 'lucide-react';

interface LoginFormData {
  email: string;
  password: string;
}

export const Login = () => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>();
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

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    setErrorMsg('');
    try {
      const response = await api.post('/auth/login', data);
      login(response.data.accessToken, response.data.refreshToken, true);

      const pending = localStorage.getItem('pending_appointment');
      navigate(pending ? '/appointment' : '/');
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao realizar login. Tente novamente.');
      setErrorMsg(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex flex-col md:flex-row bg-white overflow-hidden">
      {/* Left Side: Carousel Slideshow of the business */}
      <div className="hidden md:flex md:w-1/2 h-screen relative flex-col justify-between p-12 text-white md:animate-slide-image-to-left overflow-hidden">
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
            Sua beleza refletida nos mínimos detalhes.
          </h1>
          <p className="text-[#fcf9f9]/90 text-sm max-w-md font-sans tracking-wide drop-shadow-lg">
            Agende serviços de alta qualidade com nossas profissionais especializadas em um ambiente
            elegante e acolhedor.
          </p>
        </div>

        {/* Footer/Copyright inside image */}
        <div className="z-20 text-xs text-[#fcf9f9]/70 drop-shadow-md">
          © {new Date().getFullYear()} Espaço Cristiane Moura. Todos os direitos reservados.
        </div>
      </div>

      {/* Right Side: Form */}
      <div className="w-full md:w-1/2 min-h-screen bg-white flex flex-col justify-center px-6 py-12 sm:px-16 lg:px-24 relative overflow-y-auto md:animate-slide-form-to-right">
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
              Bem-vinda de volta
            </h2>
            <p className="text-sm text-[#7a7074] mt-2">
              Faça login para gerenciar seus agendamentos e acessar sua conta.
            </p>
          </div>

          {errorMsg && (
            <div className="p-4 bg-rose-50 border border-rose-100 rounded-xl text-rose-700 text-sm flex items-start gap-2.5 animate-fadeIn">
              <AlertCircle size={18} className="shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
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
              <label className="label-premium">Senha</label>
              <input
                type="password"
                placeholder="Sua senha"
                {...register('password', { required: 'Senha é obrigatória' })}
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
              className="w-full py-3 bg-[#be8a83] hover:bg-[#a1706a] text-white font-semibold rounded-xl text-sm transition-all shadow-md shadow-[#be8a83]/10 disabled:opacity-50 disabled:pointer-events-none cursor-pointer flex items-center justify-center gap-2"
            >
              {isLoading ? 'Acessando...' : 'Entrar na minha conta'}
            </button>

            <div className="text-center pt-4 text-sm">
              <span className="text-[#7a7074]">Não tem uma conta? </span>
              <Link to="/register" className="text-[#be8a83] font-semibold hover:underline">
                Cadastre-se
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
