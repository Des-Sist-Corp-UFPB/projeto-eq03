import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { salonServicesApi, displayServiceDuration } from '../../services/services/services';
import type { SalonServiceData } from '../../services/services/services';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const AdminServices = () => {
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filterActive, setFilterActive] = useState<boolean | undefined>(undefined);

  const [showForm, setShowForm] = useState(false);
  const [editingService, setEditingService] = useState<SalonServiceData | null>(null);

  const [showConfirm, setShowConfirm] = useState(false);
  const [serviceToDelete, setServiceToDelete] = useState<number | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<SalonServiceData>();
  const { error: showError } = useAlert();

  const loadServices = async () => {
    setIsLoading(true);
    try {
      const data = await salonServicesApi.findAll(filterActive);
      setServices(data);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar serviços');
      await showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadServices();
  }, [filterActive]);

  const handleOpenForm = (service?: SalonServiceData) => {
    reset();
    if (service) {
      setEditingService(service);
      setValue('name', service.name);
      setValue('description', service.description);
      setValue('price', service.price ?? undefined);
      setValue('durationMin', service.durationMin ?? undefined);
      setValue('durationEstimate', service.durationEstimate ?? '');
      setValue('active', service.active);
    } else {
      setEditingService(null);
      setValue('active', true);
    }
    setShowForm(true);
  };

  const onSubmit = async (data: SalonServiceData) => {
    const hasEst = (data.durationEstimate ?? '').trim().length > 0;
    const hasMin = data.durationMin != null && Number(data.durationMin) > 0;
    if (!hasEst && !hasMin) {
      await showError(
        'Informe o tempo estimado em texto (ex.: em média 50 min) e/ou minutos para encaixe na agenda.'
      );
      return;
    }
    try {
      const payload: SalonServiceData = {
        ...data,
        price: data.price ?? null,
        durationEstimate: hasEst ? data.durationEstimate!.trim() : null,
        durationMin: hasMin ? Number(data.durationMin) : null,
      };
      if (editingService?.id) {
        await salonServicesApi.update(editingService.id, payload);
      } else {
        await salonServicesApi.create(payload);
      }
      setShowForm(false);
      loadServices();
    } catch (err) {
      const msg = getApiErrorMessage(
        err,
        'Erro ao salvar serviço. Verifique os dados e tente novamente.'
      );
      await showError(msg);
    }
  };

  const confirmDelete = async () => {
    if (!serviceToDelete) return;
    try {
      await salonServicesApi.delete(serviceToDelete);
      setShowConfirm(false);
      loadServices();
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao excluir serviço.');
      await showError(msg);
    }
  };

  const columns = [
    { key: 'name', label: 'Nome' },
    {
      key: 'price',
      label: 'Referência',
      render: (item: SalonServiceData) =>
        item.price != null ? `A partir de R$ ${item.price.toFixed(2)}` : '—',
    },
    {
      key: 'duration',
      label: 'Tempo estimado',
      render: (item: SalonServiceData) => displayServiceDuration(item),
    },
    {
      key: 'active',
      label: 'Status',
      render: (item: SalonServiceData) => (item.active ? 'Ativo' : 'Inativo'),
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: SalonServiceData) => (
        <div className="flex gap-2">
          <PermissionGate method="PUT" endpoint={`/v1/services/${item.id}`}>
            <button
              onClick={() => handleOpenForm(item)}
              className="p-1.5 text-indigo-600 hover:bg-indigo-50 border border-indigo-200 rounded-lg transition-all cursor-pointer"
            >
              <Edit size={15} />
            </button>
          </PermissionGate>
          <PermissionGate method="DELETE" endpoint={`/v1/services/${item.id}`}>
            <button
              onClick={() => {
                setServiceToDelete(item.id!);
                setShowConfirm(true);
              }}
              className="p-1.5 text-rose-600 hover:bg-rose-50 border border-rose-200 rounded-lg transition-all cursor-pointer"
            >
              <Trash2 size={15} />
            </button>
          </PermissionGate>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Gerenciar Serviços</h2>
        <div className="flex gap-3">
          <select
            value={filterActive === undefined ? 'ALL' : filterActive ? 'ACTIVE' : 'INACTIVE'}
            onChange={(e) => {
              const val = e.target.value;
              setFilterActive(val === 'ALL' ? undefined : val === 'ACTIVE');
            }}
            className="input-premium py-2 text-sm w-auto cursor-pointer"
          >
            <option value="ALL">Todos os Registros</option>
            <option value="ACTIVE">Apenas Ativos</option>
            <option value="INACTIVE">Apenas Inativos</option>
          </select>
          <PermissionGate method="POST" endpoint="/v1/services">
            <button onClick={() => handleOpenForm()} className="btn-premium">
              <Plus size={18} /> Novo Serviço
            </button>
          </PermissionGate>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center gap-2 text-sm text-[#3b3036]/60 py-8">
          <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-[#be8a83]"></div>
          Carregando serviços...
        </div>
      ) : (
        <Table columns={columns} data={services} keyExtractor={(item) => item.id!} />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingService ? 'Editar Serviço' : 'Novo Serviço'}
        onSubmit={handleSubmit(onSubmit)}
      >
        <div className="space-y-4">
          <div>
            <label className={labelCls}>Nome do Serviço</label>
            <input
              type="text"
              className={`${inputCls} ${errors.name ? 'border-rose-300' : ''}`}
              {...register('name', {
                required: 'Nome é obrigatório',
                minLength: { value: 3, message: 'Mín. 3 caracteres' },
              })}
            />
            {errors.name && (
              <span className="text-xs text-rose-500 font-semibold">{errors.name.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>Descrição</label>
            <textarea rows={3} className={`${inputCls} resize-none`} {...register('description')} />
          </div>
          <div>
            <label className={labelCls}>Valor de referência — "a partir de" (opcional)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              placeholder="Deixe em branco se o valor for combinado"
              className={inputCls}
              {...register('price', {
                setValueAs: (v) =>
                  v === '' || v === undefined || v === null ? undefined : Number(v),
              })}
            />
            <p className="text-xs text-gray-400 mt-1">
              O preço final pode ser registrado no fluxo de caixa ao concluir o atendimento.
            </p>
          </div>
          <div>
            <label className={labelCls}>Tempo estimado (mostrado ao cliente)</label>
            <input
              type="text"
              placeholder="Ex.: Em média 50 min · Em média 1h20"
              className={inputCls}
              {...register('durationEstimate')}
            />
            <p className="text-xs text-gray-400 mt-1">
              Texto livre. Obrigatório informar isto ou os minutos abaixo (ou ambos).
            </p>
          </div>
          <div>
            <label className={labelCls}>Minutos para encaixe na agenda (opcional)</label>
            <input
              type="number"
              min={1}
              placeholder="Só números — ajuda a evitar sobreposição de horários"
              className={inputCls}
              {...register('durationMin', {
                setValueAs: (v) =>
                  v === '' || v === undefined || v === null ? undefined : Number(v),
              })}
            />
          </div>
          <div className="flex items-center gap-3">
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" {...register('active')} />
              <div className="w-10 h-5 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#be8a83]"></div>
            </label>
            <span className="text-sm font-semibold text-[#3b3036]">Serviço Ativo</span>
          </div>
        </div>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmDelete}
        title="Excluir Serviço"
        message="Tem certeza que deseja excluir este serviço? Esta ação não pode ser desfeita."
      />
    </div>
  );
};
