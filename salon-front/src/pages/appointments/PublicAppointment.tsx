import { useState, useEffect } from 'react';
import { Form, Alert, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { Clock, User as UserIcon, Calendar, CheckCircle, ArrowLeft, ArrowRight, MessageSquare, CalendarHeart } from 'lucide-react';
import { salonServicesApi, displayServiceDuration } from '../services/services/services';
import type { SalonServiceData } from '../services/services/services';
import { employeesApi } from '../admin/employees/services/employees';
import type { EmployeeData } from '../admin/employees/services/employees';
import { appointmentsApi } from './services/appointments';
import { useAuth } from '../../hooks/useAuth';
import './PublicAppointment.css';
import { getApiErrorMessage } from '../../utils/apiError';
import { featureFlagsService } from '../../services/featureFlags';

function priceTagLabel(price: number | null | undefined): string | null {
  if (price == null || Number.isNaN(price)) return null;
  return `A partir de R$ ${price.toFixed(2)}`;
}

function localTodayIso(): string {
  const n = new Date();
  const y = n.getFullYear();
  const m = String(n.getMonth() + 1).padStart(2, '0');
  const d = String(n.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export const PublicAppointment = () => {
  const [step, setStep] = useState(1);
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [employees, setEmployees] = useState<EmployeeData[]>([]);

  const [selectedService, setSelectedService] = useState<number | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<number | null>(null);
  const [preferredDate, setPreferredDate] = useState<string>('');
  const [clientNotes, setClientNotes] = useState<string>('');

  const [isLoading, setIsLoading] = useState(false);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [isBookingEnabled, setIsBookingEnabled] = useState(true);

  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const raw = localStorage.getItem('pending_appointment');
    if (raw) {
      try {
        const p = JSON.parse(raw) as {
          serviceId?: number;
          employeeId?: number;
          preferredDate?: string;
          clientNotes?: string;
        };
        if (p.serviceId) setSelectedService(p.serviceId);
        if (p.employeeId) setSelectedEmployee(p.employeeId);
        if (p.preferredDate) setPreferredDate(p.preferredDate);
        if (p.clientNotes) setClientNotes(p.clientNotes);
        if (p.serviceId && p.employeeId) setStep(4);
        else if (p.serviceId) setStep(2);
      } catch {
        /* ignore */
      }
    }
  }, []);

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const [servicesData, employeesData, flagsData] = await Promise.all([
          salonServicesApi.findAll(),
          employeesApi.findAllForBooking(),
          featureFlagsService.getPublicFlags().catch(() => [] as any[])
        ]);
        setServices(servicesData.filter(s => s.active));
        setEmployees(employeesData);
        const bookingFlag = flagsData.find(f => f.name === 'CLIENT_BOOKING');
        if (bookingFlag && !bookingFlag.enabled) {
          setIsBookingEnabled(false);
        }
      } catch (error) {
        const msg = getApiErrorMessage(error, 'Não foi possível carregar serviços ou profissionais. Tente novamente em instantes.');
        setErrorMsg(msg);
      } finally {
        setIsInitialLoading(false);
      }
    };
    fetchInitialData();
  }, []);

  const handleNext = () => {
    if (step === 1 && !selectedService) return;
    if (step === 2 && !selectedEmployee) return;
    if (step === 3) {
      if (preferredDate && preferredDate < localTodayIso()) {
        setErrorMsg('A data de preferência deve ser hoje ou uma data futura.');
        return;
      }
      setErrorMsg('');
    }
    setStep(step + 1);
    window.scrollTo(0, 0);
  };

  const handleBack = () => {
    setStep(step - 1);
    window.scrollTo(0, 0);
  };

  const handleSubmit = async () => {
    if (!isAuthenticated) {
      localStorage.setItem('pending_appointment', JSON.stringify({
        serviceId: selectedService,
        employeeId: selectedEmployee,
        preferredDate: preferredDate || undefined,
        clientNotes: clientNotes || undefined
      }));
      navigate('/login');
      return;
    }

    setIsLoading(true);
    setErrorMsg('');
    try {
      await appointmentsApi.create({
        serviceId: selectedService!,
        employeeId: selectedEmployee!,
        preferredDate: preferredDate || undefined,
        clientNotes: clientNotes.trim() || undefined
      });
      localStorage.removeItem('pending_appointment');
      navigate('/my-appointments');
    } catch (error) {
      const msg = getApiErrorMessage(error, 'Erro ao enviar solicitação.');
      setErrorMsg(msg);
    } finally {
      setIsLoading(false);
    }
  };

  if (isInitialLoading) {
    return (
      <div className="d-flex justify-content-center align-items-center min-vh-100">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (!isBookingEnabled) {
    return (
      <div className="appointment-container text-center fade-in-up py-5">
        <div className="p-5 bg-white shadow-sm border" style={{ borderRadius: '16px', maxWidth: '600px', margin: '40px auto' }}>
          <CalendarHeart size={64} className="text-muted mb-4" />
          <h3 className="mb-3 text-secondary">Agendamentos Online Desativados</h3>
          <p className="text-muted mb-4" style={{ lineHeight: '1.6' }}>
            Os agendamentos online para clientes estão temporariamente desativados no momento. Por favor, entre em contato direto com o salão para agendar seu horário.
          </p>
          <button className="btn btn-primary btn-lg" onClick={() => navigate('/')}>
            Voltar para o início
          </button>
        </div>
      </div>
    );
  }

  const steps = [
    { n: 1, label: 'Serviço', icon: <Clock size={16} /> },
    { n: 2, label: 'Profissional', icon: <UserIcon size={16} /> },
    { n: 3, label: 'Preferências', icon: <Calendar size={16} /> },
    { n: 4, label: 'Enviar', icon: <CheckCircle size={16} /> }
  ];

  const selectedSrv = services.find(s => s.id === selectedService);
  const priceLabel = priceTagLabel(selectedSrv?.price);

  return (
    <div className="appointment-container fade-in-up">
      <div className="appointment-header">
        <h2>Solicitar horário</h2>
        <p className="text-muted">
          Monte seu pedido abaixo. O salão confirma data e horário e avisa você por aqui.
        </p>
      </div>

      <div className="stepper">
        {steps.map((s) => (
          <div key={s.n} className={`step-item ${step === s.n ? 'active' : ''} ${step > s.n ? 'completed' : ''}`}>
            <div className="step-number">
              {step > s.n ? <CheckCircle size={20} /> : s.n}
            </div>
            <div className="step-label">{s.label}</div>
          </div>
        ))}
      </div>

      <div className="appointment-content">
        {errorMsg && <Alert variant="danger" className="mb-4">{errorMsg}</Alert>}

        {step === 1 && (
          <div>
            <h4 className="mb-4 text-center">O que vamos fazer?</h4>
            <div className="selection-grid">
              {services.map(srv => {
                const tag = priceTagLabel(srv.price);
                return (
                  <div
                    key={srv.id}
                    className={`option-card ${selectedService === srv.id ? 'selected' : ''}`}
                    onClick={() => setSelectedService(srv.id!)}
                  >
                    {tag ? <span className="price-tag">{tag}</span> : null}
                    <h5 className="mb-1">{srv.name}</h5>
                    <p className="text-muted small mb-2">{srv.description || 'Tratamento especializado para você.'}</p>
                    <div className="duration">
                      <Clock size={14} />
                      {displayServiceDuration(srv)}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {step === 2 && (
          <div>
            <h4 className="mb-4 text-center">Com quem você prefere?</h4>
            <div className="selection-grid">
              {employees.map(emp => (
                <div
                  key={emp.id}
                  className={`option-card ${selectedEmployee === emp.id ? 'selected' : ''}`}
                  onClick={() => setSelectedEmployee(emp.id!)}
                >
                  <div className="d-flex align-items-center gap-3">
                    <div className="avatar-placeholder">
                      {(emp.name ?? 'P').charAt(0)}
                    </div>
                    <div>
                      <h5 className="mb-0">{emp.name}</h5>
                      <p className="text-muted small mb-0">{emp.bio || 'Profissional especialista'}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {step === 3 && (
          <div style={{ maxWidth: '600px', margin: '0 auto' }}>
            <h4 className="mb-4 text-center">Preferência de dia e observações</h4>
            <p className="text-muted small text-center mb-4">
              O horário exato será combinado pela equipe. Indique um dia de preferência, se quiser.
            </p>
            <Form.Group className="mb-4">
              <Form.Label className="fw-bold d-flex align-items-center gap-2">
                <Calendar size={18} /> Dia de preferência (opcional)
              </Form.Label>
              <Form.Control
                type="date"
                className="custom-input"
                min={localTodayIso()}
                value={preferredDate}
                onChange={(e) => setPreferredDate(e.target.value)}
              />
            </Form.Group>
            <Form.Group className="mb-2">
              <Form.Label className="fw-bold d-flex align-items-center gap-2">
                <MessageSquare size={18} /> Observações (opcional)
              </Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                className="custom-input"
                placeholder="Ex.: só de manhã, comentários sobre o cabelo, etc."
                value={clientNotes}
                onChange={(e) => setClientNotes(e.target.value)}
              />
            </Form.Group>
          </div>
        )}

        {step === 4 && (
          <div style={{ maxWidth: '600px', margin: '0 auto' }}>
            <h4 className="mb-4 text-center">Revisar pedido</h4>
            <div className="summary-card">
              <div className="summary-header">
                <CheckCircle size={40} className="mb-2" />
                <h5>Resumo da solicitação</h5>
              </div>
              <div className="summary-body">
                <div className="summary-row">
                  <span className="summary-label">Serviço</span>
                  <span className="summary-value">{selectedSrv?.name}</span>
                </div>
                <div className="summary-row">
                  <span className="summary-label">Profissional</span>
                  <span className="summary-value">{employees.find(e => e.id === selectedEmployee)?.name}</span>
                </div>
                {preferredDate ? (
                  <div className="summary-row">
                    <span className="summary-label">Dia preferido</span>
                    <span className="summary-value">
                      {new Date(preferredDate + 'T12:00:00').toLocaleDateString('pt-BR')}
                    </span>
                  </div>
                ) : null}
                {clientNotes.trim() ? (
                  <div className="summary-row summary-row--notes">
                    <span className="summary-label">Observações</span>
                    <p className="summary-notes">{clientNotes.trim()}</p>
                  </div>
                ) : null}
                {priceLabel ? (
                  <div className="summary-row summary-row--divider">
                    <span className="summary-label fw-bold text-dark">Referência</span>
                    <span className="summary-value text-primary fs-6">{priceLabel}</span>
                  </div>
                ) : (
                  <div className="summary-row summary-row--divider">
                    <span className="summary-label text-muted">Valor</span>
                    <span className="summary-value text-muted fs-6">Definido no atendimento / caixa</span>
                  </div>
                )}
              </div>
            </div>

            {!isAuthenticated && (
              <Alert variant="warning" className="mt-4">
                Você precisará entrar na sua conta para enviar a solicitação.
              </Alert>
            )}
          </div>
        )}

        <div className="nav-footer">
          <button
            className="btn-nav btn-outline-custom d-flex align-items-center gap-2"
            onClick={handleBack}
            disabled={step === 1 || isLoading}
          >
            <ArrowLeft size={18} />
            Voltar
          </button>

          {step < 4 ? (
            <button
              className="btn-nav btn-primary-custom d-flex align-items-center gap-2"
              onClick={handleNext}
              disabled={(step === 1 && !selectedService) || (step === 2 && !selectedEmployee)}
            >
              Próximo
              <ArrowRight size={18} />
            </button>
          ) : (
            <button
              className="btn-nav btn-primary-custom d-flex align-items-center gap-2"
              onClick={handleSubmit}
              disabled={isLoading}
            >
              {isLoading ? 'Enviando...' : 'Enviar solicitação'}
              <CheckCircle size={18} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
