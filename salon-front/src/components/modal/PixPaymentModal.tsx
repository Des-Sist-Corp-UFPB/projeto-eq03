import { useState } from 'react';
import { X, Copy, Check, QrCode } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

interface PixPaymentModalProps {
  show: boolean;
  onHide: () => void;
  pixQrCode: string;
  serviceName: string;
  price?: number | null;
}

export const PixPaymentModal = ({
  show,
  onHide,
  pixQrCode,
  serviceName,
  price,
}: PixPaymentModalProps) => {
  const [copied, setCopied] = useState(false);

  if (!show) return null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(pixQrCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Falha ao copiar PIX:', err);
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
              <h3 className="text-lg font-semibold font-heading text-[#3b3036]">Pagamento via PIX</h3>
            </div>
            <button
              type="button"
              onClick={onHide}
              className="p-1 ml-auto bg-transparent border-0 text-[#7a7074] hover:text-[#be8a83] float-right text-3xl leading-none font-semibold outline-none focus:outline-none transition-colors cursor-pointer"
            >
              <X size={20} />
            </button>
          </div>

          {/* Body */}
          <div className="relative p-6 flex-auto flex flex-col items-center text-center space-y-6">
            <div className="space-y-1">
              <h4 className="font-bold text-[#3b3036] text-base">{serviceName}</h4>
              {price != null && (
                <p className="text-xl font-heading font-extrabold text-[#be8a83]">
                  R$ {price.toFixed(2)}
                </p>
              )}
            </div>

            {/* QR Code container */}
            <div className="bg-[#fcf9f9] p-4 rounded-2xl border border-[#eae1e1] shadow-inner flex items-center justify-center">
              <QRCodeSVG value={pixQrCode} size={200} level="M" includeMargin={true} />
            </div>

            <p className="text-xs text-[#7a7074] leading-relaxed max-w-xs">
              Abra o aplicativo do seu banco, escolha a opção <strong>Pagar via PIX (QR Code)</strong> e escaneie a imagem acima.
            </p>

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
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end p-5 border-t border-solid border-[#eae1e1] rounded-b-2xl bg-[#fcf9f9]">
            <button
              type="button"
              onClick={onHide}
              className="w-full px-4 py-2.5 bg-[#be8a83] hover:bg-[#a1706a] text-white shadow-md shadow-[#be8a83]/15 font-semibold text-sm rounded-xl transition-all duration-200"
            >
              Fechar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
