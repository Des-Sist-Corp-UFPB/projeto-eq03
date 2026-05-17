import { useState, useEffect } from 'react';
import { Button, Form } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { salonServicesApi, displayServiceDuration } from '../../services/services/services';
import type { SalonServiceData } from '../../services/services/services';
import { useAlert } from '../../../hooks/useAlert';

export const AdminServices = () => {
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [editingService, setEditingService] = useState<SalonServiceData | null>(null);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [serviceToDelete, setServiceToDelete] = useState<number | null>(null);
  
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<SalonServiceData>();
  const { error: showError } = useAlert();

  const loadServices = async () => {
    setIsLoading(true);
    try {
      const data = await salonServicesApi.findAll();
      setServices(data);
    } catch (err) {
      await showError('Erro ao carregar serviços');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadServices();
  }, []);

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
      await showError('Informe o tempo estimado em texto (ex.: em média 50 min) e/ou minutos para encaixe na agenda.');
      return;
    }
    try {
      const payload: SalonServiceData = {
        ...data,
        price: data.price ?? null,
        durationEstimate: hasEst ? data.durationEstimate!.trim() : null,
        durationMin: hasMin ? Number(data.durationMin) : null
      };
      if (editingService?.id) {
        await salonServicesApi.update(editingService.id, payload);
      } else {
        await salonServicesApi.create(payload);
      }
      setShowForm(false);
      loadServices();
    } catch (err) {
      await showError('Erro ao salvar serviço. Verifique os dados e tente novamente.');
    }
  };

  const confirmDelete = async () => {
    if (!serviceToDelete) return;
    try {
      await salonServicesApi.delete(serviceToDelete);
      setShowConfirm(false);
      loadServices();
    } catch (err) {
      await showError('Erro ao excluir serviço.');
    }
  };

  const columns = [
    { key: 'name', label: 'Nome' },
    { 
      key: 'price', 
      label: 'Referência',
      render: (item: SalonServiceData) =>
        item.price != null ? `A partir de R$ ${item.price.toFixed(2)}` : '—'
    },
    { 
      key: 'duration', 
      label: 'Tempo estimado',
      render: (item: SalonServiceData) => displayServiceDuration(item)
    },
    { 
      key: 'active', 
      label: 'Status',
      render: (item: SalonServiceData) => item.active ? 'Ativo' : 'Inativo'
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: SalonServiceData) => (
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
          <Form.Label>Valor de referência — &quot;a partir de&quot; (opcional)</Form.Label>
          <Form.Control 
            type="number" 
            step="0.01" 
            min="0"
            placeholder="Deixe em branco se o valor for combinado ou lançado só no caixa"
            {...register('price', {
              setValueAs: (v) => (v === '' || v === undefined || v === null ? undefined : Number(v))
            })}
          />
          <Form.Text className="text-muted">
            O preço final pode ser registrado no fluxo de caixa ao concluir o atendimento.
          </Form.Text>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Tempo estimado (mostrado ao cliente)</Form.Label>
          <Form.Control 
            type="text" 
            placeholder="Ex.: Em média 50 min · Em média 1h20"
            {...register('durationEstimate')}
          />
          <Form.Text className="text-muted">
            Texto livre. Obrigatório informar isto ou os minutos abaixo (ou ambos).
          </Form.Text>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Minutos para encaixe na agenda (opcional)</Form.Label>
          <Form.Control 
            type="number" 
            min={1}
            placeholder="Só números — ajuda a evitar sobreposição de horários"
            {...register('durationMin', {
              setValueAs: (v) => (v === '' || v === undefined || v === null ? undefined : Number(v))
            })}
          />
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
