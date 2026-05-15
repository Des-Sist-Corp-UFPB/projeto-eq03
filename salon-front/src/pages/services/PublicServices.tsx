import { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Badge, Spinner } from 'react-bootstrap';
import { servicesApi, ServiceData } from './services/services';

export const PublicServices = () => {
  const [services, setServices] = useState<ServiceData[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadServices = async () => {
      try {
        const data = await servicesApi.findAll();
        setServices(data.filter(s => s.active));
      } catch (error) {
        console.error('Erro ao carregar serviços', error);
      } finally {
        setIsLoading(false);
      }
    };
    loadServices();
  }, []);

  return (
    <Container className="py-5">
      <h2 className="text-center mb-5">Nossos Serviços</h2>
      {isLoading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
        </div>
      ) : (
        <Row className="g-4">
          {services.map((service) => (
            <Col md={6} lg={4} key={service.id}>
              <Card className="h-100 shadow-sm border-0">
                <Card.Body className="d-flex flex-column">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <Card.Title className="mb-0 fw-bold">{service.name}</Card.Title>
                    <Badge bg="primary" className="fs-6">R$ {service.price.toFixed(2)}</Badge>
                  </div>
                  <Card.Text className="text-muted flex-grow-1">
                    {service.description || 'Sem descrição'}
                  </Card.Text>
                  <div className="mt-auto d-flex align-items-center text-secondary small">
                    <i className="bi bi-clock me-2"></i>
                    Duração aprox: {service.durationMin} min
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Container>
  );
};
