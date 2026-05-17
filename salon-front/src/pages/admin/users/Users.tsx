import { useState, useEffect } from 'react';
import { Button, Form } from 'react-bootstrap';
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

  useEffect(() => {
    loadUsers();
  }, []);

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
      setValue('roleId', 4); // Default to CLIENTE
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
    { 
      key: 'active', 
      label: 'Status',
      render: (item: UserData) => item.active ? 'Ativo' : 'Inativo'
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: UserData) => (
        <div className="d-flex gap-2">
          <PermissionGate method="PATCH" endpoint={`/v1/users/${item.id}`}>
            <Button variant="outline-primary" size="sm" onClick={() => handleOpenForm(item)}>
              <Edit size={16} />
            </Button>
          </PermissionGate>
          
          <PermissionGate method="DELETE" endpoint={`/v1/users/${item.id}`}>
            <Button 
              variant="outline-danger" 
              size="sm" 
              onClick={() => {
                setUserToDelete(item.id);
                setShowConfirm(true);
              }}
            >
              <Trash2 size={16} />
            </Button>
          </PermissionGate>
        </div>
      )
    }
  ];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Gerenciar Clientes</h2>
        <PermissionGate method="POST" endpoint="/v1/users">
          <Button variant="primary" onClick={() => handleOpenForm()}>
            <Plus size={18} className="me-2" /> Novo Cliente/Funcionário
          </Button>
        </PermissionGate>
      </div>

      {isLoading ? (
        <p>Carregando dados...</p>
      ) : (
        <Table 
          columns={columns} 
          data={users} 
          keyExtractor={(item) => item.id} 
        />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingUser ? "Editar Conta" : "Nova Conta"}
        onSubmit={handleSubmit(onSubmit)}
      >
        <Form.Group className="mb-3">
          <Form.Label>Nome</Form.Label>
          <Form.Control type="text" {...register('name', { required: true })} />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Email</Form.Label>
          <Form.Control type="email" {...register('email', { required: true })} />
        </Form.Group>
        
        <Form.Group className="mb-3">
          <Form.Label>Tipo de Conta (Papel)</Form.Label>
          <Form.Select {...register('roleId', { required: true })}>
            <option value="4">Cliente</option>
            <option value="3">Funcionária</option>
            <option value="2">Gerente</option>
            <option value="1">Administrador</option>
          </Form.Select>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Telefone</Form.Label>
          <Form.Control type="text" {...register('phone')} />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>{editingUser ? "Nova Senha (opcional)" : "Senha *"}</Form.Label>
          <Form.Control type="password" {...register('password', { required: !editingUser })} />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Check type="switch" label="Conta Ativa" {...register('active')} />
        </Form.Group>
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
