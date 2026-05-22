import { Link } from 'react-router-dom';
import { Sparkles, CalendarHeart, Scissors } from 'lucide-react';

export const PublicHome = () => {
  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-20 space-y-16">
      {/* Hero Section */}
      <section className="text-center space-y-6 max-w-2xl mx-auto animate-fadeIn">
        <p className="text-xs font-semibold uppercase tracking-widest text-[#be8a83]">
          Salão Cristiane
        </p>
        <h1 className="font-heading text-4xl sm:text-5xl font-extrabold text-[#3b3036] tracking-tight leading-tight">
          Beleza e bem-estar no seu ritmo
        </h1>
        <p className="text-base sm:text-lg text-[#3b3036]/60 max-w-lg mx-auto leading-relaxed">
          Conheça nossos serviços e reserve um horário com quem mais entende do seu estilo.
        </p>
        <div className="flex flex-wrap justify-center gap-4 pt-4">
          <Link 
            to="/appointment" 
            className="inline-flex items-center gap-2 px-6 py-3 bg-[#be8a83] text-white hover:bg-[#a6726b] font-semibold text-sm rounded-full transition-all active:translate-y-0"
          >
            <CalendarHeart size={18} />
            Agendar agora
          </Link>
          <Link 
            to="/services" 
            className="inline-flex items-center gap-2 px-6 py-3 border border-[#be8a83] text-[#be8a83] hover:bg-[#be8a83]/5 font-semibold text-sm rounded-full transition-all active:translate-y-0"
          >
            <Scissors size={18} />
            Ver serviços
          </Link>
        </div>
      </section>

      {/* Features Grid */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 animate-fadeIn" style={{ animationDelay: '150ms' }}>
        <div className="bg-white rounded-2xl border border-gray-100 p-6 text-center space-y-3 shadow-xs transition-all duration-300">
          <div className="mx-auto bg-[#be8a83]/10 text-[#be8a83] rounded-full p-3.5 w-fit">
            <Sparkles size={24} />
          </div>
          <h3 className="font-heading font-bold text-lg text-[#3b3036]">
            Atendimento cuidadoso
          </h3>
          <p className="text-sm text-[#3b3036]/60 leading-relaxed">
            Equipe dedicada para destacar o melhor em você.
          </p>
        </div>

        <div className="bg-white rounded-2xl border border-gray-100 p-6 text-center space-y-3 shadow-xs transition-all duration-300">
          <div className="mx-auto bg-[#be8a83]/10 text-[#be8a83] rounded-full p-3.5 w-fit">
            <CalendarHeart size={24} />
          </div>
          <h3 className="font-heading font-bold text-lg text-[#3b3036]">
            Agendamento online
          </h3>
          <p className="text-sm text-[#3b3036]/60 leading-relaxed">
            Escolha serviço, profissional e horário em poucos passos.
          </p>
        </div>

        <div className="bg-white rounded-2xl border border-gray-100 p-6 text-center space-y-3 shadow-xs transition-all duration-300 sm:col-span-2 lg:col-span-1">
          <div className="mx-auto bg-[#be8a83]/10 text-[#be8a83] rounded-full p-3.5 w-fit">
            <Scissors size={24} />
          </div>
          <h3 className="font-heading font-bold text-lg text-[#3b3036]">
            Serviços variados
          </h3>
          <p className="text-sm text-[#3b3036]/60 leading-relaxed">
            Tratamentos pensados para realçar sua beleza natural.
          </p>
        </div>
      </section>
    </div>
  );
};
