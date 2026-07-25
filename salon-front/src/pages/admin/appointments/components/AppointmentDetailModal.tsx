import { X } from 'lucide-react';
import type { AppointmentResponse } from '../../../appointments/services/appointments';

const labelCls = 'label-premium';

function formatMoney(value: number | null | undefined): string {
  return value != null ? `R$ ${value.toFixed(2)}` : '—';
}

function formatDuration(value: number | null | undefined): string {
  return value != null ? `${value} min` : '—';
}

interface AppointmentDetailModalProps {
  appointment: AppointmentResponse | null;
  catalogPrice?: number | null;
  catalogDurationMin?: number | null;
  onClose: () => void;
}

export const AppointmentDetailModal = ({
  appointment,
  catalogPrice,
  catalogDurationMin,
  onClose,
}: AppointmentDetailModalProps) => {
  if (!appointment) return null;

  const isCustomized =
    appointment.customPrice != null ||
    appointment.customDurationMin != null ||
    !!appointment.customServiceNotes;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#261f23]/40 backdrop-blur-md">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md border border-[#eae1e1]/85 overflow-hidden animate-scale-up">
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#eae1e1] bg-[#fcf9f9]/50">
          <h3 className="font-heading text-lg font-bold text-[#3b3036]">Detalhes do agendamento</h3>
          <button
            onClick={onClose}
            className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all cursor-pointer"
          >
            <X size={20} />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <span className={labelCls}>Cliente</span>
              <p className="text-[#3b3036]">{appointment.clientName}</p>
            </div>
            <div>
              <span className={labelCls}>Profissional</span>
              <p className="text-[#3b3036]">{appointment.employeeName}</p>
            </div>
            <div className="col-span-2">
              <span className={labelCls}>Serviço</span>
              <p className="text-[#3b3036]">{appointment.serviceName}</p>
            </div>
          </div>

          {isCustomized ? (
            <div className="border border-[#eae1e1] rounded-xl divide-y divide-[#eae1e1]/70">
              <div className="flex items-center justify-between px-4 py-3">
                <span className="text-xs font-semibold text-[#7a7074]">Preço</span>
                <div className="text-right">
                  {appointment.customPrice != null && catalogPrice != null && (
                    <p className="text-xs text-gray-400 line-through">Catálogo: {formatMoney(catalogPrice)}</p>
                  )}
                  <p className="font-semibold text-[#3b3036]">{formatMoney(appointment.effectivePrice)}</p>
                </div>
              </div>
              <div className="flex items-center justify-between px-4 py-3">
                <span className="text-xs font-semibold text-[#7a7074]">Duração</span>
                <div className="text-right">
                  {appointment.customDurationMin != null && catalogDurationMin != null && (
                    <p className="text-xs text-gray-400 line-through">Catálogo: {formatDuration(catalogDurationMin)}</p>
                  )}
                  <p className="font-semibold text-[#3b3036]">{formatDuration(appointment.effectiveDurationMin)}</p>
                </div>
              </div>
              {appointment.customServiceNotes && (
                <div className="px-4 py-3">
                  <span className="text-xs font-semibold text-[#7a7074]">Observações do serviço</span>
                  <p className="text-sm text-[#3b3036] mt-1">{appointment.customServiceNotes}</p>
                </div>
              )}
              <div className="px-4 py-2 bg-[#fdf6f5]">
                <p className="text-[11px] text-[#a6726b]">
                  Personalizado para este agendamento — o cadastro do serviço não foi alterado.
                </p>
              </div>
            </div>
          ) : (
            <div className="border border-[#eae1e1] rounded-xl divide-y divide-[#eae1e1]/70">
              <div className="flex items-center justify-between px-4 py-3">
                <span className="text-xs font-semibold text-[#7a7074]">Preço</span>
                <p className="font-semibold text-[#3b3036]">{formatMoney(appointment.effectivePrice)}</p>
              </div>
              <div className="flex items-center justify-between px-4 py-3">
                <span className="text-xs font-semibold text-[#7a7074]">Duração</span>
                <p className="font-semibold text-[#3b3036]">{formatDuration(appointment.effectiveDurationMin)}</p>
              </div>
            </div>
          )}

          {appointment.clientNotes && (
            <div>
              <span className={labelCls}>Observações do cliente</span>
              <p className="text-sm text-[#3b3036] mt-1">{appointment.clientNotes}</p>
            </div>
          )}
        </div>

        <div className="flex justify-end px-6 py-4 border-t border-[#eae1e1] bg-[#fcf9f9]/50">
          <button
            onClick={onClose}
            className="px-5 py-2.5 border border-[#eae1e1] font-semibold text-sm text-[#3b3036] hover:bg-white hover:border-[#be8a83]/50 rounded-xl transition-all cursor-pointer"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};
