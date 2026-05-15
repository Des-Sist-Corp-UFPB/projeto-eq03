import { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { servicesApi } from '../services/services/services';
import type { ServiceData } from '../services/services/services';
import { employeesApi } from '../admin/employees/services/employees';
import type { EmployeeData } from '../admin/employees/services/employees';
import { appointmentsApi } from './services/appointments';
import type { TimeSlotResponse } from './services/appointments';
import { useAuth } from '../../hooks/useAuth';

export const PublicAppointment = () => {
  const [step, setStep] = useState(1);
  const [services, setServices] = useState<ServiceData[]>([]);
  const [employees, setEmployees] = useState<EmployeeData[]>([]);
  const [slots, setSlots] = useState<TimeSlotResponse[]>([]);
  
  const [selectedService, setSelectedService] = useState<number | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<number | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>('');
  const [selectedTime, setSelectedTime] = useState<string>('');
  
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const [servicesData, employeesData] = await Promise.all([
          servicesApi.findAll(),
          employeesApi.findAll()
        ]);
        setServices(servicesData.filter(s => s.active));
        setEmployees(employeesData);
      } catch (error) {
        console.error('Erro ao buscar dados', error);
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
    if (step === 1 && !selectedService) return alert('Selecione um serviço');
    if (step === 2 && !selectedEmployee) return alert('Selecione um profissional');
    if (step === 3 && (!selectedDate || !selectedTime)) return alert('Selecione a data e o horário');
    setStep(step + 1);
  };

  const handleConfirm = async () => {
    if (!isAuthenticated) {
      alert('Você precisa estar logado para agendar. Redirecionando...');
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
      alert('Agendamento realizado com sucesso!');
      navigate('/my-appointments');
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || 'Erro ao agendar.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Container className="py-5">
      <Row className="justify-content-center">
        <Col md={8}>
          <h2 className="text-center mb-4">Agende seu Horário</h2>
          
          <div className="d-flex justify-content-between mb-4">
            <Badge bg={step >= 1 ? 'primary' : 'secondary'} className="p-2 fs-6">1. Serviço</Badge>
            <Badge bg={step >= 2 ? 'primary' : 'secondary'} className="p-2 fs-6">2. Profissional</Badge>
            <Badge bg={step >= 3 ? 'primary' : 'secondary'} className="p-2 fs-6">3. Data e Hora</Badge>
            <Badge bg={step >= 4 ? 'primary' : 'secondary'} className="p-2 fs-6">4. Confirmar</Badge>
          </div>

          <Card className="shadow-sm">
            <Card.Body className="p-4">
              {errorMsg && <Alert variant="danger">{errorMsg}</Alert>}
              
              {step === 1 && (
                <div>
                  <h4 className="mb-4">Escolha o serviço</h4>
                  <div className="d-grid gap-3">
                    {services.map(srv => (
                      <Button 
                        key={srv.id} 
                        variant={selectedService === srv.id ? 'primary' : 'outline-primary'}
                        className="text-start p-3"
                        onClick={() => setSelectedService(srv.id!)}
                      >
                        <div className="d-flex justify-content-between align-items-center">
                          <strong>{srv.name}</strong>
                          <span>R$ {srv.price.toFixed(2)}</span>
                        </div>
                        <small className="d-block">{srv.durationMin} minutos</small>
                      </Button>
                    ))}
                  </div>
                </div>
              )}

              {step === 2 && (
                <div>
                  <h4 className="mb-4">Escolha o profissional</h4>
                  <div className="d-grid gap-3">
                    {employees.map(emp => (
                      <Button 
                        key={emp.id} 
                        variant={selectedEmployee === emp.id ? 'primary' : 'outline-primary'}
                        className="text-start p-3"
                        onClick={() => setSelectedEmployee(emp.id!)}
                      >
                        <strong>{emp.name}</strong>
                        <small className="d-block">{emp.bio || 'Profissional parceira'}</small>
                      </Button>
                    ))}
                  </div>
                </div>
              )}

              {step === 3 && (
                <div>
                  <h4 className="mb-4">Escolha data e horário</h4>
                  <Form.Group className="mb-4">
                    <Form.Label>Data</Form.Label>
                    <Form.Control 
                      type="date" 
                      min={new Date().toISOString().split('T')[0]}
                      value={selectedDate}
                      onChange={(e) => setSelectedDate(e.target.value)}
                    />
                  </Form.Group>
                  
                  {selectedDate && (
                    <div>
                      <Form.Label>Horários Disponíveis</Form.Label>
                      {isLoading ? (
                        <p>Carregando horários...</p>
                      ) : slots.length === 0 ? (
                        <p>Nenhum horário disponível para esta data.</p>
                      ) : (
                        <div className="d-flex flex-wrap gap-2">
                          {slots.filter(s => s.available).map((slot, i) => (
                            <Button
                              key={i}
                              variant={selectedTime === slot.time ? 'primary' : 'outline-primary'}
                              onClick={() => setSelectedTime(slot.time)}
                            >
                              {slot.time.substring(0, 5)}
                            </Button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              {step === 4 && (
                <div>
                  <h4 className="mb-4">Confirme seu agendamento</h4>
                  <div className="bg-light p-4 rounded mb-4">
                    <p><strong>Serviço:</strong> {services.find(s => s.id === selectedService)?.name}</p>
                    <p><strong>Profissional:</strong> {employees.find(e => e.id === selectedEmployee)?.name}</p>
                    <p><strong>Data:</strong> {selectedDate}</p>
                    <p><strong>Horário:</strong> {selectedTime}</p>
                  </div>
                  {!isAuthenticated && (
                    <Alert variant="warning">
                      Você será redirecionado para a tela de login/cadastro antes de confirmar.
                    </Alert>
                  )}
                </div>
              )}

              <div className="d-flex justify-content-between mt-5">
                <Button 
                  variant="secondary" 
                  onClick={() => setStep(step - 1)} 
                  disabled={step === 1 || isLoading}
                >
                  Voltar
                </Button>
                
                {step < 4 ? (
                  <Button variant="primary" onClick={handleNext}>
                    Próximo
                  </Button>
                ) : (
                  <Button variant="success" onClick={handleConfirm} disabled={isLoading}>
                    {isLoading ? 'Confirmando...' : 'Confirmar Agendamento'}
                  </Button>
                )}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

// Workaround for Badge not imported above
import { Badge } from 'react-bootstrap';
