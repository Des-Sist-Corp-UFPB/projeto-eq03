import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { X, Copy, Check, QrCode, ArrowRight, Loader2 } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { usersApi } from '../../pages/admin/users/services/users';
import { useAuth } from '../../hooks/useAuth';
import { getApiErrorMessage } from '../../utils/apiError';

interface PixPaymentModalProps {
  show: boolean;
  onHide: () => void;
  onGeneratePix: () => Promise<void>;
  pixQrCode: string | null;
  serviceName: string;
  price?: number | null;
  isGenerating?: boolean;
}

interface CpfFormData {
  cpf: string;
}

// Máscara ###.###.###-##
const formatCpf = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  return digits
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
};

export const PixPaymentModal = ({
  show,
  onHide,
  onGeneratePix,
  pixQrCode,
  serviceName,
  price,
  isGenerating = false,
}: PixPaymentModalProps) => {
  const { user, updateUserCpf } = useAuth();
  const [copied, setCopied] = useState(false);
  const [step, setStep] = useState<'cpf' | 'qr'>('cpf');
  const [isSavingCpf, setIsSavingCpf] = useState(false);
  const [cpfError, setCpfError] = useState('');

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CpfFormData>();

  const cpfValue = watch('cpf') || '';

  // Determina o step inicial com base no CPF do usuário em contexto
  useEffect(() => {
    if (show) {
      if (user?.cpf) {
        setStep('qr');
      } else {
        setStep('cpf');
      }
      setCpfError('');
    }
  }, [show, user?.cpf]);

  // Quando chegamos na etapa 'qr' e não há QR Code ainda, dispara a geração
  useEffect(() => {
    if (show && step === 'qr' && !pixQrCode && !isGenerating) {
      onGeneratePix();
    }
  }, [show, step, pixQrCode, isGenerating, onGeneratePix]);

  if (!show) return null;

  const handleCopy = async () => {
    if (!pixQrCode) return;
    try {
      await navigator.clipboard.writeText(pixQrCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Falha ao copiar PIX:', err);
    }
  };

  const handleCpfSubmit = async (data: CpfFormData) => {
    const rawCpf = data.cpf.replace(/\D/g, '');
    setIsSavingCpf(true);
    setCpfError('');
    try {
      await usersApi.updateMyCpf(rawCpf);
      // Atualiza o estado em memória sem recarregar a página
      updateUserCpf(rawCpf);
      setStep('qr');
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao salvar CPF. Tente novamente.');
      setCpfError(msg);
    } finally {
      setIsSavingCpf(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-x-hidden overflow-y-auto outline-none focus:outline-none">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-[#261f23]/40 backdrop-blur-md transition-opacity duration-300"
        onClick={onHide}
      />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-md mx-auto my-6 z-50 px-4">
        <div className="relative flex flex-col w-full bg-white border border-[#eae1e1] rounded-2xl shadow-xl outline-none focus:outline-none animate-scale-up">
          {/* Header */}
          <div className="flex items-center justify-between p-5 border-b border-solid border-[#eae1e1] rounded-t-2xl">
            <div className="flex items-center gap-2">
              <QrCode size={20} className="text-[#be8a83]" />
              <h3 className="text-lg font-semibold font-heading text-[#3b3036]">
                {step === 'cpf' ? 'Identificação para PIX' : 'Pagamento via PIX'}
              </h3>
            </div>
            <button
              type="button"
              onClick={onHide}
              className="p-1 ml-auto bg-transparent border-0 text-[#7a7074] hover:text-[#be8a83] float-right text-3xl leading-none font-semibold outline-none focus:outline-none transition-colors cursor-pointer"
            >
              <X size={20} />
            </button>
          </div>

          {/* ── ETAPA 0: Coleta de CPF (JIT) ── */}
          {step === 'cpf' && (
            <form onSubmit={handleSubmit(handleCpfSubmit)}>
              <div className="relative p-6 flex-auto flex flex-col gap-5">
                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-800">
                  <p className="font-semibold mb-1">CPF obrigatório para pagamento via PIX</p>
                  <p className="text-xs leading-relaxed">
                    O Banco Central exige o CPF do pagador para transações PIX. Seus dados ficam seguros e são usados apenas para processar o pagamento.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <label className="label-premium">
                    CPF <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    id="pix-cpf-input"
                    placeholder="000.000.000-00"
                    value={cpfValue}
                    {...register('cpf', {
                      required: 'CPF é obrigatório para gerar o PIX',
                      validate: (v) => {
                        const digits = v.replace(/\D/g, '');
                        return digits.length === 11 || 'CPF deve ter exatamente 11 dígitos';
                      },
                    })}
                    onChange={(e) => setValue('cpf', formatCpf(e.target.value))}
                    className={`input-premium ${errors.cpf || cpfError ? 'border-rose-300 focus:border-rose-500' : ''}`}
                    maxLength={14}
                    autoFocus
                  />
                  {errors.cpf && (
                    <span className="text-xs text-rose-500 font-semibold">{errors.cpf.message}</span>
                  )}
                  {cpfError && !errors.cpf && (
                    <span className="text-xs text-rose-500 font-semibold">{cpfError}</span>
                  )}
                </div>

                <p className="text-xs text-[#7a7074] leading-relaxed">
                  Você também pode cadastrar seu CPF permanentemente na página{' '}
                  <span className="text-[#be8a83] font-semibold">Meu Perfil</span> para não precisar informar nas próximas vezes.
                </p>
              </div>

              <div className="flex items-center justify-end p-5 border-t border-solid border-[#eae1e1] rounded-b-2xl bg-[#fcf9f9] gap-3">
                <button
                  type="button"
                  onClick={onHide}
                  className="px-4 py-2.5 border border-[#eae1e1] text-[#7a7074] font-semibold text-sm rounded-xl transition-all hover:bg-gray-50 cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSavingCpf}
                  className="flex items-center gap-2 px-5 py-2.5 bg-[#be8a83] hover:bg-[#a1706a] text-white shadow-md shadow-[#be8a83]/15 font-semibold text-sm rounded-xl transition-all duration-200 disabled:opacity-50 cursor-pointer"
                >
                  {isSavingCpf ? (
                    <Loader2 size={16} className="animate-spin" />
                  ) : (
                    <ArrowRight size={16} />
                  )}
                  {isSavingCpf ? 'Salvando...' : 'Continuar'}
                </button>
              </div>
            </form>
          )}

          {/* ── ETAPA 1: QR Code PIX ── */}
          {step === 'qr' && (
            <>
              <div className="relative p-6 flex-auto flex flex-col items-center text-center space-y-6">
                <div className="space-y-1">
                  <h4 className="font-bold text-[#3b3036] text-base">{serviceName}</h4>
                  {price != null && (
                    <p className="text-xl font-heading font-extrabold text-[#be8a83]">
                      R$ {price.toFixed(2)}
                    </p>
                  )}
                </div>

                {/* QR Code ou loading */}
                <div className="bg-[#fcf9f9] p-4 rounded-2xl border border-[#eae1e1] shadow-inner flex items-center justify-center min-h-[216px] w-full">
                  {isGenerating || !pixQrCode ? (
                    <div className="flex flex-col items-center gap-3 text-[#7a7074]">
                      <Loader2 size={40} className="animate-spin text-[#be8a83]" />
                      <p className="text-sm font-medium">Gerando QR Code...</p>
                    </div>
                  ) : (
                    <QRCodeSVG value={pixQrCode} size={200} level="M" includeMargin={true} />
                  )}
                </div>

                <p className="text-xs text-[#7a7074] leading-relaxed max-w-xs">
                  Abra o aplicativo do seu banco, escolha a opção <strong>Pagar via PIX (QR Code)</strong> e escaneie a imagem acima.
                </p>

                {pixQrCode && (
                  <div className="w-full space-y-2">
                    <div className="flex justify-between items-center text-xs font-semibold text-[#7a7074]/80 uppercase tracking-wider">
                      <span>PIX Copia e Cola</span>
                      <button
                        type="button"
                        onClick={handleCopy}
                        className="flex items-center gap-1 text-[#be8a83] hover:text-[#a1706a] transition-colors cursor-pointer"
                      >
                        {copied ? (
                          <>
                            <Check size={14} className="text-emerald-500" />
                            <span className="text-emerald-600 font-bold">Copiado!</span>
                          </>
                        ) : (
                          <>
                            <Copy size={14} />
                            <span>Copiar código</span>
                          </>
                        )}
                      </button>
                    </div>
                    <textarea
                      readOnly
                      value={pixQrCode}
                      rows={3}
                      className="w-full text-xs font-mono p-3 bg-gray-50 border border-[#eae1e1] rounded-xl outline-none focus:border-[#be8a83]/50 transition-colors resize-none select-all"
                      onClick={(e) => (e.target as HTMLTextAreaElement).select()}
                    />
                  </div>
                )}
              </div>

              <div className="flex items-center justify-end p-5 border-t border-solid border-[#eae1e1] rounded-b-2xl bg-[#fcf9f9]">
                <button
                  type="button"
                  onClick={onHide}
                  className="w-full px-4 py-2.5 bg-[#be8a83] hover:bg-[#a1706a] text-white shadow-md shadow-[#be8a83]/15 font-semibold text-sm rounded-xl transition-all duration-200"
                >
                  Fechar
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

