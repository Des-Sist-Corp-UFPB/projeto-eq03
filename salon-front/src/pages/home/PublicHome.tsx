import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Sparkles, CalendarHeart, Scissors, MapPin, Phone, AtSign, MessageCircle, Clock } from 'lucide-react';
import { salonProfileService, DAY_ORDER, DAY_LABELS } from '../../services/salonProfile';
import type { SalonProfileData } from '../../services/salonProfile';

export const PublicHome = () => {
  const [profile, setProfile] = useState<SalonProfileData | null>(null);

  useEffect(() => {
    // Best-effort: se falhar, a home segue funcionando normalmente sem essa seção — não é
    // crítico o suficiente para travar a página inicial com um erro.
    salonProfileService
      .getPublic()
      .then(setProfile)
      .catch(() => setProfile(null));
  }, []);

  const hasContactInfo = !!(profile?.address || profile?.phone || profile?.instagram || profile?.whatsapp);

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-20 space-y-16">
      {/* Hero Section */}
      <section className="text-center space-y-6 max-w-2xl mx-auto animate-fadeIn">
        <p className="text-xs font-semibold uppercase tracking-widest text-[#be8a83]">
          Espaço Cristiane Moura
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
      <section
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 animate-fadeIn"
        style={{ animationDelay: '150ms' }}
      >
        <div className="bg-white rounded-2xl border border-gray-100 p-6 text-center space-y-3 shadow-xs transition-all duration-300">
          <div className="mx-auto bg-[#be8a83]/10 text-[#be8a83] rounded-full p-3.5 w-fit">
            <Sparkles size={24} />
          </div>
          <h3 className="font-heading font-bold text-lg text-[#3b3036]">Atendimento cuidadoso</h3>
          <p className="text-sm text-[#3b3036]/60 leading-relaxed">
            Equipe dedicada para destacar o melhor em você.
          </p>
        </div>

        <div className="bg-white rounded-2xl border border-gray-100 p-6 text-center space-y-3 shadow-xs transition-all duration-300">
          <div className="mx-auto bg-[#be8a83]/10 text-[#be8a83] rounded-full p-3.5 w-fit">
            <CalendarHeart size={24} />
          </div>
          <h3 className="font-heading font-bold text-lg text-[#3b3036]">Agendamento online</h3>
          <p className="text-sm text-[#3b3036]/60 leading-relaxed">
            Escolha serviço, profissional e horário em poucos passos.
          </p>
        </div>

        <div className="bg-white rounded-2xl border border-gray-100 p-6 text-center space-y-3 shadow-xs transition-all duration-300 sm:col-span-2 lg:col-span-1">
          <div className="mx-auto bg-[#be8a83]/10 text-[#be8a83] rounded-full p-3.5 w-fit">
            <Scissors size={24} />
          </div>
          <h3 className="font-heading font-bold text-lg text-[#3b3036]">Serviços variados</h3>
          <p className="text-sm text-[#3b3036]/60 leading-relaxed">
            Tratamentos pensados para realçar sua beleza natural.
          </p>
        </div>
      </section>

      {/* Sobre / Contato / Horário de Funcionamento (issue #117 + #116) */}
      {profile && (profile.description || hasContactInfo || profile.businessHours.length > 0) && (
        <section
          className="grid grid-cols-1 md:grid-cols-2 gap-8 animate-fadeIn bg-white rounded-2xl border border-gray-100 shadow-xs p-6 md:p-8"
          style={{ animationDelay: '300ms' }}
        >
          <div className="space-y-4">
            <h3 className="font-heading text-xl font-bold text-[#3b3036]">Sobre o {profile.name}</h3>
            {profile.description && (
              <p className="text-sm text-[#3b3036]/70 leading-relaxed whitespace-pre-wrap">
                {profile.description}
              </p>
            )}
            {hasContactInfo && (
              <div className="space-y-2 pt-2">
                {profile.address && (
                  <div className="flex items-start gap-2 text-sm text-[#3b3036]/70">
                    <MapPin size={16} className="shrink-0 mt-0.5 text-[#be8a83]" />
                    <span>{profile.address}</span>
                  </div>
                )}
                {profile.phone && (
                  <div className="flex items-center gap-2 text-sm text-[#3b3036]/70">
                    <Phone size={16} className="shrink-0 text-[#be8a83]" />
                    <span>{profile.phone}</span>
                  </div>
                )}
                {profile.whatsapp && (
                  <div className="flex items-center gap-2 text-sm text-[#3b3036]/70">
                    <MessageCircle size={16} className="shrink-0 text-[#be8a83]" />
                    <span>{profile.whatsapp}</span>
                  </div>
                )}
                {profile.instagram && (
                  <div className="flex items-center gap-2 text-sm text-[#3b3036]/70">
                    <AtSign size={16} className="shrink-0 text-[#be8a83]" />
                    <span>{profile.instagram}</span>
                  </div>
                )}
              </div>
            )}
          </div>

          {profile.businessHours.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Clock size={18} className="text-[#be8a83]" />
                <h3 className="font-heading text-xl font-bold text-[#3b3036]">Horário de Funcionamento</h3>
              </div>
              <div className="divide-y divide-gray-50">
                {DAY_ORDER.map((day) => {
                  const hour = profile.businessHours.find((bh) => bh.dayOfWeek === day);
                  return (
                    <div key={day} className="flex justify-between py-1.5 text-sm">
                      <span className="text-[#3b3036]/70">{DAY_LABELS[day]}</span>
                      <span className="font-semibold text-[#3b3036]">
                        {hour?.open && hour.openTime && hour.closeTime
                          ? `${hour.openTime.slice(0, 5)} às ${hour.closeTime.slice(0, 5)}`
                          : 'Fechado'}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </section>
      )}
    </div>
  );
};
