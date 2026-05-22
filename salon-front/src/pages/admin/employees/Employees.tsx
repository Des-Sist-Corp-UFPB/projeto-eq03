import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { employeesApi } from './services/employees';
import type { EmployeeData } from './services/employees';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const Employees = () => {
  const [employees, setEmployees] = useState<EmployeeData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<EmployeeData | null>(null);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [employeeToDelete, setProductToDelete] = useState<number | null>(null);
  
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<EmployeeData>();
  const { error: showError } = useAlert();

  const loadEmployees = async () => {
    setIsLoading(true);
    try {
      const data = await employeesApi.findAll();
      setEmployees(data);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar funcionárias');
      await showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadEmployees(); }, []);

  const handleOpenForm = (employee?: EmployeeData) => {
    reset();
    if (employee) {
      setEditingEmployee(employee);
      setValue('userId', employee.userId);
      setValue('bio', employee.bio);
    } else {
      setEditingEmployee(null);
    }
    setShowForm(true);
  };

  const onSubmit = async (data: EmployeeData) => {
    try {
      if (editingEmployee?.id) {
        await employeesApi.update(editingEmployee.id, data);
      } else {
        await employeesApi.create(data);
      }
      setShowForm(false);
      loadEmployees();
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao salvar funcionária. Verifique se o ID de usuário está correto.');
      await showError(msg);
    }
  };

  const confirmDelete = async () => {
    if (!employeeToDelete) return;
    try {
      await employeesApi.delete(employeeToDelete);
      setShowConfirm(false);
      loadEmployees();
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao excluir funcionária.');
      await showError(msg);
    }
  };

  const columns = [
    { key: 'name', label: 'Nome' },
    { key: 'email', label: 'Email' },
    { key: 'userId', label: 'ID Usuário' },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: EmployeeData) => (
        <div className="flex gap-2">
          <PermissionGate method="PUT" endpoint={`/v1/employees/${item.id}`}>
            <button
              onClick={() => handleOpenForm(item)}
              className="p-1.5 text-indigo-600 hover:bg-indigo-50 border border-indigo-200 rounded-lg transition-all cursor-pointer"
            >
              <Edit size={15} />
            </button>
          </PermissionGate>
          <PermissionGate method="DELETE" endpoint={`/v1/employees/${item.id}`}>
            <button
              onClick={() => { setProductToDelete(item.id!); setShowConfirm(true); }}
              className="p-1.5 text-rose-600 hover:bg-rose-50 border border-rose-200 rounded-lg transition-all cursor-pointer"
            >
              <Trash2 size={15} />
            </button>
          </PermissionGate>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Gerenciar Funcionárias</h2>
        <PermissionGate method="POST" endpoint="/v1/employees">
          <button
            onClick={() => handleOpenForm()}
            className="btn-premium"
          >
            <Plus size={18} /> Vincular Funcionária
          </button>
        </PermissionGate>
      </div>

      {isLoading ? (
        <div className="flex items-center gap-2 text-sm text-[#3b3036]/60 py-8">
          <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-[#be8a83]"></div>
          Carregando funcionárias...
        </div>
      ) : (
        <Table columns={columns} data={employees} keyExtractor={(item) => item.id!} />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingEmployee ? 'Editar Funcionária' : 'Nova Funcionária'}
        onSubmit={handleSubmit(onSubmit)}
      >
        <div className="space-y-4">
          <div>
            <label className={labelCls}>ID do Usuário</label>
            <input
              type="number"
              className={`${inputCls} ${errors.userId ? 'border-rose-300' : ''} ${editingEmployee ? 'opacity-60 cursor-not-allowed' : ''}`}
              {...register('userId', { required: 'ID do usuário é obrigatório' })}
              disabled={!!editingEmployee}
            />
            <p className="text-xs text-gray-400 mt-1">Insira o ID do usuário que será vinculado como funcionária.</p>
            {errors.userId && <span className="text-xs text-rose-500 font-semibold">{errors.userId.message}</span>}
          </div>
          <div>
            <label className={labelCls}>Biografia / Especialidade</label>
            <textarea rows={3} className={`${inputCls} resize-none`} {...register('bio')} />
          </div>
        </div>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmDelete}
        title="Desvincular Funcionária"
        message="Tem certeza que deseja remover esta funcionária? O usuário continuará existindo, apenas perderá o vínculo de funcionária."
      />
    </div>
  );
};
