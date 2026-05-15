import { useState, useEffect } from 'react';
import { Button, Form, Badge } from 'react-bootstrap';
import { Table } from '../../../components/table/Table';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { appointmentsApi } from '../../appointments/services/appointments';
import type { AppointmentResponse } from '../../appointments/services/appointments';

export const AdminAppointments = () => {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [appointmentToCancel, setAppointmentToCancel] = useState<number | null>(null);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.findAll();
      data.sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime());
      setAppointments(data);
    } catch (error) {
      console.error('Erro ao carregar agendamentos', error);
      alert('Erro ao carregar agendamentos');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAppointments();
  }, []);

  const handleStatusChange = async (id: number, newStatus: string) => {
    try {
      await appointmentsApi.updateStatus(id, newStatus);
      loadAppointments();
    } catch (error) {
      console.error('Erro ao atualizar status', error);
      alert('Erro ao atualizar status');
    }
  };

  const confirmCancel = async () => {
    if (!appointmentToCancel) return;
    try {
      await appointmentsApi.cancel(appointmentToCancel);
      setShowConfirm(false);
      loadAppointments();
    } catch (error) {
      console.error('Erro ao cancelar', error);
      alert('Erro ao cancelar agendamento');
    }
  };

  const getStatusBadge = (status: string) => {
    switch(status) {
      case 'PENDING': return <Badge bg="warning" text="dark">Pendente</Badge>;
      case 'CONFIRMED': return <Badge bg="primary">Confirmado</Badge>;
      case 'DONE': return <Badge bg="success">Concluído</Badge>;
      case 'CANCELLED': return <Badge bg="danger">Cancelado</Badge>;
      default: return <Badge bg="secondary">{status}</Badge>;
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }).format(date);
  };

  const columns = [
    { 
      key: 'scheduledAt', 
      label: 'Data/Hora',
      render: (item: AppointmentResponse) => formatDate(item.scheduledAt)
    },
    { key: 'clientName', label: 'Cliente' },
    { key: 'employeeName', label: 'Profissional' },
    { key: 'serviceName', label: 'Serviço' },
    { 
      key: 'status', 
      label: 'Status',
      render: (item: AppointmentResponse) => (
        <div className="d-flex align-items-center gap-2">
          {getStatusBadge(item.status)}
          <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/status`}>
            {item.status !== 'CANCELLED' && item.status !== 'DONE' && (
              <Form.Select 
                size="sm" 
                value={item.status} 
                onChange={(e) => handleStatusChange(item.id, e.target.value)}
                style={{ width: '120px' }}
              >
                <option value="PENDING">Pendente</option>
                <option value="CONFIRMED">Confirmado</option>
                <option value="DONE">Concluído</option>
              </Form.Select>
            )}
          </PermissionGate>
        </div>
      )
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: AppointmentResponse) => (
        item.status !== 'CANCELLED' && item.status !== 'DONE' && (
          <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/cancel`}>
            <Button 
              variant="outline-danger" 
              size="sm"
              onClick={() => {
                setAppointmentToCancel(item.id);
                setShowConfirm(true);
              }}
            >
              Cancelar
            </Button>
          </PermissionGate>
        )
      )
    }
  ];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Agendamentos (Admin)</h2>
      </div>

      {isLoading ? (
        <p>Carregando agendamentos...</p>
      ) : (
        <Table 
          columns={columns} 
          data={appointments} 
          keyExtractor={(item) => item.id} 
        />
      )}

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmCancel}
        title="Cancelar Agendamento"
        message="Tem certeza que deseja cancelar este agendamento? Esta ação não pode ser desfeita."
      />
    </div>
  );
};
