import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Edit, Trash2, Plus, RotateCcw } from 'lucide-react';
import { DataTable } from '../../../components/table/DataTable';
import type { FilterField } from '../../../components/table/DataTable';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { clientsApi } from './services/clients';
import type { ClientFilter } from './services/clients';
import { usersApi } from '../users/services/users';
import type { UserData, UserCreateRequest, UserUpdateRequest } from '../users/services/users';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';
import { ClientDrawer } from './components/ClientDrawer';

const labelCls = 'label-premium';

export const Clients = () => {
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const [showForm, setShowForm] = useState(false);
  const [editingClient, setEditingClient] = useState<UserData | null>(null);

  const [showConfirm, setShowConfirm] = useState(false);
  const [targetClientId, setTargetClientId] = useState<number | null>(null);
  const [confirmAction, setConfirmAction] = useState<'delete' | 'restore'>('delete');

  // Drawer states
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<UserCreateRequest & UserUpdateRequest>();
  const { error: showError } = useAlert();

  const handleOpenForm = (client?: UserData) => {
    reset();
    if (client) {
      setEditingClient(client);
      setValue('name', client.name);
      setValue('email', client.email);
      setValue('phone', client.phone);
      setValue('cpf', client.cpf || '');
      setValue('active', client.active);
      setValue('roleId', 4); // Client role ID
    } else {
      setEditingClient(null);
      setValue('active', true);
      setValue('roleId', 4); // Client role ID
    }
    setShowForm(true);
  };

  const onSubmit = async (data: UserCreateRequest & UserUpdateRequest) => {
    try {
      // Force roleId to be 4 (Cliente)
      const payload = {
        ...data,
        roleId: 4,
      };

      if (editingClient && !payload.password) {
        delete payload.password;
      }

      if (editingClient?.id) {
        await usersApi.update(editingClient.id, payload);
      } else {
        await usersApi.create(payload as UserCreateRequest);
      }
      setShowForm(false);
      setRefreshTrigger((prev) => prev + 1);
    } catch (error: any) {
      if (error.response?.status === 400 && error.response.data?.errors) {
        const fieldErrors = error.response.data.errors;
        Object.keys(fieldErrors).forEach((field) => {
          setError(field as any, { type: 'server', message: fieldErrors[field] });
        });
      } else if (error.response?.status === 409) {
        const msg = error.response.data?.message || 'E-mail ou CPF já cadastrado.';
        if (msg.toLowerCase().includes('cpf')) {
          setError('cpf', { type: 'server', message: msg });
        } else {
          setError('email', { type: 'server', message: msg });
        }
      } else {
        const msg = getApiErrorMessage(error, 'Erro ao salvar cliente.');
        await showError(msg);
      }
    }
  };

  const handleConfirmAction = async () => {
    if (!targetClientId) return;
    try {
      if (confirmAction === 'delete') {
        await usersApi.delete(targetClientId);
      } else {
        await usersApi.restore(targetClientId);
      }
      setShowConfirm(false);
      setRefreshTrigger((prev) => prev + 1);
    } catch (error) {
      const fallbackMsg =
        confirmAction === 'delete' ? 'Erro ao desativar cliente.' : 'Erro ao reativar cliente.';
      const msg = getApiErrorMessage(error, fallbackMsg);
      await showError(msg);
    }
  };

  const getStatusBadge = (active: boolean) => {
    const className = active
      ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20'
      : 'bg-rose-50 text-rose-700 border border-rose-200 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20';
    return (
      <span
        className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap ${className}`}
      >
        {active ? 'Ativo' : 'Inativo'}
      </span>
    );
  };

  const handleRowClick = (client: UserData) => {
    setSelectedClientId(client.id);
    setIsDrawerOpen(true);
  };

  const columns = [
    { key: 'name', label: 'Nome' },
    { key: 'email', label: 'Email' },
    { key: 'phone', label: 'Telefone', render: (item: UserData) => item.phone || 'Não informado' },
    { key: 'cpf', label: 'CPF', render: (item: UserData) => item.cpf || 'Não informado' },
    {
      key: 'active',
      label: 'Status',
      render: (item: UserData) => getStatusBadge(item.active),
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: UserData) => (
        <div className="flex gap-2">
          <PermissionGate method="PATCH" endpoint={`/v1/users/${item.id}`}>
            <button
              onClick={(e) => {
                e.stopPropagation(); // Prevents opening the drawer when clicking the button
                handleOpenForm(item);
              }}
              className="p-1.5 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/20 border border-indigo-200 dark:border-indigo-800 rounded-lg transition-all cursor-pointer"
              title="Editar Cliente"
            >
              <Edit size={15} />
            </button>
          </PermissionGate>
          {item.active ? (
            <PermissionGate method="DELETE" endpoint={`/v1/users/${item.id}`}>
              <button
                onClick={(e) => {
                  e.stopPropagation(); // Prevents opening the drawer when clicking the button
                  setTargetClientId(item.id);
                  setConfirmAction('delete');
                  setShowConfirm(true);
                }}
                className="p-1.5 text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/20 border border-rose-200 dark:border-rose-800 rounded-lg transition-all cursor-pointer flex items-center gap-1 text-xs font-semibold"
                title="Desativar Cliente"
              >
                <Trash2 size={15} />
                <span>Desativar</span>
              </button>
            </PermissionGate>
          ) : (
            <PermissionGate method="PATCH" endpoint={`/v1/users/${item.id}/restore`}>
              <button
                onClick={(e) => {
                  e.stopPropagation(); // Prevents opening the drawer when clicking the button
                  setTargetClientId(item.id);
                  setConfirmAction('restore');
                  setShowConfirm(true);
                }}
                className="p-1.5 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800 rounded-lg transition-all cursor-pointer flex items-center gap-1 text-xs font-semibold"
                title="Reativar Cliente"
              >
                <RotateCcw size={15} />
                <span>Reativar</span>
              </button>
            </PermissionGate>
          )}
        </div>
      ),
    },
  ];

  const filtersConfig: FilterField[] = [
    { key: 'name', label: 'Nome', type: 'text' },
    { key: 'email', label: 'Email', type: 'text' },
    { key: 'phone', label: 'Telefone', type: 'text' },
    { key: 'cpf', label: 'CPF', type: 'text' },
    { key: 'active', label: 'Status', type: 'boolean' },
  ];

  const initialFilters: ClientFilter = {
    name: '',
    email: '',
    phone: '',
    cpf: '',
    active: undefined,
  };

  const fetchClientsData = async (filter: ClientFilter, page: number, size: number) => {
    return clientsApi.findAll(filter, page, size);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="font-heading text-2xl font-bold text-[#3b3036] dark:text-white">
          Gerenciar Clientes
        </h2>
        <PermissionGate method="POST" endpoint="/v1/users">
          <button onClick={() => handleOpenForm()} className="btn-premium font-semibold">
            <Plus size={18} /> Novo Cliente
          </button>
        </PermissionGate>
      </div>

      <DataTable
        columns={columns}
        fetchData={fetchClientsData}
        filtersConfig={filtersConfig}
        keyExtractor={(item) => item.id}
        onRowClick={handleRowClick}
        refreshTrigger={refreshTrigger}
        initialFilters={initialFilters}
      />

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingClient ? 'Editar Cliente' : 'Novo Cliente'}
        onSubmit={handleSubmit(onSubmit)}
        isSubmitting={isSubmitting}
      >
        <div className="space-y-4">
          <div>
            <label className={labelCls}>Nome Completo *</label>
            <input
              type="text"
              className={`input-premium ${errors.name ? 'border-rose-300 focus:border-rose-500' : ''}`}
              {...register('name', {
                required: 'Nome é obrigatório',
                minLength: { value: 3, message: 'Mínimo 3 caracteres' },
              })}
            />
            {errors.name && (
              <span className="text-xs text-rose-500 font-semibold">{errors.name.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>Email *</label>
            <input
              type="email"
              className={`input-premium ${errors.email ? 'border-rose-300 focus:border-rose-500' : ''}`}
              {...register('email', {
                required: 'Email é obrigatório',
                pattern: {
                  value: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
                  message: 'Formato de e-mail inválido',
                },
              })}
            />
            {errors.email && (
              <span className="text-xs text-rose-500 font-semibold">{errors.email.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>Telefone</label>
            <input
              type="text"
              className={`input-premium ${errors.phone ? 'border-rose-300 focus:border-rose-500' : ''}`}
              {...register('phone', {
                pattern: {
                  value: /^$|^\(?\d{2}\)?\s?\d{4,5}-?\d{4}$/,
                  message: 'Formato inválido. Use (XX) XXXXX-XXXX',
                },
              })}
            />
            {errors.phone && (
              <span className="text-xs text-rose-500 font-semibold">{errors.phone.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>CPF</label>
            <input
              type="text"
              className={`input-premium ${errors.cpf ? 'border-rose-300 focus:border-rose-500' : ''}`}
              {...register('cpf', {
                pattern: {
                  value: /^$|^\d{11}$/,
                  message: 'O CPF deve conter exatamente 11 dígitos numéricos',
                },
              })}
            />
            {errors.cpf && (
              <span className="text-xs text-rose-500 font-semibold">{errors.cpf.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>
              {editingClient ? 'Nova Senha (opcional)' : 'Senha *'}
            </label>
            <input
              type="password"
              className={`input-premium ${errors.password ? 'border-rose-300 focus:border-rose-500' : ''}`}
              {...register('password', {
                validate: (val) => {
                  if (!val) {
                    return editingClient ? true : 'Senha é obrigatória';
                  }
                  if (val.length < 8) return 'A senha deve ter no mínimo 8 caracteres';
                  if (!/\d/.test(val)) return 'A senha deve conter pelo menos um número';
                  return true;
                },
              })}
            />
            {errors.password && (
              <span className="text-xs text-rose-500 font-semibold">
                {errors.password.message}
              </span>
            )}
          </div>
          <div className="flex items-center gap-3">
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" {...register('active')} />
              <div className="w-10 h-5 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#be8a83]"></div>
            </label>
            <span className="text-sm font-semibold text-[#3b3036] dark:text-gray-200">
              Conta Ativa
            </span>
          </div>
        </div>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={handleConfirmAction}
        title={confirmAction === 'delete' ? 'Desativar Cliente' : 'Reativar Cliente'}
        message={
          confirmAction === 'delete'
            ? 'Tem certeza que deseja desativar este cliente? O acesso dele será bloqueado.'
            : 'Tem certeza que deseja reativar este cliente? O acesso dele será restaurado.'
        }
        confirmLabel={confirmAction === 'delete' ? 'Desativar' : 'Reativar'}
        variant={confirmAction === 'delete' ? 'danger' : 'primary'}
      />

      <ClientDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        clientId={selectedClientId}
      />
    </div>
  );
};
