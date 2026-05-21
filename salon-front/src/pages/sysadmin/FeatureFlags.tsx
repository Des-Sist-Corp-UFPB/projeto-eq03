import { useState, useEffect } from 'react';
import { Card, Row, Col, Form, Spinner } from 'react-bootstrap';
import { featureFlagsService, type FeatureFlag } from '../../services/featureFlags';
import { getApiErrorMessage } from '../../utils/apiError';
import { useAlert } from '../../hooks/useAlert';
import { ShieldAlert } from 'lucide-react';

export const FeatureFlags = () => {
  const [flags, setFlags] = useState<FeatureFlag[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [togglingName, setTogglingName] = useState<string | null>(null);

  const { error: showError, success: showSuccess } = useAlert();

  const loadFlags = async () => {
    setIsLoading(true);
    try {
      const data = await featureFlagsService.getAllFlags();
      setFlags(data);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar as feature flags.');
      showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFlags();
  }, []);

  const handleToggle = async (name: string, currentStatus: boolean) => {
    setTogglingName(name);
    try {
      await featureFlagsService.toggleFlag(name);
      // Update local state
      setFlags(prev =>
        prev.map(flag =>
          flag.name === name ? { ...flag, enabled: !currentStatus } : flag
        )
      );
      showSuccess(`A feature flag ${name} foi ${!currentStatus ? 'ativada' : 'desativada'} com sucesso.`);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao alternar o estado da feature flag.');
      showError(msg);
    } finally {
      setTogglingName(null);
    }
  };

  return (
    <div className="container-fluid py-4">
      <div className="d-flex align-items-center mb-4 pb-2 border-bottom">
        <ShieldAlert size={32} className="text-primary me-2" />
        <h2 className="mb-0">Gerenciar Feature Flags</h2>
      </div>

      <p className="text-muted mb-4">
        Controle as funcionalidades do sistema em tempo real. Habilitar ou desabilitar flags afeta instantaneamente a experiência do usuário e os fluxos do backend.
      </p>

      {isLoading ? (
        <div className="d-flex justify-content-center my-5">
          <Spinner animation="border" variant="primary" />
        </div>
      ) : (
        <Row className="g-4">
          {flags.map(flag => (
            <Col key={flag.name} xs={12} md={6} xl={4}>
              <Card className="h-100 shadow-sm border-0" style={{ borderRadius: '12px' }}>
                <Card.Body className="d-flex flex-column justify-content-between p-4">
                  <div>
                    <div className="d-flex justify-content-between align-items-start mb-3">
                      <span className="fw-bold text-dark font-monospace" style={{ fontSize: '1.1rem' }}>
                        {flag.name}
                      </span>
                      <span
                        className={`badge px-3 py-2 rounded-pill ${
                          flag.enabled ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'
                        }`}
                        style={{ fontSize: '0.8rem' }}
                      >
                        {flag.enabled ? 'Ativo' : 'Inativo'}
                      </span>
                    </div>
                    <Card.Text className="text-muted small mb-4" style={{ minHeight: '40px' }}>
                      {flag.description || 'Nenhuma descrição fornecida.'}
                    </Card.Text>
                  </div>

                  <div className="d-flex justify-content-between align-items-center pt-3 border-top">
                    <span className="text-muted small">Alternar Status</span>
                    {togglingName === flag.name ? (
                      <Spinner animation="border" size="sm" variant="primary" />
                    ) : (
                      <Form.Check
                        type="switch"
                        id={`flag-switch-${flag.name}`}
                        checked={flag.enabled}
                        onChange={() => handleToggle(flag.name, flag.enabled)}
                        style={{ cursor: 'pointer', scale: '1.2' }}
                      />
                    )}
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}

          {flags.length === 0 && (
            <div className="text-center my-5">
              <p className="text-muted">Nenhuma feature flag cadastrada no banco de dados.</p>
            </div>
          )}
        </Row>
      )}
    </div>
  );
};
