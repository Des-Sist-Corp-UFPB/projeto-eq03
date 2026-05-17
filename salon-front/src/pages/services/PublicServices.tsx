import { useState, useEffect } from 'react';
import { Row, Col, Spinner } from 'react-bootstrap';
import { salonServicesApi, displayServiceDuration } from './services/services';
import type { SalonServiceData } from './services/services';
import './PublicServices.css';
import { useAlert } from '../../hooks/useAlert';

export const PublicServices = () => {
  const [services, setServices] = useState<SalonServiceData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { error: showError } = useAlert();

  useEffect(() => {
    const loadServices = async () => {
      try {
        const data = await salonServicesApi.findAll();
        setServices(data.filter(s => s.active));
      } catch (err) {
        showError('Erro ao carregar serviços');
      } finally {
        setIsLoading(false);
      }
    };
    loadServices();
  }, []);

  return (
    <div className="public-services-section">
      <div className="section-header fade-in-up">
        <h2 className="section-title">Nosso Menu de Serviços</h2>
        <p className="section-subtitle">Realce sua beleza com a nossa seleção exclusiva de tratamentos.</p>
      </div>

      {isLoading ? (
        <div className="text-center py-5">
          <Spinner animation="border" className="custom-spinner" />
          <p className="mt-3 text-muted">Carregando serviços...</p>
        </div>
      ) : (
        <Row className="g-4 fade-in-up" style={{ animationDelay: '0.2s' }}>
          {services.map((service) => (
            <Col md={6} lg={4} key={service.id}>
              <div className="service-card">
                <div className="service-header">
                  <h3 className="service-title">{service.name}</h3>
                  {service.price != null ? (
                    <span className="service-price">A partir de R$ {service.price.toFixed(2)}</span>
                  ) : null}
                </div>
                <div className="service-body">
                  <p className="service-desc">{service.description || 'Tratamento especial para os seus cuidados.'}</p>
                </div>
                <div className="service-footer">
                  <div className="duration-tag">
                    <span className="duration-icon">⏱</span>
                    <span>{displayServiceDuration(service)}</span>
                  </div>
                </div>
              </div>
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
};
