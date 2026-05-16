import { useState, useEffect } from 'react';
import { Container, Form, Alert, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { Clock, User as UserIcon, Calendar, CheckCircle, ArrowLeft, ArrowRight } from 'lucide-react';
import { salonServicesApi } from '../services/services/services';
import type { SalonServiceData } from '../services/services/services';
import { employeesApi } from '../admin/employees/services/employees';
import type { EmployeeData } from '../admin/employees/services/employees';
import { appointmentsApi } from './services/appointments';
import type { TimeSlotResponse } from './services/appointments';
import { useAuth } from '../../hooks/useAuth';
import './PublicAppointment.css';

export const PublicAppointment = () => {
  const [step, setStep] = useState(1);
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [employees, setEmployees] = useState<EmployeeData[]>([]);
  const [slots, setSlots] = useState<TimeSlotResponse[]>([]);
  
  const [selectedService, setSelectedService] = useState<number | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<number | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>('');
  const [selectedTime, setSelectedTime] = useState<string>('');
  
  const [isLoading, setIsLoading] = useState(false);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');

  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const [servicesData, employeesData] = await Promise.all([
          salonServicesApi.findAll(),
          employeesApi.findAll()
        ]);
        setServices(servicesData.filter(s => s.active));
        setEmployees(employeesData);
      } catch (error) {
        console.error('Erro ao buscar dados', error);
      } finally {
        setIsInitialLoading(false);
      }
    };
    fetchInitialData();
  }, []);

  useEffect(() => {
    if (selectedEmployee && selectedDate) {
      loadSlots();
    }
  }, [selectedEmployee, selectedDate]);

  const loadSlots = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.getSlots(selectedDate, selectedEmployee!);
      setSlots(data);
    } catch (error) {
      console.error('Erro ao buscar horários', error);
      setSlots([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleNext = () => {
    if (step === 1 && !selectedService) return;
    if (step === 2 && !selectedEmployee) return;
    if (step === 3 && (!selectedDate || !selectedTime)) return;
    setStep(step + 1);
    window.scrollTo(0, 0);
  };

  const handleBack = () => {
    setStep(step - 1);
    window.scrollTo(0, 0);
  };

  const handleConfirm = async () => {
    if (!isAuthenticated) {
      localStorage.setItem('pending_appointment', JSON.stringify({
        serviceId: selectedService,
        employeeId: selectedEmployee,
        date: selectedDate,
        time: selectedTime
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
        scheduledAt: `${selectedDate}T${selectedTime}`
      });
      navigate('/my-appointments');
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || 'Erro ao agendar.');
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

  const steps = [
    { n: 1, label: 'Serviço', icon: <Clock size={16} /> },
    { n: 2, label: 'Profissional', icon: <UserIcon size={16} /> },
    { n: 3, label: 'Data e Hora', icon: <Calendar size={16} /> },
    { n: 4, label: 'Confirmar', icon: <CheckCircle size={16} /> }
  ];

  return (
    <div className="appointment-container fade-in-up">
      <div className="appointment-header">
        <h2>Reserve seu Momento</h2>
        <p className="text-muted">Siga os passos abaixo para agendar seu atendimento exclusivo.</p>
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
            <h4 className="mb-4 text-center">O que vamos fazer hoje?</h4>
            <div className="selection-grid">
              {services.map(srv => (
                <div 
                  key={srv.id} 
                  className={`option-card ${selectedService === srv.id ? 'selected' : ''}`}
                  onClick={() => setSelectedService(srv.id!)}
                >
                  <span className="price-tag">R$ {srv.price.toFixed(2)}</span>
                  <h5 className="mb-1">{srv.name}</h5>
                  <p className="text-muted small mb-2">{srv.description || 'Tratamento especializado para você.'}</p>
                  <div className="duration">
                    <Clock size={14} />
                    {srv.durationMin} minutos
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {step === 2 && (
          <div>
            <h4 className="mb-4 text-center">Com quem você deseja agendar?</h4>
            <div className="selection-grid">
              {employees.map(emp => (
                <div 
                  key={emp.id} 
                  className={`option-card ${selectedEmployee === emp.id ? 'selected' : ''}`}
                  onClick={() => setSelectedEmployee(emp.id!)}
                >
                  <div className="d-flex align-items-center gap-3">
                    <div className="avatar-placeholder">
                      {emp.name.charAt(0)}
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
            <h4 className="mb-4 text-center">Quando seria melhor para você?</h4>
            <Form.Group className="mb-5">
              <Form.Label className="fw-bold">Selecione o dia</Form.Label>
              <Form.Control 
                type="date" 
                className="custom-input"
                min={new Date().toISOString().split('T')[0]}
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
              />
            </Form.Group>
            
            {selectedDate && (
              <div>
                <Form.Label className="fw-bold mb-3">Horários disponíveis</Form.Label>
                {isLoading ? (
                  <div className="text-center py-4">
                    <Spinner animation="border" size="sm" variant="primary" />
                  </div>
                ) : slots.length === 0 ? (
                  <Alert variant="info">Nenhum horário disponível para esta data.</Alert>
                ) : (
                  <div className="slots-container">
                    {slots.map((slot, i) => (
                      <button
                        key={i}
                        className={`slot-btn ${selectedTime === slot.time ? 'selected' : ''}`}
                        disabled={!slot.available}
                        onClick={() => setSelectedTime(slot.time)}
                      >
                        {slot.time.substring(0, 5)}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {step === 4 && (
          <div style={{ maxWidth: '600px', margin: '0 auto' }}>
            <h4 className="mb-4 text-center">Tudo certo?</h4>
            <div className="summary-card">
              <div className="summary-header">
                <CheckCircle size={40} className="mb-2" />
                <h5>Resumo do Agendamento</h5>
              </div>
              <div className="summary-body">
                <div className="summary-item">
                  <span className="summary-label">Serviço</span>
                  <span className="summary-value">{services.find(s => s.id === selectedService)?.name}</span>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Profissional</span>
                  <span className="summary-value">{employees.find(e => e.id === selectedEmployee)?.name}</span>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Data</span>
                  <span className="summary-value">{new Date(selectedDate + 'T00:00:00').toLocaleDateString('pt-BR')}</span>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Horário</span>
                  <span className="summary-value">{selectedTime.substring(0, 5)}</span>
                </div>
                <div className="summary-item pt-3">
                  <span className="summary-label fw-bold text-dark">Total</span>
                  <span className="summary-value text-primary fs-5">R$ {services.find(s => s.id === selectedService)?.price.toFixed(2)}</span>
                </div>
              </div>
            </div>
            
            {!isAuthenticated && (
              <Alert variant="warning" className="mt-4">
                Você precisará entrar na sua conta para finalizar o agendamento.
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
              disabled={(step === 1 && !selectedService) || (step === 2 && !selectedEmployee) || (step === 3 && (!selectedDate || !selectedTime))}
            >
              Próximo
              <ArrowRight size={18} />
            </button>
          ) : (
            <button 
              className="btn-nav btn-primary-custom d-flex align-items-center gap-2"
              onClick={handleConfirm} 
              disabled={isLoading}
            >
              {isLoading ? 'Confirmando...' : 'Finalizar Agendamento'}
              <CheckCircle size={18} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

