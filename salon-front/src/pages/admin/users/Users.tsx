import { useState, useEffect } from 'react';
import { Button, Form } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { usersApi } from './services/users';
import type { UserData, UserUpdateRequest } from './services/users';

export const Users = () => {
  const [users, setUsers] = useState<UserData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState<UserData | null>(null);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [userToDelete, setUserToDelete] = useState<number | null>(null);
  
  const { register, handleSubmit, reset, setValue } = useForm<UserUpdateRequest>();

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      const data = await usersApi.findAll();
      setUsers(data);
    } catch (error) {
      console.error('Erro ao carregar usuários', error);
      alert('Erro ao carregar usuários');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleOpenForm = (user: UserData) => {
    reset();
    setEditingUser(user);
    setValue('name', user.name);
    setValue('email', user.email);
    setValue('phone', user.phone);
    setValue('active', user.active);
    setShowForm(true);
  };

  const onSubmit = async (data: UserUpdateRequest) => {
    try {
      if (editingUser?.id) {
        await usersApi.update(editingUser.id, data);
      }
      setShowForm(false);
      loadUsers();
    } catch (error) {
      console.error('Erro ao salvar usuário', error);
      alert('Erro ao salvar usuário. Verifique os dados e tente novamente.');
    }
  };

  const confirmDelete = async () => {
    if (!userToDelete) return;
    try {
      await usersApi.delete(userToDelete);
      setShowConfirm(false);
      loadUsers();
    } catch (error) {
      console.error('Erro ao excluir usuário', error);
      alert('Erro ao excluir usuário.');
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
        <h2>Gerenciar Usuários</h2>
      </div>

      {isLoading ? (
        <p>Carregando usuários...</p>
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
        title="Editar Usuário"
        onSubmit={handleSubmit(onSubmit)}
      >
        <Form.Group className="mb-3">
          <Form.Label>Nome</Form.Label>
          <Form.Control type="text" {...register('name')} />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Email</Form.Label>
          <Form.Control type="email" {...register('email')} />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Telefone</Form.Label>
          <Form.Control type="text" {...register('phone')} />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Nova Senha (deixe em branco para não alterar)</Form.Label>
          <Form.Control type="password" {...register('password')} />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Check type="switch" label="Usuário Ativo" {...register('active')} />
        </Form.Group>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmDelete}
        title="Excluir Usuário"
        message="Tem certeza que deseja excluir este usuário? Esta ação não pode ser desfeita."
      />
    </div>
  );
};
