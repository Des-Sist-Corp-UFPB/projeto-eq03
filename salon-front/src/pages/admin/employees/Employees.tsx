import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2, Eye } from 'lucide-react';
import { DataTable } from '../../../components/table/DataTable';
import type { FilterField } from '../../../components/table/DataTable';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { employeesApi } from './services/employees';
import type { EmployeeData, EmployeeFilter } from './services/employees';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const Employees = () => {
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const [showForm, setShowForm] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<EmployeeData | null>(null);

  const [showConfirm, setShowConfirm] = useState(false);
  const [employeeToDelete, setProductToDelete] = useState<number | null>(null);

  const [showDetails, setShowDetails] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<EmployeeData | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    clearErrors,
    formState: { errors },
  } = useForm<EmployeeData>();
  const remunerationType = watch('remunerationType');
  const { error: showError } = useAlert();

  useEffect(() => {
    clearErrors('remunerationValue');
    clearErrors('commissionValue');
    clearErrors('commissionScope');
  }, [remunerationType, clearErrors]);

  const handleOpenDetails = (employee: EmployeeData) => {
    setSelectedEmployee(employee);
    setShowDetails(true);
  };

  const fetchEmployeesData = async (filter: EmployeeFilter, page: number, size: number) => {
    return employeesApi.findAll(filter, page, size);
  };

  const handleOpenForm = (employee?: EmployeeData) => {
    reset();
    if (employee) {
      setEditingEmployee(employee);
      setValue('userId', employee.userId);
      setValue('bio', employee.bio || '');
      setValue('remunerationType', employee.remunerationType);
      setValue('commissionScope', employee.commissionScope);
      setValue('remunerationValue', employee.remunerationValue);
      setValue('commissionValue', employee.commissionValue);
    } else {
      setEditingEmployee(null);
    }
    setShowForm(true);
  };

  const onSubmit = async (data: EmployeeData) => {
    const payload: EmployeeData = {
      userId: Number(data.userId),
      bio: data.bio || undefined,
      remunerationType: data.remunerationType || undefined,
    };

    if (data.remunerationType === 'SALARIO_FIXO') {
      payload.remunerationValue = Number(data.remunerationValue);
      payload.commissionScope = undefined;
      payload.commissionValue = undefined;
    } else if (data.remunerationType === 'COMISSIONADO') {
      payload.remunerationValue = Number(data.remunerationValue);
      payload.commissionScope = data.commissionScope;
      payload.commissionValue = undefined;
    } else if (data.remunerationType === 'FIXO_E_COMISSIONADO') {
      payload.remunerationValue = Number(data.remunerationValue);
      payload.commissionScope = data.commissionScope;
      payload.commissionValue = Number(data.commissionValue);
    } else {
      payload.remunerationValue = undefined;
      payload.commissionScope = undefined;
      payload.commissionValue = undefined;
    }

    try {
      if (editingEmployee?.id) {
        await employeesApi.update(editingEmployee.id, payload);
      } else {
        await employeesApi.create(payload);
      }
      setShowForm(false);
      setRefreshTrigger((prev) => prev + 1);
    } catch (err) {
      const msg = getApiErrorMessage(
        err,
        'Erro ao salvar funcionário(a). Verifique se o ID de usuário está correto.'
      );
      await showError(msg);
    }
  };

  const confirmDelete = async () => {
    if (!employeeToDelete) return;
    try {
      await employeesApi.delete(employeeToDelete);
      setShowConfirm(false);
      setRefreshTrigger((prev) => prev + 1);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao desvincular funcionário(a).');
      await showError(msg);
    }
  };

  const columns = [
    {
      key: 'name',
      label: 'Nome',
      render: (item: EmployeeData) => (
        <button
          onClick={() => handleOpenDetails(item)}
          className="text-left font-semibold text-[#8b6d68] hover:underline cursor-pointer focus:outline-none"
        >
          {item.name}
        </button>
      ),
    },
    { key: 'userId', label: 'ID Usuário' },
    {
      key: 'remunerationType',
      label: 'Remuneração',
      render: (item: EmployeeData) => {
        if (item.remunerationType === 'SALARIO_FIXO') return 'Salário Fixo';
        if (item.remunerationType === 'COMISSIONADO') return 'Comissionado';
        if (item.remunerationType === 'FIXO_E_COMISSIONADO') return 'Fixo + Comissionado';
        return 'Não definido';
      },
    },
    {
      key: 'remunerationValue',
      label: 'Valor',
      render: (item: EmployeeData) => {
        if (item.remunerationType === 'SALARIO_FIXO') {
          return `R$ ${(item.remunerationValue ?? 0).toFixed(2)}`;
        }
        if (item.remunerationType === 'COMISSIONADO') {
          const scope = item.commissionScope === 'GLOBAL' ? 'Global' : 'Individual';
          return `${item.remunerationValue ?? 0}% (${scope})`;
        }
        if (item.remunerationType === 'FIXO_E_COMISSIONADO') {
          const scope = item.commissionScope === 'GLOBAL' ? 'Global' : 'Individual';
          return `R$ ${(item.remunerationValue ?? 0).toFixed(2)} + ${(item.commissionValue ?? 0).toFixed(0)}% (${scope})`;
        }
        return '-';
      },
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: EmployeeData) => (
        <div className="flex gap-2">
          <button
            onClick={() => handleOpenDetails(item)}
            className="p-1.5 text-slate-600 hover:bg-slate-50 border border-slate-200 rounded-lg transition-all cursor-pointer"
            title="Ver Detalhes"
          >
            <Eye size={15} />
          </button>
          <PermissionGate method="PUT" endpoint={`/v1/employees/${item.id}`}>
            <button
              onClick={() => handleOpenForm(item)}
              className="p-1.5 text-indigo-600 hover:bg-indigo-50 border border-indigo-200 rounded-lg transition-all cursor-pointer"
              title="Editar"
            >
              <Edit size={15} />
            </button>
          </PermissionGate>
          <PermissionGate method="DELETE" endpoint={`/v1/employees/${item.id}`}>
            <button
              onClick={() => {
                setProductToDelete(item.id!);
                setShowConfirm(true);
              }}
              className="p-1.5 text-rose-600 hover:bg-rose-50 border border-rose-200 rounded-lg transition-all cursor-pointer"
              title="Apagar"
            >
              <Trash2 size={15} />
            </button>
          </PermissionGate>
        </div>
      ),
    },
  ];

  const filtersConfig: FilterField[] = [
    { key: 'name', label: 'Nome', type: 'text' },
    { key: 'active', label: 'Status', type: 'boolean' },
  ];

  const initialFilters: EmployeeFilter = {
    name: '',
    active: undefined,
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Gerenciar Funcionários(as)</h2>
        <PermissionGate method="POST" endpoint="/v1/employees">
          <button onClick={() => handleOpenForm()} className="btn-premium">
            <Plus size={18} /> Vincular Funcionário(a)
          </button>
        </PermissionGate>
      </div>

      <DataTable
        columns={columns}
        fetchData={fetchEmployeesData}
        filtersConfig={filtersConfig}
        keyExtractor={(item) => item.id!}
        refreshTrigger={refreshTrigger}
        initialFilters={initialFilters}
      />

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingEmployee ? 'Editar Funcionário(a)' : 'Novo(a) Funcionário(a)'}
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
            <p className="text-xs text-gray-400 mt-1">
              Insira o ID do usuário que será vinculado como funcionário(a).
            </p>
            {errors.userId && (
              <span className="text-xs text-rose-500 font-semibold">{errors.userId.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>Biografia / Especialidade</label>
            <textarea rows={3} className={`${inputCls} resize-none`} {...register('bio')} />
          </div>

          <div className="border-t border-[#eae1e1]/50 pt-4">
            <h4 className="font-heading font-semibold text-sm text-[#3b3036] mb-3">
              Modelo de Remuneração
            </h4>
            <div className="grid grid-cols-1 gap-4">
              <div>
                <label className={labelCls}>Tipo de Remuneração</label>
                <select className={inputCls} {...register('remunerationType')}>
                  <option value="">Não definido</option>
                  <option value="SALARIO_FIXO">Salário Fixo</option>
                  <option value="COMISSIONADO">Comissionado</option>
                  <option value="FIXO_E_COMISSIONADO">Salário Fixo + Comissionado</option>
                </select>
              </div>

              {(remunerationType === 'COMISSIONADO' ||
                remunerationType === 'FIXO_E_COMISSIONADO') && (
                <div>
                  <label className={labelCls}>Escopo da Comissão</label>
                  <select
                    className={`${inputCls} ${errors.commissionScope ? 'border-rose-300' : ''}`}
                    {...register('commissionScope', {
                      required:
                        remunerationType === 'COMISSIONADO' ||
                        remunerationType === 'FIXO_E_COMISSIONADO'
                          ? 'O escopo da comissão é obrigatório'
                          : false,
                    })}
                  >
                    <option value="">Selecione o escopo...</option>
                    <option value="INDIVIDUAL">
                      Comissão Individual (sobre serviços executados)
                    </option>
                    <option value="GLOBAL">Comissão Global (sobre total do salão)</option>
                  </select>
                  {errors.commissionScope && (
                    <span className="text-xs text-rose-500 font-semibold">
                      {errors.commissionScope.message}
                    </span>
                  )}
                </div>
              )}

              {(remunerationType === 'SALARIO_FIXO' ||
                remunerationType === 'COMISSIONADO' ||
                remunerationType === 'FIXO_E_COMISSIONADO') && (
                <div>
                  <label className={labelCls}>
                    {remunerationType === 'COMISSIONADO'
                      ? 'Porcentagem da Comissão (%)'
                      : 'Valor do Salário Fixo (R$)'}
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    className={`${inputCls} ${errors.remunerationValue ? 'border-rose-300' : ''}`}
                    {...register('remunerationValue', {
                      required:
                        remunerationType === 'COMISSIONADO'
                          ? 'A porcentagem da comissão é obrigatória'
                          : 'O salário fixo é obrigatório',
                      min: { value: 0, message: 'O valor não pode ser negativo' },
                      validate: {
                        commissionLimit: (value) => {
                          if (remunerationType === 'COMISSIONADO' && Number(value) > 100) {
                            return 'A comissão não pode passar de 100%';
                          }
                          return true;
                        },
                      },
                    })}
                  />
                  {errors.remunerationValue && (
                    <span className="text-xs text-rose-500 font-semibold">
                      {errors.remunerationValue.message}
                    </span>
                  )}
                </div>
              )}

              {remunerationType === 'FIXO_E_COMISSIONADO' && (
                <div>
                  <label className={labelCls}>Porcentagem da Comissão (%)</label>
                  <input
                    type="number"
                    step="0.01"
                    className={`${inputCls} ${errors.commissionValue ? 'border-rose-300' : ''}`}
                    {...register('commissionValue', {
                      required: 'A porcentagem da comissão é obrigatória',
                      min: { value: 0, message: 'O valor não pode ser negativo' },
                      max: { value: 100, message: 'A comissão não pode passar de 100%' },
                    })}
                  />
                  {errors.commissionValue && (
                    <span className="text-xs text-rose-500 font-semibold">
                      {errors.commissionValue.message}
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmDelete}
        title="Desvincular Funcionário(a)"
        message="Tem certeza que deseja remover este(a) funcionário(a)? O usuário continuará existindo, apenas perderá o vínculo de funcionário(a)."
      />

      {showDetails && selectedEmployee && (
        <div className="fixed inset-0 z-50 flex items-center justify-center overflow-x-hidden overflow-y-auto outline-none focus:outline-none">
          <div
            className="fixed inset-0 bg-[#261f23]/40 backdrop-blur-md transition-opacity duration-300"
            onClick={() => setShowDetails(false)}
          />
          <div className="relative w-full max-w-md mx-auto my-6 z-50 px-4">
            <div className="relative flex flex-col w-full bg-white border border-[#eae1e1] rounded-2xl shadow-xl outline-none focus:outline-none animate-scale-up">
              {/* Header */}
              <div className="flex items-center justify-between p-5 border-b border-solid border-[#eae1e1] rounded-t-2xl">
                <h3 className="text-lg font-semibold font-heading text-[#3b3036]">
                  Detalhes do(a) Funcionário(a)
                </h3>
                <button
                  onClick={() => setShowDetails(false)}
                  className="p-1 ml-auto bg-transparent border-0 text-[#7a7074] hover:text-[#be8a83] float-right text-3xl leading-none font-semibold outline-none focus:outline-none transition-colors cursor-pointer"
                >
                  <span className="text-xl">×</span>
                </button>
              </div>

              {/* Body */}
              <div className="relative p-6 flex-auto space-y-4 text-sm text-[#3b3036]">
                <div>
                  <span className="font-semibold block text-xs text-[#3b3036]/60 uppercase tracking-wider mb-1">
                    Nome
                  </span>
                  <div className="text-base font-semibold text-[#3b3036]">
                    {selectedEmployee.name}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <span className="font-semibold block text-xs text-[#3b3036]/60 uppercase tracking-wider mb-1">
                      ID Usuário
                    </span>
                    <div className="text-base">{selectedEmployee.userId}</div>
                  </div>
                  <div>
                    <span className="font-semibold block text-xs text-[#3b3036]/60 uppercase tracking-wider mb-1">
                      E-mail
                    </span>
                    <div className="text-base break-all">{selectedEmployee.email || '-'}</div>
                  </div>
                </div>

                <div>
                  <span className="font-semibold block text-xs text-[#3b3036]/60 uppercase tracking-wider mb-1">
                    Biografia / Especialidade
                  </span>
                  <div className="bg-[#fcf9f9] border border-[#eae1e1] rounded-xl p-3 text-[#3b3036]/80 italic">
                    {selectedEmployee.bio || 'Nenhuma biografia cadastrada.'}
                  </div>
                </div>

                <div className="border-t border-[#eae1e1] pt-4">
                  <span className="font-semibold block text-xs text-[#3b3036]/60 uppercase tracking-wider mb-2">
                    Configuração de Pagamento
                  </span>
                  <div className="bg-[#fcf9f9] border border-[#eae1e1] rounded-xl p-3 space-y-2">
                    <div>
                      <span className="font-semibold text-xs text-[#3b3036]/70">Tipo:</span>{' '}
                      <span className="text-sm font-semibold">
                        {selectedEmployee.remunerationType === 'SALARIO_FIXO' && 'Salário Fixo'}
                        {selectedEmployee.remunerationType === 'COMISSIONADO' && 'Comissionado'}
                        {selectedEmployee.remunerationType === 'FIXO_E_COMISSIONADO' &&
                          'Salário Fixo + Comissionado'}
                        {!selectedEmployee.remunerationType && 'Não definido'}
                      </span>
                    </div>

                    {selectedEmployee.remunerationType === 'SALARIO_FIXO' && (
                      <div>
                        <span className="font-semibold text-xs text-[#3b3036]/70">
                          Salário Fixo:
                        </span>{' '}
                        <span className="text-sm font-semibold text-[#8b6d68]">
                          R$ {(selectedEmployee.remunerationValue ?? 0).toFixed(2)}
                        </span>
                      </div>
                    )}

                    {selectedEmployee.remunerationType === 'COMISSIONADO' && (
                      <>
                        <div>
                          <span className="font-semibold text-xs text-[#3b3036]/70">Comissão:</span>{' '}
                          <span className="text-sm font-semibold text-[#8b6d68]">
                            {selectedEmployee.remunerationValue ?? 0}%
                          </span>
                        </div>
                        <div>
                          <span className="font-semibold text-xs text-[#3b3036]/70">Escopo:</span>{' '}
                          <span className="text-xs font-semibold px-2 py-0.5 bg-[#eae1e1] rounded-md uppercase">
                            {selectedEmployee.commissionScope === 'GLOBAL'
                              ? 'Global'
                              : 'Individual'}
                          </span>
                        </div>
                      </>
                    )}

                    {selectedEmployee.remunerationType === 'FIXO_E_COMISSIONADO' && (
                      <>
                        <div>
                          <span className="font-semibold text-xs text-[#3b3036]/70">
                            Salário Fixo Base:
                          </span>{' '}
                          <span className="text-sm font-semibold text-[#8b6d68]">
                            R$ {(selectedEmployee.remunerationValue ?? 0).toFixed(2)}
                          </span>
                        </div>
                        <div>
                          <span className="font-semibold text-xs text-[#3b3036]/70">Comissão:</span>{' '}
                          <span className="text-sm font-semibold text-[#8b6d68]">
                            {selectedEmployee.commissionValue ?? 0}%
                          </span>
                        </div>
                        <div>
                          <span className="font-semibold text-xs text-[#3b3036]/70">Escopo:</span>{' '}
                          <span className="text-xs font-semibold px-2 py-0.5 bg-[#eae1e1] rounded-md uppercase">
                            {selectedEmployee.commissionScope === 'GLOBAL'
                              ? 'Global'
                              : 'Individual'}
                          </span>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              </div>

              {/* Footer */}
              <div className="flex items-center justify-end p-5 border-t border-solid border-[#eae1e1] rounded-b-2xl bg-[#fcf9f9]">
                <button
                  type="button"
                  onClick={() => setShowDetails(false)}
                  className="px-6 py-2 bg-[#be8a83] hover:bg-[#a1706a] text-white rounded-xl text-sm font-semibold transition-all duration-200 shadow-md shadow-[#be8a83]/10 cursor-pointer"
                >
                  Fechar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
