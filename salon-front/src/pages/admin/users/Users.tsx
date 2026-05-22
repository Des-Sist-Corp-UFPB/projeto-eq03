import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Edit, Trash2, Plus } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { usersApi } from './services/users';
import type { UserData, UserUpdateRequest, UserCreateRequest } from './services/users';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const Users = () => {
  const [users, setUsers] = useState<UserData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState<UserData | null>(null);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [userToDelete, setUserToDelete] = useState<number | null>(null);
  
  const { register, handleSubmit, reset, setValue } = useForm<UserCreateRequest & UserUpdateRequest>();
  const { error: showError } = useAlert();

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      const data = await usersApi.findAll();
      setUsers(data);
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao carregar usuários');
      await showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadUsers(); }, []);

  const handleOpenForm = (user?: UserData) => {
    reset();
    if (user) {
      setEditingUser(user);
      setValue('name', user.name);
      setValue('email', user.email);
      setValue('phone', user.phone);
      setValue('active', user.active);
      setValue('roleId', getRoleIdByName(user.role));
    } else {
      setEditingUser(null);
      setValue('active', true);
      setValue('roleId', 4);
    }
    setShowForm(true);
  };

  const getRoleIdByName = (roleName: string) => {
    switch (roleName) {
      case 'ADMIN': return 1;
      case 'GERENTE_DE_ATENDIMENTO': return 2;
      case 'FUNCIONARIA': return 3;
      case 'CLIENTE': return 4;
      default: return 4;
    }
  };

  const onSubmit = async (data: UserCreateRequest & UserUpdateRequest) => {
    try {
      if (editingUser?.id) {
        await usersApi.update(editingUser.id, data);
      } else {
        await usersApi.create(data as UserCreateRequest);
      }
      setShowForm(false);
      loadUsers();
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao salvar. Verifique os dados e tente novamente.');
      await showError(msg);
    }
  };

  const confirmDelete = async () => {
    if (!userToDelete) return;
    try {
      await usersApi.delete(userToDelete);
      setShowConfirm(false);
      loadUsers();
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao excluir usuário.');
      await showError(msg);
    }
  };

  const columns = [
    { key: 'name', label: 'Nome' },
    { key: 'email', label: 'Email' },
    { key: 'role', label: 'Papel' },
    { key: 'active', label: 'Status', render: (item: UserData) => item.active ? 'Ativo' : 'Inativo' },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: UserData) => (
        <div className="flex gap-2">
          <PermissionGate method="PATCH" endpoint={`/v1/users/${item.id}`}>
            <button
              onClick={() => handleOpenForm(item)}
              className="p-1.5 text-indigo-600 hover:bg-indigo-50 border border-indigo-200 rounded-lg transition-all cursor-pointer"
            >
              <Edit size={15} />
            </button>
          </PermissionGate>
          <PermissionGate method="DELETE" endpoint={`/v1/users/${item.id}`}>
            <button
              onClick={() => { setUserToDelete(item.id); setShowConfirm(true); }}
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
        <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Gerenciar Clientes</h2>
        <PermissionGate method="POST" endpoint="/v1/users">
          <button
            onClick={() => handleOpenForm()}
            className="btn-premium"
          >
            <Plus size={18} /> Novo Cliente/Funcionário
          </button>
        </PermissionGate>
      </div>

      {isLoading ? (
        <div className="flex items-center gap-2 text-sm text-[#3b3036]/60 py-8">
          <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-[#be8a83]"></div>
          Carregando dados...
        </div>
      ) : (
        <Table columns={columns} data={users} keyExtractor={(item) => item.id} />
      )}

      <ModalForm show={showForm} onHide={() => setShowForm(false)} title={editingUser ? 'Editar Conta' : 'Nova Conta'} onSubmit={handleSubmit(onSubmit)}>
        <div className="space-y-4">
          <div>
            <label className={labelCls}>Nome</label>
            <input type="text" className={inputCls} {...register('name', { required: true })} />
          </div>
          <div>
            <label className={labelCls}>Email</label>
            <input type="email" className={inputCls} {...register('email', { required: true })} />
          </div>
          <div>
            <label className={labelCls}>Tipo de Conta (Papel)</label>
            <select className={inputCls} {...register('roleId', { required: true })}>
              <option value="4">Cliente</option>
              <option value="3">Funcionária</option>
              <option value="2">Gerente</option>
              <option value="1">Administrador</option>
            </select>
          </div>
          <div>
            <label className={labelCls}>Telefone</label>
            <input type="text" className={inputCls} {...register('phone')} />
          </div>
          <div>
            <label className={labelCls}>{editingUser ? 'Nova Senha (opcional)' : 'Senha *'}</label>
            <input type="password" className={inputCls} {...register('password', { required: !editingUser })} />
          </div>
          <div className="flex items-center gap-3">
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" {...register('active')} />
              <div className="w-10 h-5 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#be8a83]"></div>
            </label>
            <span className="text-sm font-semibold text-[#3b3036]">Conta Ativa</span>
          </div>
        </div>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmDelete}
        title="Excluir Conta"
        message="Tem certeza que deseja excluir esta conta? Esta ação não pode ser desfeita."
      />
    </div>
  );
};
