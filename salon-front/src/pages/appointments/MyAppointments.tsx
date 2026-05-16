import { useState, useEffect } from 'react';
import { Badge, Button, Row, Col } from 'react-bootstrap';
import { appointmentsApi } from './services/appointments';
import type { AppointmentResponse } from './services/appointments';
import { ConfirmDialog } from '../../components/modal/ConfirmDialog';
import './Appointments.css';

export const MyAppointments = () => {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [appointmentToCancel, setAppointmentToCancel] = useState<number | null>(null);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.getMyAppointments();
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
      // Em produção usaríamos toast, vamos simplificar
    } catch (error) {
      console.error('Erro ao cancelar agendamento', error);
      alert('Erro ao cancelar agendamento.');
    }
  };

  const getStatusBadge = (status: string) => {
    switch(status) {
      case 'PENDING': return <Badge className="status-badge pending">Pendente</Badge>;
      case 'CONFIRMED': return <Badge className="status-badge confirmed">Confirmado</Badge>;
      case 'DONE': return <Badge className="status-badge done">Concluído</Badge>;
      case 'CANCELLED': return <Badge className="status-badge cancelled">Cancelado</Badge>;
      default: return <Badge bg="secondary">{status}</Badge>;
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return {
      dayStr: new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(date),
      timeStr: new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(date),
      yearStr: new Intl.DateTimeFormat('pt-BR', { year: 'numeric'}).format(date),
    }
  };

  return (
    <div className="appointments-section">
      <div className="section-header">
        <h2 className="section-title">Minha Agenda</h2>
        <p className="section-subtitle">Gerencie seus próximos horários no salão</p>
      </div>
      
      {isLoading ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Buscando seus horários...</p>
        </div>
      ) : appointments.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📅</div>
          <h3>Nenhum agendamento encontrado</h3>
          <p>Você ainda não marcou nenhum serviço conosco.</p>
        </div>
      ) : (
        <Row className="g-4">
          {appointments.map(apt => {
            const dateObj = formatDate(apt.scheduledAt);
            return (
              <Col lg={6} xl={4} key={apt.id}>
                <div className={`appointment-card ${apt.status.toLowerCase()}`}>
                  <div className="appointment-date">
                    <span className="date-time">{dateObj.timeStr}</span>
                    <div className="date-day">
                      <span className="day">{dateObj.dayStr}</span>
                      <span className="year">{dateObj.yearStr}</span>
                    </div>
                  </div>
                  
                  <div className="appointment-content">
                    <div className="d-flex justify-content-between align-items-start mb-3">
                      <h4 className="service-name">{apt.serviceName}</h4>
                      {getStatusBadge(apt.status)}
                    </div>
                    
                    <div className="professional-info">
                      <span className="prof-label">Profissional</span>
                      <span className="prof-name">{apt.employeeName || 'Não especificado'}</span>
                    </div>

                    {apt.status !== 'CANCELLED' && apt.status !== 'DONE' && (
                      <div className="appointment-actions">
                        <Button 
                          variant="none" 
                          className="btn-cancel-custom"
                          onClick={() => {
                            setAppointmentToCancel(apt.id);
                            setShowConfirm(true);
                          }}
                        >
                          Cancelar agendamento
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              </Col>
            );
          })}
        </Row>
      )}

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmCancel}
        title="Cancelar Horário"
        message="Puxa, tem certeza que deseja cancelar este agendamento? Esta ação não pode ser desfeita."
      />
    </div>
  );
};
