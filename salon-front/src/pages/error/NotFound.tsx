import { useNavigate } from 'react-router-dom';

export const NotFound = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[#fcf9f9] px-4 py-8">
      <h1 className="text-8xl font-extrabold text-[#be8a83] tracking-tighter">404</h1>
      <h2 className="text-2xl font-bold text-[#3b3036] mt-4 mb-2">Página não encontrada</h2>
      <p className="text-[#3b3036]/60 text-center mb-8 max-w-md text-sm leading-relaxed">
        A página que você está procurando pode ter sido removida, <br />
        renomeada ou está temporariamente indisponível.
      </p>
      <button 
        onClick={() => navigate('/')}
        className="px-6 py-2.5 bg-[#be8a83] text-white hover:bg-[#a6726b] font-semibold text-sm rounded-full transition-all active:scale-[0.98]"
      >
        Voltar para o Início
      </button>
    </div>
  );
};
