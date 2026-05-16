import { useState, useEffect } from 'react';
import { Button, Form, Badge, Modal, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { Plus, Clock, User as UserIcon, Calendar as CalendarIcon } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { appointmentsApi } from '../../appointments/services/appointments';
import type { AppointmentResponse, TimeSlotResponse } from '../../appointments/services/appointments';
import { salonServicesApi } from '../../services/services/services';
import type { SalonServiceData } from '../../services/services/services';
import { employeesApi } from '../employees/services/employees';
import type { EmployeeData } from '../employees/services/employees';
import { usersApi } from '../users/services/users';
import type { UserData } from '../users/services/users';

export const AdminAppointments = () => {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  // Modal states
  const [showModal, setShowModal] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [clients, setClients] = useState<UserData[]>([]);
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [employees, setEmployees] = useState<EmployeeData[]>([]);
  const [slots, setSlots] = useState<TimeSlotResponse[]>([]);
  const [loadingSlots, setLoadingSlots] = useState(false);

  // Form states
  const [selectedClient, setSelectedClient] = useState('');
  const [selectedService, setSelectedService] = useState('');
  const [selectedEmployee, setSelectedEmployee] = useState('');
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedTime, setSelectedTime] = useState('');

  const [showConfirm, setShowConfirm] = useState(false);
  const [appointmentToCancel, setAppointmentToCancel] = useState<number | null>(null);

  const parseDate = (dateValue: any): number => {
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour, minute] = dateValue;
      return new Date(year, month - 1, day, hour, minute).getTime();
    }
    return new Date(dateValue).getTime();
  };

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.findAll();
      data.sort((a, b) => parseDate(b.scheduledAt) - parseDate(a.scheduledAt));
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

  useEffect(() => {
    if (selectedEmployee && selectedDate) {
      const fetchSlots = async () => {
        setLoadingSlots(true);
        try {
          const data = await appointmentsApi.getSlots(selectedDate, Number(selectedEmployee));
          setSlots(data);
        } catch (error) {
          console.error('Erro ao carregar slots', error);
          setSlots([]);
        } finally {
          setLoadingSlots(false);
        }
      };
      fetchSlots();
    }
  }, [selectedEmployee, selectedDate]);

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
    if (!selectedClient || !selectedService || !selectedEmployee || !selectedDate || !selectedTime) {
      alert('Preencha todos os campos');
      return;
    }

    setIsSaving(true);
    try {
      await appointmentsApi.create({
        clientId: Number(selectedClient),
        serviceId: Number(selectedService),
        employeeId: Number(selectedEmployee),
        scheduledAt: `${selectedDate}T${selectedTime}`
      });
      setShowModal(false);
      loadAppointments();
      resetForm();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao criar agendamento');
    } finally {
      setIsSaving(false);
    }
  };

  const resetForm = () => {
    setSelectedClient('');
    setSelectedService('');
    setSelectedEmployee('');
    setSelectedDate('');
    setSelectedTime('');
    setSlots([]);
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

  const formatDate = (dateValue: any) => {
    if (!dateValue) return 'Data não definida';
    
    let date: Date;
    if (Array.isArray(dateValue)) {
      // Handle array format [year, month, day, hour, minute]
      const [year, month, day, hour, minute] = dateValue;
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

      {/* Modal Novo Agendamento */}
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
                      <option key={s.id} value={s.id}>{s.name} (R$ {s.price.toFixed(2)})</option>
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
                  <Form.Label><CalendarIcon size={16} className="me-1" /> Data</Form.Label>
                  <Form.Control 
                    type="date" 
                    value={selectedDate} 
                    onChange={(e) => setSelectedDate(e.target.value)}
                    min={new Date().toISOString().split('T')[0]}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>

            <div className="mt-3">
              <Form.Label className="fw-bold">Horários Disponíveis</Form.Label>
              {loadingSlots ? (
                <div className="py-2"><Spinner animation="border" size="sm" /></div>
              ) : !selectedDate || !selectedEmployee ? (
                <p className="text-muted small">Selecione profissional e data para ver os horários.</p>
              ) : slots.length === 0 ? (
                <Alert variant="info" className="py-2">Sem horários para esta data.</Alert>
              ) : (
                <div className="d-flex flex-wrap gap-2">
                  {slots.map((slot, i) => (
                    <Button
                      key={i}
                      variant={selectedTime === slot.time ? 'primary' : 'outline-secondary'}
                      size="sm"
                      disabled={!slot.available}
                      onClick={() => setSelectedTime(slot.time)}
                    >
                      {slot.time.substring(0, 5)}
                    </Button>
                  ))}
                </div>
              )}
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancelar</Button>
            <Button variant="primary" type="submit" disabled={isSaving || !selectedTime}>
              {isSaving ? 'Salvando...' : 'Criar Agendamento'}
            </Button>
          </Modal.Footer>
        </Form>
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

