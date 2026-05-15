import { useState, useEffect } from 'react';
import { Card, Badge, Button, Row, Col } from 'react-bootstrap';
import { appointmentsApi } from './services/appointments';
import type { AppointmentResponse } from './services/appointments';
import { ConfirmDialog } from '../../components/modal/ConfirmDialog';

export const MyAppointments = () => {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [appointmentToCancel, setAppointmentToCancel] = useState<number | null>(null);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.getMyAppointments();
      // Sort by date descending
      data.sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime());
      setAppointments(data);
    } catch (error) {
      console.error('Erro ao carregar agendamentos', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAppointments();
  }, []);

  const confirmCancel = async () => {
    if (!appointmentToCancel) return;
    try {
      await appointmentsApi.cancel(appointmentToCancel);
      setShowConfirm(false);
      loadAppointments();
      alert('Agendamento cancelado com sucesso.');
    } catch (error) {
      console.error('Erro ao cancelar agendamento', error);
      alert('Erro ao cancelar agendamento.');
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

  return (
    <div>
      <h2 className="mb-4">Meus Agendamentos</h2>
      
      {isLoading ? (
        <p>Carregando...</p>
      ) : appointments.length === 0 ? (
        <p className="text-muted">Você ainda não tem agendamentos.</p>
      ) : (
        <Row className="g-4">
          {appointments.map(apt => (
            <Col md={6} lg={4} key={apt.id}>
              <Card className="h-100 shadow-sm">
                <Card.Header className="d-flex justify-content-between align-items-center bg-white">
                  <strong>{formatDate(apt.scheduledAt)}</strong>
                  {getStatusBadge(apt.status)}
                </Card.Header>
                <Card.Body>
                  <Card.Title>{apt.serviceName}</Card.Title>
                  <Card.Text>
                    <span className="d-block mb-2">
                      <i className="bi bi-person me-2"></i>
                      Profissional: {apt.employeeName}
                    </span>
                  </Card.Text>
                </Card.Body>
                {apt.status !== 'CANCELLED' && apt.status !== 'DONE' && (
                  <Card.Footer className="bg-white border-top-0">
                    <Button 
                      variant="outline-danger" 
                      size="sm" 
                      className="w-100"
                      onClick={() => {
                        setAppointmentToCancel(apt.id);
                        setShowConfirm(true);
                      }}
                    >
                      Cancelar Agendamento
                    </Button>
                  </Card.Footer>
                )}
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmCancel}
        title="Cancelar Agendamento"
        message="Tem certeza que deseja cancelar este agendamento?"
      />
    </div>
  );
};
