import { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Sparkles, Eye, EyeOff, PlugZap, CheckCircle2, XCircle } from 'lucide-react';
import {
  aiConfigService,
  AI_MODELS,
  type AiConfigData,
  type AiConfigUpdatePayload,
  type AiConfigTestResult,
} from '../../services/aiConfig';
import { useAlert } from '../../hooks/useAlert';
import { getApiErrorMessage } from '../../utils/apiError';
import { McpTokensSection } from './McpTokensSection';
import { aiConfigFormSchema } from './aiConfig.schema';
import type { AiConfigFormValues } from './aiConfig.schema';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const AiConfig = () => {
  const [config, setConfig] = useState<AiConfigData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [showApiKey, setShowApiKey] = useState(false);
  const [isTesting, setIsTesting] = useState(false);
  const [testResult, setTestResult] = useState<AiConfigTestResult | null>(null);

  const { error: showError, success: showSuccess } = useAlert();

  const {
    register,
    handleSubmit,
    reset,
    getValues,
    formState: { errors },
  } = useForm<AiConfigFormValues>({ resolver: zodResolver(aiConfigFormSchema) });

  const loadConfig = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await aiConfigService.get();
      setConfig(data);
      reset({
        baseUrl: data.baseUrl,
        model: data.model,
        apiKey: '',
        temperature: data.temperature,
        maxTokens: data.maxTokens,
        enabled: data.enabled,
        dailyCallBudget: data.dailyCallBudget,
      });
    } catch (err) {
      showError(getApiErrorMessage(err, 'Erro ao carregar a configuração de IA.'));
    } finally {
      setIsLoading(false);
    }
  }, [reset, showError]);

  useEffect(() => {
    loadConfig();
  }, [loadConfig]);

  const onSubmit = async (data: AiConfigFormValues) => {
    setIsSaving(true);
    try {
      const payload: AiConfigUpdatePayload = {
        baseUrl: data.baseUrl,
        model: data.model,
        apiKey: data.apiKey ? data.apiKey : null,
        temperature: Number(data.temperature),
        maxTokens: Number(data.maxTokens),
        enabled: data.enabled,
        dailyCallBudget: Number(data.dailyCallBudget),
      };
      const updated = await aiConfigService.update(payload);
      setConfig(updated);
      reset({
        baseUrl: updated.baseUrl,
        model: updated.model,
        apiKey: '',
        temperature: updated.temperature,
        maxTokens: updated.maxTokens,
        enabled: updated.enabled,
        dailyCallBudget: updated.dailyCallBudget,
      });
      showSuccess('Configuração de IA atualizada com sucesso.');
    } catch (err) {
      showError(getApiErrorMessage(err, 'Erro ao salvar a configuração de IA.'));
    } finally {
      setIsSaving(false);
    }
  };

  const handleTestConnection = async () => {
    const { baseUrl, model, apiKey } = getValues();
    setIsTesting(true);
    setTestResult(null);
    try {
      const result = await aiConfigService.testConnection({
        baseUrl,
        model,
        apiKey: apiKey ? apiKey : null,
      });
      setTestResult(result);
    } catch (err) {
      setTestResult({
        success: false,
        message: getApiErrorMessage(err, 'Erro ao testar a conexão.'),
        latencyMs: null,
      });
    } finally {
      setIsTesting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-[#be8a83]"></div>
        <span className="text-sm text-[#3b3036]/60 font-medium font-sans">
          Carregando configuração de IA...
        </span>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6 animate-fade-in-up">
      <div className="flex items-center gap-3">
        <Sparkles size={32} className="text-[#be8a83]" />
        <div>
          <h2 className="font-heading text-2xl font-bold text-[#3b3036] tracking-wide">
            Central de IA
          </h2>
          <p className="text-sm text-[#3b3036]/60 mt-1">
            Configuração do provedor de IA usado pelo motor de recomendações e pelo servidor MCP.
            A API key nunca é exibida em texto puro depois de salva.
          </p>
        </div>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="bg-white rounded-2xl border border-[#eae1e1]/80 p-6 space-y-5 shadow-sm"
      >
        <div className="space-y-1.5">
          <label className={labelCls}>URL base do provedor *</label>
          <input
            type="text"
            placeholder="https://llm.rodrigor.com"
            {...register('baseUrl')}
            className={`${inputCls} ${errors.baseUrl ? 'border-rose-300 focus:border-rose-500' : ''}`}
          />
          {errors.baseUrl && (
            <span className="text-xs text-rose-500 font-semibold">{errors.baseUrl.message}</span>
          )}
        </div>

        <div className="space-y-1.5">
          <label className={labelCls}>Modelo *</label>
          <select {...register('model')} className={inputCls}>
            {AI_MODELS.map((model) => (
              <option key={model} value={model}>
                {model}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1.5">
          <label className={labelCls}>
            API key
            {config?.apiKeyConfigured && (
              <span className="ml-2 text-xs font-normal text-[#3b3036]/50">
                (atual: {config.apiKeyMasked})
              </span>
            )}
          </label>
          <div className="relative">
            <input
              type={showApiKey ? 'text' : 'password'}
              placeholder={config?.apiKeyConfigured ? 'Deixe em branco para manter a atual' : 'sk-...'}
              {...register('apiKey')}
              className={`${inputCls} pr-10`}
            />
            <button
              type="button"
              onClick={() => setShowApiKey(!showApiKey)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 cursor-pointer flex items-center"
            >
              {showApiKey ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="space-y-1.5">
            <label className={labelCls}>Temperatura (0–1) *</label>
            <input
              type="number"
              step="0.1"
              min="0"
              max="1"
              {...register('temperature')}
              className={inputCls}
            />
          </div>
          <div className="space-y-1.5">
            <label className={labelCls}>Máx. de tokens *</label>
            <input
              type="number"
              min="50"
              max="4000"
              {...register('maxTokens')}
              className={inputCls}
            />
          </div>
          <div className="space-y-1.5">
            <label className={labelCls}>Orçamento diário (chamadas) *</label>
            <input
              type="number"
              min="1"
              {...register('dailyCallBudget')}
              className={inputCls}
            />
          </div>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-[#eae1e1]/80">
          <span className="text-sm font-semibold text-[#3b3036]">Recomendações de IA ativas</span>
          <label className="relative inline-flex items-center cursor-pointer">
            <input type="checkbox" className="sr-only peer" {...register('enabled')} />
            <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#be8a83] shadow-inner"></div>
          </label>
        </div>

        {config?.updatedBy && (
          <p className="text-xs text-[#3b3036]/50">
            Última atualização por {config.updatedBy}
            {config.updatedAt ? ` em ${new Date(config.updatedAt).toLocaleString('pt-BR')}` : ''}
          </p>
        )}

        {testResult && (
          <div
            className={`flex items-start gap-2 rounded-xl p-3 text-sm font-medium min-w-0 ${
              testResult.success
                ? 'bg-emerald-50 text-emerald-700'
                : 'bg-rose-50 text-rose-700'
            }`}
          >
            {testResult.success ? (
              <CheckCircle2 size={18} className="shrink-0 mt-0.5" />
            ) : (
              <XCircle size={18} className="shrink-0 mt-0.5" />
            )}
            <span className="min-w-0 break-words [overflow-wrap:anywhere]">
              {testResult.message}
              {testResult.latencyMs != null ? ` (${testResult.latencyMs}ms)` : ''}
            </span>
          </div>
        )}

        <div className="flex gap-3">
          <button
            type="button"
            onClick={handleTestConnection}
            disabled={isTesting}
            className="flex-1 py-3 bg-white border border-[#be8a83] hover:bg-[#fdf6f5] text-[#be8a83] font-semibold rounded-xl text-sm transition-all disabled:opacity-50 disabled:pointer-events-none cursor-pointer flex items-center justify-center gap-2"
          >
            <PlugZap size={16} />
            {isTesting ? 'Testando...' : 'Testar conexão'}
          </button>
          <button
            type="submit"
            disabled={isSaving}
            className="flex-1 py-3 bg-[#be8a83] hover:bg-[#a1706a] text-[#fcf9f9] font-semibold rounded-xl text-sm transition-all disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
          >
            {isSaving ? 'Salvando...' : 'Salvar configuração'}
          </button>
        </div>
      </form>

      <McpTokensSection />
    </div>
  );
};
