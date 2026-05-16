import { useState, useEffect } from 'react';
import { Button, Form, Badge, Modal, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { Plus, Clock, User as UserIcon, Calendar as CalendarIcon } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { appointmentsApi } from '../../appointments/services/appointments';
import type { AppointmentResponse } from '../../appointments/services/appointments';
import { salonServicesApi } from '../../services/services/services';
import type { SalonServiceData } from '../../services/services/services';
import { employeesApi } from '../employees/services/employees';
import type { EmployeeData } from '../employees/services/employees';
import { usersApi } from '../users/services/users';
import type { UserData } from '../users/services/users';

function toLocalDateTimeIso(dtLocal: string): string {
  if (!dtLocal) return '';
  return dtLocal.length === 16 ? `${dtLocal}:00` : dtLocal;
}

function formatServiceOption(s: SalonServiceData): string {
  const ref = s.price != null ? ` — a partir de R$ ${s.price.toFixed(2)}` : '';
  return `${s.name}${ref}`;
}

export const AdminAppointments = () => {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [showModal, setShowModal] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [clients, setClients] = useState<UserData[]>([]);
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [employees, setEmployees] = useState<EmployeeData[]>([]);

  const [selectedClient, setSelectedClient] = useState('');
  const [selectedService, setSelectedService] = useState('');
  const [selectedEmployee, setSelectedEmployee] = useState('');
  const [selectedDateTime, setSelectedDateTime] = useState('');

  const [showConfirm, setShowConfirm] = useState(false);
  const [appointmentToCancel, setAppointmentToCancel] = useState<number | null>(null);

  const [confirmTarget, setConfirmTarget] = useState<AppointmentResponse | null>(null);
  const [confirmDateTime, setConfirmDateTime] = useState('');
  const [confirmSaving, setConfirmSaving] = useState(false);

  const parseDate = (dateValue: string | unknown[] | null | undefined): number => {
    if (!dateValue) return 0;
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour, minute] = dateValue as unknown as number[];
      return new Date(year, month - 1, day, hour, minute).getTime();
    }
    return new Date(dateValue as string).getTime();
  };

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.findAll();
      data.sort((a, b) => {
        const ta = a.scheduledAt
          ? parseDate(a.scheduledAt)
          : a.preferredDate
            ? new Date(a.preferredDate + 'T12:00:00').getTime()
            : 0;
        const tb = b.scheduledAt
          ? parseDate(b.scheduledAt)
          : b.preferredDate
            ? new Date(b.preferredDate + 'T12:00:00').getTime()
            : 0;
        return tb - ta;
      });
      setAppointments(data);
    } catch (error) {
      console.error('Erro ao carregar agendamentos', error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadFormData = async () => {
    try {
      const [usersData, servicesData, employeesData] = await Promise.all([
        usersApi.findAll(),
        salonServicesApi.findAll(),
        employeesApi.findAll()
      ]);
      setClients(usersData.filter(u => u.role === 'CLIENTE'));
      setServices(servicesData.filter(s => s.active));
      setEmployees(employeesData);
    } catch (error) {
      console.error('Erro ao carregar dados do formulário', error);
    }
  };

  useEffect(() => {
    loadAppointments();
    loadFormData();
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

  const handleCreateAppointment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClient || !selectedService || !selectedEmployee || !selectedDateTime) {
      alert('Preencha todos os campos, incluindo data e hora');
      return;
    }

    setIsSaving(true);
    try {
      await appointmentsApi.create({
        clientId: Number(selectedClient),
        serviceId: Number(selectedService),
        employeeId: Number(selectedEmployee),
        scheduledAt: toLocalDateTimeIso(selectedDateTime)
      });
      setShowModal(false);
      loadAppointments();
      resetForm();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Erro ao criar agendamento');
    } finally {
      setIsSaving(false);
    }
  };

  const resetForm = () => {
    setSelectedClient('');
    setSelectedService('');
    setSelectedEmployee('');
    setSelectedDateTime('');
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

  const openConfirmModal = (item: AppointmentResponse) => {
    setConfirmTarget(item);
    setConfirmDateTime('');
  };

  const submitConfirm = async () => {
    if (!confirmTarget || !confirmDateTime) return;
    setConfirmSaving(true);
    try {
      await appointmentsApi.confirm(confirmTarget.id, toLocalDateTimeIso(confirmDateTime));
      setConfirmTarget(null);
      loadAppointments();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Erro ao confirmar horário');
    } finally {
      setConfirmSaving(false);
    }
  };

  const handleDecline = async (id: number) => {
    if (!window.confirm('Recusar esta solicitação?')) return;
    try {
      await appointmentsApi.decline(id);
      loadAppointments();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Erro ao recusar');
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Badge bg="warning" text="dark">Pendente</Badge>;
      case 'REQUESTED':
        return <Badge bg="info" text="dark">Solicitado</Badge>;
      case 'CONFIRMED':
        return <Badge bg="primary">Confirmado</Badge>;
      case 'DECLINED':
        return <Badge bg="secondary">Recusado</Badge>;
      case 'DONE':
        return <Badge bg="success">Concluído</Badge>;
      case 'CANCELLED':
        return <Badge bg="danger">Cancelado</Badge>;
      default:
        return <Badge bg="secondary">{status}</Badge>;
    }
  };

  const formatDate = (dateValue: string | unknown[] | null | undefined) => {
    if (!dateValue) return '—';
    let date: Date;
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour, minute] = dateValue as unknown as number[];
      date = new Date(year, month - 1, day, hour, minute);
    } else {
      date = new Date(dateValue);
    }
    if (isNaN(date.getTime())) return 'Data inválida';
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }).format(date);
  };

  const columns = [
    {
      key: 'scheduledAt',
      label: 'Data / hora',
      render: (item: AppointmentResponse) =>
        item.scheduledAt
          ? formatDate(item.scheduledAt)
          : item.preferredDate
            ? `Pref.: ${new Date(item.preferredDate + 'T12:00:00').toLocaleDateString('pt-BR')} (a combinar)`
            : 'A combinar'
    },
    { key: 'clientName', label: 'Cliente' },
    { key: 'employeeName', label: 'Profissional' },
    { key: 'serviceName', label: 'Serviço' },
    {
      key: 'notes',
      label: 'Obs.',
      render: (item: AppointmentResponse) => (
        <span className="small text-muted" style={{ maxWidth: 220, display: 'inline-block' }}>
          {item.clientNotes ? `${item.clientNotes.slice(0, 80)}${item.clientNotes.length > 80 ? '…' : ''}` : '—'}
        </span>
      )
    },
    {
      key: 'status',
      label: 'Status',
      render: (item: AppointmentResponse) => (
        <div className="d-flex flex-column align-items-start gap-2">
          {getStatusBadge(item.status)}
          {item.status === 'REQUESTED' && (
            <div className="d-flex flex-wrap gap-1">
              <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/confirm`}>
                <Button size="sm" variant="primary" onClick={() => openConfirmModal(item)}>
                  Definir horário
                </Button>
              </PermissionGate>
              <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/decline`}>
                <Button size="sm" variant="outline-danger" onClick={() => handleDecline(item.id)}>
                  Recusar
                </Button>
              </PermissionGate>
            </div>
          )}
          <PermissionGate method="PATCH" endpoint={`/v1/appointments/${item.id}/status`}>
            {item.status !== 'CANCELLED' && item.status !== 'DONE' && item.status !== 'DECLINED' && item.status !== 'REQUESTED' && (
              <Form.Select
                size="sm"
                value={item.status}
                onChange={(e) => handleStatusChange(item.id, e.target.value)}
                style={{ width: '140px' }}
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
        item.status !== 'CANCELLED' && item.status !== 'DONE' && item.status !== 'DECLINED' && (
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
        <PermissionGate method="POST" endpoint="/v1/appointments">
          <Button variant="primary" onClick={() => setShowModal(true)}>
            <Plus size={18} className="me-2" />
            Novo Agendamento
          </Button>
        </PermissionGate>
      </div>

      {isLoading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
          <p className="mt-2">Carregando agendamentos...</p>
        </div>
      ) : (
        <Table
          columns={columns}
          data={appointments}
          keyExtractor={(item) => item.id?.toString() || Math.random().toString()}
        />
      )}

      <Modal show={showModal} onHide={() => setShowModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Novo Agendamento</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateAppointment}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label><UserIcon size={16} className="me-1" /> Cliente</Form.Label>
                  <Form.Select
                    value={selectedClient}
                    onChange={(e) => setSelectedClient(e.target.value)}
                    required
                  >
                    <option value="">Selecione o cliente</option>
                    {clients.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label><Clock size={16} className="me-1" /> Serviço</Form.Label>
                  <Form.Select
                    value={selectedService}
                    onChange={(e) => setSelectedService(e.target.value)}
                    required
                  >
                    <option value="">Selecione o serviço</option>
                    {services.map(s => (
                      <option key={s.id} value={s.id}>{formatServiceOption(s)}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
            </Row>

            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label><UserIcon size={16} className="me-1" /> Profissional</Form.Label>
                  <Form.Select
                    value={selectedEmployee}
                    onChange={(e) => setSelectedEmployee(e.target.value)}
                    required
                  >
                    <option value="">Selecione a profissional</option>
                    {employees.map(e => (
                      <option key={e.id} value={e.id}>{e.name}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label><CalendarIcon size={16} className="me-1" /> Data e hora</Form.Label>
                  <Form.Control
                    type="datetime-local"
                    value={selectedDateTime}
                    onChange={(e) => setSelectedDateTime(e.target.value)}
                    required
                  />
                  <Form.Text>Horário livre — sem grade fixa no sistema.</Form.Text>
                </Form.Group>
              </Col>
            </Row>
            <Alert variant="light" className="border small mb-0">
              O agendamento nasce já <strong>confirmado</strong>. Clientes pelo site enviam uma <strong>solicitação</strong> para você aceitar e marcar o horário.
            </Alert>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)}>Fechar</Button>
            <Button variant="primary" type="submit" disabled={isSaving}>
              {isSaving ? 'Salvando...' : 'Criar Agendamento'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={!!confirmTarget} onHide={() => !confirmSaving && setConfirmTarget(null)} centered>
        <Modal.Header closeButton={!confirmSaving}>
          <Modal.Title>Confirmar horário</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="small text-muted mb-3">
            Defina data e hora para {confirmTarget?.clientName}. Conflitos com outros agendamentos confirmados do mesmo profissional serão bloqueados.
          </p>
          <Form.Group>
            <Form.Label>Data e hora</Form.Label>
            <Form.Control
              type="datetime-local"
              value={confirmDateTime}
              onChange={(e) => setConfirmDateTime(e.target.value)}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setConfirmTarget(null)} disabled={confirmSaving}>Cancelar</Button>
          <Button variant="primary" onClick={submitConfirm} disabled={confirmSaving || !confirmDateTime}>
            {confirmSaving ? 'Salvando...' : 'Confirmar solicitação'}
          </Button>
        </Modal.Footer>
      </Modal>

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
