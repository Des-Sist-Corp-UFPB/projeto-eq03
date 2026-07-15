import { useState, useEffect, useCallback } from 'react';
import { Lightbulb, DollarSign, Users, RefreshCw } from 'lucide-react';
import {
  recommendationsService,
  type RecommendationResult,
  type RecommendationType,
  type RecommendationPriority,
} from '../../../services/recommendations';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

const priorityStyles: Record<RecommendationPriority, string> = {
  ALTA: 'bg-rose-50 text-rose-700 border-rose-100',
  MEDIA: 'bg-amber-50 text-amber-700 border-amber-100',
  BAIXA: 'bg-emerald-50 text-emerald-700 border-emerald-100',
};

const priorityLabels: Record<RecommendationPriority, string> = {
  ALTA: 'Alta prioridade',
  MEDIA: 'Média prioridade',
  BAIXA: 'Baixa prioridade',
};

interface SectionConfig {
  type: RecommendationType;
  title: string;
  description: string;
  icon: typeof DollarSign;
}

const sections: SectionConfig[] = [
  {
    type: 'FINANCEIRO',
    title: 'Financeiro e ocupação',
    description: 'Insights sobre faturamento, horários ociosos e distribuição entre profissionais.',
    icon: DollarSign,
  },
  {
    type: 'RETENCAO',
    title: 'Retenção de clientes',
    description: 'Clientes sem agendamento há bastante tempo e sugestões de reengajamento.',
    icon: Users,
  },
];

const RecommendationSection = ({ config }: { config: SectionConfig }) => {
  const [result, setResult] = useState<RecommendationResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isGenerating, setIsGenerating] = useState(false);
  const { error: showError } = useAlert();
  const Icon = config.icon;

  const loadCached = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await recommendationsService.getLatest(config.type);
      setResult(data);
    } catch (err: any) {
      if (err.response?.status !== 404) {
        showError(getApiErrorMessage(err, 'Erro ao carregar recomendações.'));
      }
      setResult(null);
    } finally {
      setIsLoading(false);
    }
  }, [config.type, showError]);

  useEffect(() => {
    loadCached();
  }, [loadCached]);

  const handleGenerate = async () => {
    setIsGenerating(true);
    try {
      const data = await recommendationsService.generate(config.type);
      setResult(data);
    } catch (err) {
      showError(getApiErrorMessage(err, 'Erro ao gerar recomendações. Tente novamente.'));
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-6 shadow-sm space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <Icon size={22} className="text-[#be8a83] mt-0.5 shrink-0" />
          <div>
            <h3 className="font-heading text-lg font-bold text-[#3b3036]">{config.title}</h3>
            <p className="text-sm text-[#3b3036]/60">{config.description}</p>
          </div>
        </div>
        <button
          onClick={handleGenerate}
          disabled={isGenerating}
          className="shrink-0 flex items-center gap-2 px-4 py-2 bg-[#be8a83] hover:bg-[#a1706a] text-[#fcf9f9] text-sm font-semibold rounded-xl transition-all disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
        >
          <RefreshCw size={16} className={isGenerating ? 'animate-spin' : ''} />
          {isGenerating ? 'Gerando...' : result ? 'Atualizar' : 'Gerar'}
        </button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-[#be8a83]"></div>
        </div>
      ) : result ? (
        <div className="space-y-3">
          {result.items.map((item, idx) => (
            <div key={idx} className="border border-[#eae1e1]/80 rounded-xl p-4 space-y-1.5">
              <div className="flex items-center justify-between gap-2">
                <span className="font-semibold text-[#3b3036] text-sm">{item.title}</span>
                <span
                  className={`text-xs font-semibold px-2 py-0.5 rounded-full border shrink-0 ${priorityStyles[item.priority]}`}
                >
                  {priorityLabels[item.priority]}
                </span>
              </div>
              <p className="text-sm text-[#3b3036]/70">{item.description}</p>
              <p className="text-xs text-[#be8a83] font-semibold">Ação sugerida: {item.suggestedAction}</p>
            </div>
          ))}
          <p className="text-xs text-[#3b3036]/40">
            {result.fromCache ? 'Última geração' : 'Gerado agora'} em{' '}
            {new Date(result.generatedAt).toLocaleString('pt-BR')}
          </p>
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center py-8 gap-2 text-center">
          <Lightbulb size={28} className="text-gray-300" />
          <span className="text-sm text-[#3b3036]/60">
            Nenhuma recomendação gerada ainda. Clique em "Gerar" para começar.
          </span>
        </div>
      )}
    </div>
  );
};

export const Recommendations = () => {
  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in-up">
      <div className="flex items-center gap-3">
        <Lightbulb size={32} className="text-[#be8a83]" />
        <div>
          <h2 className="font-heading text-2xl font-bold text-[#3b3036] tracking-wide">
            Recomendações de IA
          </h2>
          <p className="text-sm text-[#3b3036]/60 mt-1">
            Insights gerados por IA a partir dos dados do salão. Cada geração consome uma chamada
            do orçamento configurado na Central de IA.
          </p>
        </div>
      </div>

      <div className="space-y-6">
        {sections.map((section) => (
          <RecommendationSection key={section.type} config={section} />
        ))}
      </div>
    </div>
  );
};
