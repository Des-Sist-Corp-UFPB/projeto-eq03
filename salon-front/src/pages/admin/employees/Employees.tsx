import { useState, useEffect } from 'react';
import { Button, Form } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { employeesApi } from './services/employees';
import type { EmployeeData } from './services/employees';

export const Employees = () => {
  const [employees, setEmployees] = useState<EmployeeData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<EmployeeData | null>(null);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [employeeToDelete, setEmployeeToDelete] = useState<number | null>(null);
  
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<EmployeeData>();

  const loadEmployees = async () => {
    setIsLoading(true);
    try {
      const data = await employeesApi.findAll();
      setEmployees(data);
    } catch (error) {
      console.error('Erro ao carregar funcionárias', error);
      alert('Erro ao carregar funcionárias');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadEmployees();
  }, []);

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
    } catch (error) {
      console.error('Erro ao salvar funcionária', error);
      alert('Erro ao salvar funcionária. Verifique se o ID de usuário está correto.');
    }
  };

  const confirmDelete = async () => {
    if (!employeeToDelete) return;
    try {
      await employeesApi.delete(employeeToDelete);
      setShowConfirm(false);
      loadEmployees();
    } catch (error) {
      console.error('Erro ao excluir funcionária', error);
      alert('Erro ao excluir funcionária.');
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
        <div className="d-flex gap-2">
          <PermissionGate method="PUT" endpoint={`/v1/employees/${item.id}`}>
            <Button variant="outline-primary" size="sm" onClick={() => handleOpenForm(item)}>
              <Edit size={16} />
            </Button>
          </PermissionGate>
          
          <PermissionGate method="DELETE" endpoint={`/v1/employees/${item.id}`}>
            <Button 
              variant="outline-danger" 
              size="sm" 
              onClick={() => {
                setEmployeeToDelete(item.id!);
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
        <h2>Gerenciar Funcionárias</h2>
        <PermissionGate method="POST" endpoint="/v1/employees">
          <Button variant="primary" onClick={() => handleOpenForm()}>
            <Plus size={18} className="me-2" />
            Vincular Funcionária
          </Button>
        </PermissionGate>
      </div>

      {isLoading ? (
        <p>Carregando funcionárias...</p>
      ) : (
        <Table 
          columns={columns} 
          data={employees} 
          keyExtractor={(item) => item.id!} 
        />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingEmployee ? 'Editar Funcionária' : 'Nova Funcionária'}
        onSubmit={handleSubmit(onSubmit)}
      >
        <Form.Group className="mb-3">
          <Form.Label>ID do Usuário</Form.Label>
          <Form.Control 
            type="number" 
            {...register('userId', { required: 'ID do usuário é obrigatório' })}
            isInvalid={!!errors.userId}
            disabled={!!editingEmployee}
          />
          <Form.Text className="text-muted">
            Insira o ID do usuário que será vinculado como funcionária.
          </Form.Text>
          <Form.Control.Feedback type="invalid">{errors.userId?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Biografia / Especialidade</Form.Label>
          <Form.Control 
            as="textarea" 
            rows={3} 
            {...register('bio')}
          />
        </Form.Group>
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
