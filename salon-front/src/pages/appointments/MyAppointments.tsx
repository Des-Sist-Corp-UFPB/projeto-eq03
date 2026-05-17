import { useState, useEffect } from 'react';
import { Badge, Button, Row, Col } from 'react-bootstrap';
import { appointmentsApi } from './services/appointments';
import type { AppointmentResponse } from './services/appointments';
import { ConfirmDialog } from '../../components/modal/ConfirmDialog';
import './Appointments.css';
import { useAlert } from '../../hooks/useAlert';

export const MyAppointments = () => {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [showConfirm, setShowConfirm] = useState(false);
  const [appointmentToCancel, setAppointmentToCancel] = useState<number | null>(null);

  const parseInstant = (dateValue: string | number[] | null | undefined): number => {
    if (!dateValue) return 0;
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour, minute] = dateValue as number[];
      return new Date(year, month - 1, day, hour, minute).getTime();
    }
    return new Date(dateValue as string).getTime();
  };

  const sortKey = (apt: AppointmentResponse): number => {
    if (apt.scheduledAt) return parseInstant(apt.scheduledAt);
    if (apt.preferredDate) return new Date(apt.preferredDate + 'T12:00:00').getTime();
    return 0;
  };

  const { error: showError } = useAlert();

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.getMyAppointments();
      data.sort((a, b) => sortKey(b) - sortKey(a));
      setAppointments(data);
    } catch (error) {
      await showError('Erro ao carregar agendamentos');
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
    } catch (error) {
      await showError('Erro ao cancelar agendamento.');
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Badge className="status-badge pending">Pendente</Badge>;
      case 'REQUESTED':
        return <Badge className="status-badge requested">Aguardando salão</Badge>;
      case 'CONFIRMED':
        return <Badge className="status-badge confirmed">Confirmado</Badge>;
      case 'DECLINED':
        return <Badge className="status-badge declined">Não atendido</Badge>;
      case 'DONE':
        return <Badge className="status-badge done">Concluído</Badge>;
      case 'CANCELLED':
        return <Badge className="status-badge cancelled">Cancelado</Badge>;
      default:
        return <Badge bg="secondary">{status}</Badge>;
    }
  };

  const formatDate = (dateValue: string | number[] | null | undefined) => {
    let date: Date;
    if (!dateValue) {
      return { dayStr: '--', timeStr: '--', yearStr: '----' };
    }
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour, minute] = dateValue as number[];
      date = new Date(year, month - 1, day, hour, minute);
    } else {
      date = new Date(dateValue);
    }

    if (isNaN(date.getTime())) {
      return { dayStr: '--', timeStr: '--', yearStr: '----' };
    }

    return {
      dayStr: new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(date),
      timeStr: new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(date),
      yearStr: new Intl.DateTimeFormat('pt-BR', { year: 'numeric' }).format(date),
    };
  };

  const dateColumnFor = (apt: AppointmentResponse) => {
    if (apt.scheduledAt) {
      const d = formatDate(apt.scheduledAt);
      return (
        <>
          <span className="date-time">{d.timeStr}</span>
          <div className="date-day">
            <span className="day">{d.dayStr}</span>
            <span className="year">{d.yearStr}</span>
          </div>
        </>
      );
    }
    if (apt.preferredDate) {
      const d = new Date(apt.preferredDate + 'T12:00:00');
      return (
        <>
          <span className="date-time" style={{ fontSize: '1rem' }}>Pref.</span>
          <div className="date-day">
            <span className="day">{d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })}</span>
            <span className="year">{d.getFullYear()}</span>
          </div>
        </>
      );
    }
    return (
      <>
        <span className="date-time" style={{ fontSize: '1rem' }}>—</span>
        <div className="date-day">
          <span className="day">A combinar</span>
          <span className="year">salão</span>
        </div>
      </>
    );
  };

  return (
    <div className="appointments-section">
      <div className="section-header">
        <h2 className="section-title">Minha Agenda</h2>
        <p className="section-subtitle">Acompanhe solicitações e horários confirmados</p>
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
          {appointments.map(apt => (
            <Col lg={6} xl={4} key={apt.id}>
              <div className={`appointment-card ${apt.status.toLowerCase()}`}>
                <div className="appointment-date">
                  {dateColumnFor(apt)}
                </div>

                <div className="appointment-content">
                  <div className="d-flex justify-content-between align-items-start mb-3">
                    <h4 className="service-name">{apt.serviceName}</h4>
                    {getStatusBadge(apt.status)}
                  </div>

                  {apt.clientNotes ? (
                    <p className="small text-muted mb-2" style={{ lineHeight: 1.4 }}>
                      {apt.clientNotes}
                    </p>
                  ) : null}

                  <div className="professional-info">
                    <span className="prof-label">Profissional</span>
                    <span className="prof-name">{apt.employeeName || 'Não especificado'}</span>
                  </div>

                  {apt.status !== 'CANCELLED' && apt.status !== 'DONE' && apt.status !== 'DECLINED' && (
                    <div className="appointment-actions">
                      <Button
                        variant="none"
                        className="btn-cancel-custom"
                        onClick={() => {
                          setAppointmentToCancel(apt.id);
                          setShowConfirm(true);
                        }}
                      >
                        Cancelar
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            </Col>
          ))}
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
