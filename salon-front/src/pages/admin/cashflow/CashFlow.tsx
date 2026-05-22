import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Plus, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { cashFlowApi } from './services/cashflow';
import type { CashFlowData } from './services/cashflow';
import { getApiErrorMessage } from '../../../utils/apiError';
import { useAlert } from '../../../hooks/useAlert';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const CashFlow = () => {
  const [cashFlows, setCashFlows] = useState<CashFlowData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<number | null>(null);

  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  
  const { register, handleSubmit, reset, formState: { errors } } = useForm<CashFlowData>();
  const { error: showError } = useAlert();

  const loadCashFlows = async () => {
    setIsLoading(true);
    try {
      const data = await cashFlowApi.findByPeriod(dateFrom || undefined, dateTo || undefined);
      setCashFlows(data);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar fluxo de caixa');
      await showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadCashFlows(); }, [dateFrom, dateTo]);

  const handleOpenForm = () => {
    reset({ type: 'INCOME', date: new Date().toISOString().split('T')[0] });
    setShowForm(true);
  };

  const onSubmit = async (data: CashFlowData) => {
    try {
      await cashFlowApi.create(data);
      setShowForm(false);
      loadCashFlows();
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao salvar registro no fluxo de caixa.');
      await showError(msg);
    }
  };

  const confirmDelete = async () => {
    if (!itemToDelete) return;
    try {
      await cashFlowApi.delete(itemToDelete);
      setShowConfirm(false);
      loadCashFlows();
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao excluir registro.');
      await showError(msg);
    }
  };

  const columns = [
    { key: 'date', label: 'Data', render: (item: CashFlowData) => new Date(item.date).toLocaleDateString('pt-BR') },
    { key: 'description', label: 'Descrição' },
    {
      key: 'type',
      label: 'Tipo',
      render: (item: CashFlowData) => (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
          item.type === 'INCOME'
            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
            : 'bg-rose-50 text-rose-700 border border-rose-200'
        }`}>
          {item.type === 'INCOME' ? 'Entrada' : 'Saída'}
        </span>
      )
    },
    {
      key: 'amount',
      label: 'Valor',
      render: (item: CashFlowData) => (
        <span className={`font-semibold ${item.type === 'INCOME' ? 'text-emerald-600' : 'text-rose-600'}`}>
          {item.type === 'INCOME' ? '+' : '-'} R$ {item.amount.toFixed(2)}
        </span>
      )
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: CashFlowData) => (
        <PermissionGate method="DELETE" endpoint={`/v1/cashflow/${item.id}`}>
          <button
            onClick={() => { setItemToDelete(item.id!); setShowConfirm(true); }}
            className="p-2 text-rose-600 hover:bg-rose-50 hover:text-rose-700 border border-[#eae1e1] hover:border-rose-200 rounded-xl transition-all cursor-pointer"
            title="Excluir Registro"
          >
            <Trash2 size={15} />
          </button>
        </PermissionGate>
      )
    }
  ];

  return (
    <div className="space-y-6 animate-fade-in-up">
      <div className="flex justify-between items-center">
        <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Fluxo de Caixa</h2>
        <PermissionGate method="POST" endpoint="/v1/cashflow">
          <button onClick={handleOpenForm} className="btn-premium font-semibold shadow-md shadow-[#be8a83]/10">
            <Plus size={18} /> Novo Registro
          </button>
        </PermissionGate>
      </div>

      <div className="flex flex-wrap gap-4 items-end bg-white/80 backdrop-blur-md rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm">
        <div className="space-y-1">
          <label className={labelCls}>De</label>
          <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} className={inputCls} />
        </div>
        <div className="space-y-1">
          <label className={labelCls}>Até</label>
          <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} className={inputCls} />
        </div>
        <button
          onClick={() => { setDateFrom(''); setDateTo(''); }}
          className="px-5 py-2.5 border border-[#eae1e1] text-sm font-semibold text-[#3b3036] hover:text-[#be8a83] hover:border-[#be8a83] bg-white rounded-xl transition-all duration-200 cursor-pointer"
        >
          Limpar Filtros
        </button>
      </div>

      {isLoading ? (
        <div className="flex items-center gap-3 text-sm text-[#3b3036]/60 py-10 justify-center">
          <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-[#be8a83]"></div>
          <span>Carregando registros...</span>
        </div>
      ) : (
        <Table columns={columns} data={cashFlows} keyExtractor={(item) => item.id!} />
      )}

      <ModalForm show={showForm} onHide={() => setShowForm(false)} title="Novo Registro" onSubmit={handleSubmit(onSubmit)}>
        <div className="space-y-4">
          <div>
            <label className={labelCls}>Tipo</label>
            <select className={inputCls} {...register('type', { required: 'Tipo é obrigatório' })}>
              <option value="INCOME">Entrada (Receita)</option>
              <option value="EXPENSE">Saída (Despesa)</option>
            </select>
          </div>
          <div>
            <label className={labelCls}>Valor (R$)</label>
            <input type="number" step="0.01" className={`${inputCls} ${errors.amount ? 'border-rose-300 focus:ring-rose-500/10 focus:border-rose-400' : ''}`} {...register('amount', { required: 'Valor é obrigatório', min: { value: 0.01, message: 'Valor inválido' } })} />
            {errors.amount && <span className="text-xs text-rose-500 font-semibold mt-1 block">{errors.amount.message}</span>}
          </div>
          <div>
            <label className={labelCls}>Descrição</label>
            <input type="text" className={`${inputCls} ${errors.description ? 'border-rose-300 focus:ring-rose-500/10 focus:border-rose-400' : ''}`} {...register('description', { required: 'Descrição é obrigatória' })} />
            {errors.description && <span className="text-xs text-rose-500 font-semibold mt-1 block">{errors.description.message}</span>}
          </div>
          <div>
            <label className={labelCls}>Data</label>
            <input type="date" className={`${inputCls} ${errors.date ? 'border-rose-300 focus:ring-rose-500/10 focus:border-rose-400' : ''}`} {...register('date', { required: 'Data é obrigatória' })} />
            {errors.date && <span className="text-xs text-rose-500 font-semibold mt-1 block">{errors.date.message}</span>}
          </div>
        </div>
      </ModalForm>

      <ConfirmDialog show={showConfirm} onHide={() => setShowConfirm(false)} onConfirm={confirmDelete} title="Excluir Registro" message="Tem certeza que deseja excluir este registro? Esta ação afetará os relatórios." />
    </div>
  );
};
