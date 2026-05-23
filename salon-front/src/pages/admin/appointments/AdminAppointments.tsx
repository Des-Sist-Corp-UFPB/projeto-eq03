import { useState, useEffect } from 'react';
import { Plus, Clock, User as UserIcon, Calendar as CalendarIcon, X } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { appointmentsApi } from '../../appointments/services/appointments';
import type { AppointmentResponse } from '../../appointments/services/appointments';
import { salonServicesApi } from '../../services/services/services';
import type { SalonServiceData } from '../../services/services/services';
import { employeesApi } from '../employees/services/employees';
import type { EmployeeData } from '../employees/services/employees';
import { usersApi } from '../users/services/users';
import type { UserData } from '../users/services/users';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

const selectCls = 'input-premium';
const labelCls = 'label-premium';

function toLocalDateTimeIso(dtLocal: string): string {
  if (!dtLocal) return '';
  return dtLocal.length === 16 ? `${dtLocal}:00` : dtLocal;
}

function formatServiceOption(s: SalonServiceData): string {
  const ref = s.price != null ? ` — a partir de R$ ${s.price.toFixed(2)}` : '';
  return `${s.name}${ref}`;
}

export const AdminAppointments = () => {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [showModal, setShowModal] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [clients, setClients] = useState<UserData[]>([]);
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [employees, setEmployees] = useState<EmployeeData[]>([]);

  const [selectedClient, setSelectedClient] = useState('');
  const [selectedService, setSelectedService] = useState('');
  const [selectedEmployee, setSelectedEmployee] = useState('');
  const [selectedDateTime, setSelectedDateTime] = useState('');

  const [showConfirm, setShowConfirm] = useState(false);
  const [appointmentToCancel, setAppointmentToCancel] = useState<number | null>(null);

  const [confirmTarget, setConfirmTarget] = useState<AppointmentResponse | null>(null);
  const [confirmDateTime, setConfirmDateTime] = useState('');
  const [confirmSaving, setConfirmSaving] = useState(false);

  const parseDate = (dateValue: string | number[] | null | undefined): number => {
    if (!dateValue) return 0;
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour, minute] = dateValue as number[];
      return new Date(year, month - 1, day, hour, minute).getTime();
    }
    return new Date(dateValue as string).getTime();
  };

  const { error: showError, confirm } = useAlert();

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.findAll();
      data.sort((a, b) => {
        const ta = a.scheduledAt ? parseDate(a.scheduledAt) : a.preferredDate ? new Date(a.preferredDate + 'T12:00:00').getTime() : 0;
        const tb = b.scheduledAt ? parseDate(b.scheduledAt) : b.preferredDate ? new Date(b.preferredDate + 'T12:00:00').getTime() : 0;
        return tb - ta;
      });
      setAppointments(data);
    } catch (err) {
      await showError('Erro ao carregar agendamentos');
    } finally {
      setIsLoading(false);
    }
  };

  const loadFormData = async () => {
    try {
      const [usersData, servicesData, employeesData] = await Promise.all([
        usersApi.findAll(),
        salonServicesApi.findAll(),
        employeesApi.findAll()
      ]);
      setClients(usersData.filter(u => u.role === 'CLIENTE'));
      setServices(servicesData.filter(s => s.active));
      setEmployees(employeesData);
    } catch (err) {
      await showError('Erro ao carregar dados do formulário');
    }
  };

  useEffect(() => { loadAppointments(); loadFormData(); }, []);

  const handleStatusChange = async (id: number, newStatus: string) => {
    try {
      await appointmentsApi.updateStatus(id, newStatus);
      loadAppointments();
    } catch (error) {
      await showError('Erro ao atualizar status');
    }
  };

  const handleCreateAppointment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClient || !selectedService || !selectedEmployee || !selectedDateTime) {
      await showError('Preencha todos os campos, incluindo data e hora');
      return;
    }
    setIsSaving(true);
    try {
      await appointmentsApi.create({
        clientId: Number(selectedClient),
        serviceId: Number(selectedService),
        employeeId: Number(selectedEmployee),
        scheduledAt: toLocalDateTimeIso(selectedDateTime)
      });
      setShowModal(false);
      loadAppointments();
      setSelectedClient(''); setSelectedService(''); setSelectedEmployee(''); setSelectedDateTime('');
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao criar agendamento');
      await showError(msg);
    } finally {
      setIsSaving(false);
    }
  };

  const confirmCancel = async () => {
    if (!appointmentToCancel) return;
    try {
      await appointmentsApi.cancel(appointmentToCancel);
      setShowConfirm(false);
      loadAppointments();
    } catch (error) {
      await showError('Erro ao cancelar agendamento');
    }
  };

  const submitConfirm = async () => {
    if (!confirmTarget || !confirmDateTime) return;
    setConfirmSaving(true);
    try {
      await appointmentsApi.confirm(confirmTarget.id, toLocalDateTimeIso(confirmDateTime));
      setConfirmTarget(null);
      loadAppointments();
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao confirmar horário');
      await showError(msg);
    } finally {
      setConfirmSaving(false);
    }
  };

  const handleDecline = async (id: number) => {
    const confirmed = await confirm('Recusar esta solicitação?');
    if (!confirmed) return;
    try {
      await appointmentsApi.decline(id);
      loadAppointments();
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao recusar');
      await showError(msg);
    }
  };

  const getStatusBadge = (status: string) => {
    const styles: Record<string, string> = {
      PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
      REQUESTED: 'bg-sky-50 text-sky-700 border border-sky-200',
      CONFIRMED: 'bg-[#be8a83]/10 text-[#a6726b] border border-[#be8a83]/20',
      DECLINED: 'bg-gray-100 text-gray-600 border border-gray-200',
      DONE: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
      CANCELLED: 'bg-rose-50 text-rose-700 border border-rose-200',
    };
    const labels: Record<string, string> = { PENDING: 'Pendente', REQUESTED: 'Solicitado', CONFIRMED: 'Confirmado', DECLINED: 'Recusado', DONE: 'Concluído', CANCELLED: 'Cancelado' };
    return (
      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap ${styles[status] || 'bg-gray-100 text-gray-600 border border-gray-200'}`}>
        {labels[status] || status}
      </span>
    );
  };

  const formatDate = (dateValue: string | number[] | null | undefined) => {
    if (!dateValue) return '—';
    let date: Date;
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour, minute] = dateValue as number[];
      date = new Date(year, month - 1, day, hour, minute);
    } else {
      date = new Date(dateValue);
    }
    if (isNaN(date.getTime())) return 'Data inválida';
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(date);
  };

  const columns = [
    {
      key: 'scheduledAt',
      label: 'Data / hora',
      render: (item: AppointmentResponse) =>
        item.scheduledAt ? formatDate(item.scheduledAt)
          : item.preferredDate ? `Pref.: ${new Date(item.preferredDate + 'T12:00:00').toLocaleDateString('pt-BR')} (a combinar)`
          : 'A combinar'
    },
    { key: 'clientName', label: 'Cliente' },
    { key: 'employeeName', label: 'Profissional' },
    { key: 'serviceName', label: 'Serviço' },
    {
      key: 'notes',
      label: 'Obs.',
      render: (item: AppointmentResponse) => (
        <span className="text-xs text-gray-500 max-w-[200px] inline-block truncate">
          {item.clientNotes ? `${item.clientNotes.slice(0, 60)}${item.clientNotes.length > 60 ? '…' : ''}` : '—'}
        </span>
      )
    },
    {
      key: 'status',
      label: 'Status',
      render: (item: AppointmentResponse) => (
        <div className="flex flex-col items-start gap-2">
          {getStatusBadge(item.status)}
          {item.status === 'REQUESTED' && (
            <div className="flex flex-wrap gap-1.5 mt-1">
              <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/confirm`}>
                <button onClick={() => { setConfirmTarget(item); setConfirmDateTime(''); }} className="px-2.5 py-1 bg-[#be8a83] text-white hover:bg-[#a6726b] text-xs font-semibold rounded-lg transition-all cursor-pointer">
                  Definir horário
                </button>
              </PermissionGate>
              <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/decline`}>
                <button onClick={() => handleDecline(item.id)} className="px-2.5 py-1 border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs font-semibold rounded-lg transition-all cursor-pointer">
                  Recusar
                </button>
              </PermissionGate>
            </div>
          )}
          <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/status`}>
            {item.status !== 'CANCELLED' && item.status !== 'DONE' && item.status !== 'DECLINED' && item.status !== 'REQUESTED' && (
              <select
                value={item.status}
                onChange={(e) => handleStatusChange(item.id, e.target.value)}
                className="text-xs px-2.5 py-1.5 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-1 focus:ring-[#be8a83]/20 focus:border-[#be8a83] transition-all cursor-pointer mt-1"
                style={{ width: '140px' }}
              >
                <option value="PENDING">Pendente</option>
                <option value="CONFIRMED">Confirmado</option>
                <option value="DONE">Concluído</option>
              </select>
            )}
          </PermissionGate>
        </div>
      )
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: AppointmentResponse) => (
        item.status !== 'CANCELLED' && item.status !== 'DONE' && item.status !== 'DECLINED' && (
          <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/cancel`}>
            <button
              onClick={() => { setAppointmentToCancel(item.id); setShowConfirm(true); }}
              className="px-2.5 py-1.5 border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs font-semibold rounded-lg transition-all whitespace-nowrap cursor-pointer hover:border-rose-300"
            >
              Cancelar
            </button>
          </PermissionGate>
        )
      )
    }
  ];

  return (
    <>
      <div className="space-y-6 animate-fade-in-up">
        <div className="flex justify-between items-center">
          <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Agendamentos (Admin)</h2>
          <PermissionGate method="POST" endpoint="/v1/appointments">
            <button onClick={() => setShowModal(true)} className="btn-premium font-semibold shadow-md shadow-[#be8a83]/10">
              <Plus size={18} /> Novo Agendamento
            </button>
          </PermissionGate>
        </div>

        {isLoading ? (
          <div className="flex items-center gap-3 text-sm text-[#3b3036]/60 py-10 justify-center">
            <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-[#be8a83]"></div>
            <span>Carregando agendamentos...</span>
          </div>
        ) : (
          <Table columns={columns} data={appointments} keyExtractor={(item) => item.id?.toString() || Math.random().toString()} />
        )}
      </div>

      {/* Create Appointment Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#261f23]/40 backdrop-blur-md">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl border border-[#eae1e1]/85 overflow-hidden animate-scale-up">
            <div className="flex items-center justify-between px-6 py-4 border-b border-[#eae1e1] bg-[#fcf9f9]/50">
              <h3 className="font-heading text-lg font-bold text-[#3b3036]">Novo Agendamento</h3>
              <button onClick={() => setShowModal(false)} className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all"><X size={20} /></button>
            </div>
            <form onSubmit={handleCreateAppointment}>
              <div className="p-6 space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className={labelCls}><UserIcon size={14} className="inline mr-1" />Cliente</label>
                    <select value={selectedClient} onChange={(e) => setSelectedClient(e.target.value)} required className={selectCls}>
                      <option value="">Selecione o cliente</option>
                      {clients.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className={labelCls}><Clock size={14} className="inline mr-1" />Serviço</label>
                    <select value={selectedService} onChange={(e) => setSelectedService(e.target.value)} required className={selectCls}>
                      <option value="">Selecione o serviço</option>
                      {services.map(s => <option key={s.id} value={s.id}>{formatServiceOption(s)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className={labelCls}><UserIcon size={14} className="inline mr-1" />Profissional</label>
                    <select value={selectedEmployee} onChange={(e) => setSelectedEmployee(e.target.value)} required className={selectCls}>
                      <option value="">Selecione a profissional</option>
                      {employees.map(e => <option key={e.id} value={e.id}>{e.name}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className={labelCls}><CalendarIcon size={14} className="inline mr-1" />Data e hora</label>
                    <input type="datetime-local" value={selectedDateTime} onChange={(e) => setSelectedDateTime(e.target.value)} required className={selectCls} />
                    <p className="text-xs text-gray-400 mt-1">Horário livre — sem grade fixa no sistema.</p>
                  </div>
                </div>
                <div className="p-3.5 bg-amber-50 border border-amber-100 rounded-xl text-xs text-amber-700">
                  O agendamento nasce já <strong>confirmado</strong>. Clientes pelo site enviam uma <strong>solicitação</strong> para você aceitar e marcar o horário.
                </div>
              </div>
              <div className="flex justify-end gap-3 px-6 py-4 border-t border-[#eae1e1] bg-[#fcf9f9]/50">
                <button type="button" onClick={() => setShowModal(false)} className="px-5 py-2.5 border border-[#eae1e1] font-semibold text-sm text-[#3b3036] hover:bg-white hover:border-[#be8a83]/50 rounded-xl transition-all">Fechar</button>
                <button type="submit" disabled={isSaving} className="px-5 py-2.5 bg-[#be8a83] text-white hover:bg-[#a6726b] font-semibold text-sm rounded-xl transition-all shadow-md shadow-[#be8a83]/10 disabled:opacity-50">
                  {isSaving ? 'Salvando...' : 'Criar Agendamento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirm DateTime Modal */}
      {confirmTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#261f23]/40 backdrop-blur-md">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md border border-[#eae1e1]/85 overflow-hidden animate-scale-up">
            <div className="flex items-center justify-between px-6 py-4 border-b border-[#eae1e1] bg-[#fcf9f9]/50">
              <h3 className="font-heading text-lg font-bold text-[#3b3036]">Confirmar horário</h3>
              {!confirmSaving && <button onClick={() => setConfirmTarget(null)} className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all"><X size={20} /></button>}
            </div>
            <div className="p-6 space-y-4">
              <p className="text-xs text-gray-500 leading-relaxed">
                Defina data e hora para <strong>{confirmTarget.clientName}</strong>. Conflitos com outros agendamentos confirmados do mesmo profissional serão bloqueados.
              </p>
              <div>
                <label className={labelCls}>Data e hora</label>
                <input type="datetime-local" value={confirmDateTime} onChange={(e) => setConfirmDateTime(e.target.value)} className={selectCls} />
              </div>
            </div>
            <div className="flex justify-end gap-3 px-6 py-4 border-t border-[#eae1e1] bg-[#fcf9f9]/50">
              <button type="button" onClick={() => setConfirmTarget(null)} disabled={confirmSaving} className="px-5 py-2.5 border border-[#eae1e1] font-semibold text-sm text-[#3b3036] hover:bg-white hover:border-[#be8a83]/50 rounded-xl transition-all disabled:opacity-50">Cancelar</button>
              <button onClick={submitConfirm} disabled={confirmSaving || !confirmDateTime} className="px-5 py-2.5 bg-[#be8a83] text-white hover:bg-[#a6726b] font-semibold text-sm rounded-xl transition-all shadow-md shadow-[#be8a83]/10 disabled:opacity-50">
                {confirmSaving ? 'Salvando...' : 'Confirmar solicitação'}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog show={showConfirm} onHide={() => setShowConfirm(false)} onConfirm={confirmCancel} title="Cancelar Agendamento" message="Tem certeza que deseja cancelar este agendamento? Esta ação não pode ser desfeita." />
    </>
  );
};
