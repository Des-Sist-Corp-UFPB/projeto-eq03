import { useState, useEffect } from 'react';
import { Button, Form } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { servicesApi } from '../../services/services/services';
import type { ServiceData } from '../../services/services/services';

export const AdminServices = () => {
  const [services, setServices] = useState<ServiceData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [editingService, setEditingService] = useState<ServiceData | null>(null);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [serviceToDelete, setServiceToDelete] = useState<number | null>(null);
  
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<ServiceData>();

  const loadServices = async () => {
    setIsLoading(true);
    try {
      const data = await servicesApi.findAll();
      setServices(data);
    } catch (error) {
      console.error('Erro ao carregar serviços', error);
      alert('Erro ao carregar serviços');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadServices();
  }, []);

  const handleOpenForm = (service?: ServiceData) => {
    reset();
    if (service) {
      setEditingService(service);
      setValue('name', service.name);
      setValue('description', service.description);
      setValue('price', service.price);
      setValue('durationMin', service.durationMin);
      setValue('active', service.active);
    } else {
      setEditingService(null);
      setValue('active', true);
    }
    setShowForm(true);
  };

  const onSubmit = async (data: ServiceData) => {
    try {
      if (editingService?.id) {
        await servicesApi.update(editingService.id, data);
      } else {
        await servicesApi.create(data);
      }
      setShowForm(false);
      loadServices();
    } catch (error) {
      console.error('Erro ao salvar serviço', error);
      alert('Erro ao salvar serviço. Verifique os dados e tente novamente.');
    }
  };

  const confirmDelete = async () => {
    if (!serviceToDelete) return;
    try {
      await servicesApi.delete(serviceToDelete);
      setShowConfirm(false);
      loadServices();
    } catch (error) {
      console.error('Erro ao excluir serviço', error);
      alert('Erro ao excluir serviço.');
    }
  };

  const columns = [
    { key: 'name', label: 'Nome' },
    { 
      key: 'price', 
      label: 'Preço',
      render: (item: ServiceData) => `R$ ${item.price.toFixed(2)}`
    },
    { 
      key: 'durationMin', 
      label: 'Duração (min)',
      render: (item: ServiceData) => `${item.durationMin} min`
    },
    { 
      key: 'active', 
      label: 'Status',
      render: (item: ServiceData) => item.active ? 'Ativo' : 'Inativo'
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: ServiceData) => (
        <div className="d-flex gap-2">
          <PermissionGate method="PUT" endpoint={`/v1/services/${item.id}`}>
            <Button variant="outline-primary" size="sm" onClick={() => handleOpenForm(item)}>
              <Edit size={16} />
            </Button>
          </PermissionGate>
          
          <PermissionGate method="DELETE" endpoint={`/v1/services/${item.id}`}>
            <Button 
              variant="outline-danger" 
              size="sm" 
              onClick={() => {
                setServiceToDelete(item.id!);
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
        <h2>Gerenciar Serviços</h2>
        <PermissionGate method="POST" endpoint="/v1/services">
          <Button variant="primary" onClick={() => handleOpenForm()}>
            <Plus size={18} className="me-2" />
            Novo Serviço
          </Button>
        </PermissionGate>
      </div>

      {isLoading ? (
        <p>Carregando serviços...</p>
      ) : (
        <Table 
          columns={columns} 
          data={services} 
          keyExtractor={(item) => item.id!} 
        />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingService ? 'Editar Serviço' : 'Novo Serviço'}
        onSubmit={handleSubmit(onSubmit)}
      >
        <Form.Group className="mb-3">
          <Form.Label>Nome do Serviço</Form.Label>
          <Form.Control 
            type="text" 
            {...register('name', { required: 'Nome é obrigatório', minLength: { value: 3, message: 'Mín. 3 caracteres'} })}
            isInvalid={!!errors.name}
          />
          <Form.Control.Feedback type="invalid">{errors.name?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Descrição</Form.Label>
          <Form.Control 
            as="textarea" 
            rows={3} 
            {...register('description')}
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Preço (R$)</Form.Label>
          <Form.Control 
            type="number" 
            step="0.01" 
            {...register('price', { required: 'Preço é obrigatório', min: { value: 0, message: 'Não pode ser negativo'} })}
            isInvalid={!!errors.price}
          />
          <Form.Control.Feedback type="invalid">{errors.price?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Duração (minutos)</Form.Label>
          <Form.Control 
            type="number" 
            {...register('durationMin', { required: 'Duração é obrigatória', min: { value: 1, message: 'Mín. 1 minuto'} })}
            isInvalid={!!errors.durationMin}
          />
          <Form.Control.Feedback type="invalid">{errors.durationMin?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Check 
            type="switch"
            label="Serviço Ativo"
            {...register('active')}
          />
        </Form.Group>
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
