import { useState, useEffect } from 'react';
import { Button, Form, Row, Col, Badge } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { Plus, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { cashFlowApi, CashFlowData } from './services/cashflow';

export const CashFlow = () => {
  const [cashFlows, setCashFlows] = useState<CashFlowData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<number | null>(null);

  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  
  const { register, handleSubmit, reset, formState: { errors } } = useForm<CashFlowData>();

  const loadCashFlows = async () => {
    setIsLoading(true);
    try {
      const data = await cashFlowApi.findByPeriod(dateFrom || undefined, dateTo || undefined);
      setCashFlows(data);
    } catch (error) {
      console.error('Erro ao carregar fluxo de caixa', error);
      alert('Erro ao carregar fluxo de caixa');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadCashFlows();
  }, [dateFrom, dateTo]);

  const handleOpenForm = () => {
    reset({
      type: 'INCOME',
      date: new Date().toISOString().split('T')[0]
    });
    setShowForm(true);
  };

  const onSubmit = async (data: CashFlowData) => {
    try {
      await cashFlowApi.create(data);
      setShowForm(false);
      loadCashFlows();
    } catch (error) {
      console.error('Erro ao salvar registro', error);
      alert('Erro ao salvar registro no fluxo de caixa.');
    }
  };

  const confirmDelete = async () => {
    if (!itemToDelete) return;
    try {
      await cashFlowApi.delete(itemToDelete);
      setShowConfirm(false);
      loadCashFlows();
    } catch (error) {
      console.error('Erro ao excluir registro', error);
      alert('Erro ao excluir registro.');
    }
  };

  const columns = [
    { 
      key: 'date', 
      label: 'Data',
      render: (item: CashFlowData) => new Date(item.date).toLocaleDateString('pt-BR')
    },
    { key: 'description', label: 'Descrição' },
    { 
      key: 'type', 
      label: 'Tipo',
      render: (item: CashFlowData) => (
        item.type === 'INCOME' 
          ? <Badge bg="success">Entrada</Badge> 
          : <Badge bg="danger">Saída</Badge>
      )
    },
    { 
      key: 'amount', 
      label: 'Valor',
      render: (item: CashFlowData) => (
        <span className={item.type === 'INCOME' ? 'text-success' : 'text-danger'}>
          {item.type === 'INCOME' ? '+' : '-'} R$ {item.amount.toFixed(2)}
        </span>
      )
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: CashFlowData) => (
        <PermissionGate method="DELETE" endpoint={`/v1/cashflow/${item.id}`}>
          <Button 
            variant="outline-danger" 
            size="sm" 
            onClick={() => {
              setItemToDelete(item.id!);
              setShowConfirm(true);
            }}
          >
            <Trash2 size={16} />
          </Button>
        </PermissionGate>
      )
    }
  ];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Fluxo de Caixa</h2>
        <PermissionGate method="POST" endpoint="/v1/cashflow">
          <Button variant="primary" onClick={handleOpenForm}>
            <Plus size={18} className="me-2" />
            Novo Registro
          </Button>
        </PermissionGate>
      </div>

      <Row className="mb-4">
        <Col md={3}>
          <Form.Group>
            <Form.Label>De</Form.Label>
            <Form.Control type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group>
            <Form.Label>Até</Form.Label>
            <Form.Control type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6} className="d-flex align-items-end">
          <Button variant="outline-secondary" onClick={() => { setDateFrom(''); setDateTo(''); }}>
            Limpar Filtros
          </Button>
        </Col>
      </Row>

      {isLoading ? (
        <p>Carregando registros...</p>
      ) : (
        <Table 
          columns={columns} 
          data={cashFlows} 
          keyExtractor={(item) => item.id!} 
        />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title="Novo Registro"
        onSubmit={handleSubmit(onSubmit)}
      >
        <Form.Group className="mb-3">
          <Form.Label>Tipo</Form.Label>
          <Form.Select {...register('type', { required: 'Tipo é obrigatório' })}>
            <option value="INCOME">Entrada (Receita)</option>
            <option value="EXPENSE">Saída (Despesa)</option>
          </Form.Select>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Valor (R$)</Form.Label>
          <Form.Control 
            type="number" 
            step="0.01" 
            {...register('amount', { required: 'Valor é obrigatório', min: { value: 0.01, message: 'Valor inválido' } })}
            isInvalid={!!errors.amount}
          />
          <Form.Control.Feedback type="invalid">{errors.amount?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Descrição</Form.Label>
          <Form.Control 
            type="text" 
            {...register('description', { required: 'Descrição é obrigatória' })}
            isInvalid={!!errors.description}
          />
          <Form.Control.Feedback type="invalid">{errors.description?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Data</Form.Label>
          <Form.Control 
            type="date" 
            {...register('date', { required: 'Data é obrigatória' })}
            isInvalid={!!errors.date}
          />
          <Form.Control.Feedback type="invalid">{errors.date?.message}</Form.Control.Feedback>
        </Form.Group>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmDelete}
        title="Excluir Registro"
        message="Tem certeza que deseja excluir este registro? Esta ação afetará os relatórios."
      />
    </div>
  );
};
