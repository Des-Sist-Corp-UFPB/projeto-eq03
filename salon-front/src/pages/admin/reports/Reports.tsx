import { useState, useEffect } from 'react';
import { Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { reportsApi, FinancialReportResponse, AppointmentReportResponse } from './services/reports';

export const Reports = () => {
  const [financial, setFinancial] = useState<FinancialReportResponse | null>(null);
  const [appointments, setAppointments] = useState<AppointmentReportResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const loadReports = async () => {
    setIsLoading(true);
    try {
      const [finData, aptData] = await Promise.all([
        reportsApi.getFinancialReport(dateFrom || undefined, dateTo || undefined),
        reportsApi.getAppointmentReport(dateFrom || undefined, dateTo || undefined)
      ]);
      setFinancial(finData);
      setAppointments(aptData);
    } catch (error) {
      console.error('Erro ao carregar relatórios', error);
      alert('Erro ao carregar relatórios');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
  }, [dateFrom, dateTo]);

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Dashboard & Relatórios</h2>
      </div>

      <Row className="mb-4">
        <Col md={3}>
          <Form.Group>
            <Form.Label>De</Form.Label>
            <Form.Control type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group>
            <Form.Label>Até</Form.Label>
            <Form.Control type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6} className="d-flex align-items-end">
          <Button variant="outline-secondary" onClick={() => { setDateFrom(''); setDateTo(''); }}>
            Mês Atual (Padrão)
          </Button>
        </Col>
      </Row>

      {isLoading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
          <p className="mt-3">Gerando relatórios...</p>
        </div>
      ) : (
        <>
          <h4 className="mb-3">Resumo Financeiro ({financial?.period})</h4>
          <Row className="g-4 mb-5">
            <Col md={4}>
              <Card className="shadow-sm border-0 border-start border-success border-5">
                <Card.Body>
                  <Card.Subtitle className="text-muted mb-2">Total Receitas</Card.Subtitle>
                  <Card.Title className="fs-3 text-success">R$ {financial?.totalIncome.toFixed(2)}</Card.Title>
                </Card.Body>
              </Card>
            </Col>
            <Col md={4}>
              <Card className="shadow-sm border-0 border-start border-danger border-5">
                <Card.Body>
                  <Card.Subtitle className="text-muted mb-2">Total Despesas</Card.Subtitle>
                  <Card.Title className="fs-3 text-danger">R$ {financial?.totalExpense.toFixed(2)}</Card.Title>
                </Card.Body>
              </Card>
            </Col>
            <Col md={4}>
              <Card className={`shadow-sm border-0 border-start border-5 ${financial?.netProfit! >= 0 ? 'border-primary' : 'border-danger'}`}>
                <Card.Body>
                  <Card.Subtitle className="text-muted mb-2">Lucro Líquido</Card.Subtitle>
                  <Card.Title className={`fs-3 ${financial?.netProfit! >= 0 ? 'text-primary' : 'text-danger'}`}>
                    R$ {financial?.netProfit.toFixed(2)}
                  </Card.Title>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          <h4 className="mb-3">Desempenho de Agendamentos ({appointments?.period})</h4>
          <Row className="g-4 mb-4">
            <Col md={3}>
              <Card className="text-center shadow-sm">
                <Card.Body>
                  <h1 className="text-primary">{appointments?.totalAppointments}</h1>
                  <p className="text-muted mb-0">Total</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center shadow-sm">
                <Card.Body>
                  <h1 className="text-success">{appointments?.done}</h1>
                  <p className="text-muted mb-0">Concluídos</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center shadow-sm">
                <Card.Body>
                  <h1 className="text-warning">{appointments?.pending}</h1>
                  <p className="text-muted mb-0">Pendentes</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center shadow-sm">
                <Card.Body>
                  <h1 className="text-danger">{appointments?.cancelled}</h1>
                  <p className="text-muted mb-0">Cancelados</p>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          <Row className="g-4">
            <Col md={6}>
              <Card className="shadow-sm">
                <Card.Header className="bg-white"><strong>Por Profissional</strong></Card.Header>
                <Card.Body>
                  {appointments?.byEmployee && Object.entries(appointments.byEmployee).map(([name, count]) => (
                    <div key={name} className="d-flex justify-content-between mb-2 pb-2 border-bottom">
                      <span>{name}</span>
                      <strong>{count}</strong>
                    </div>
                  ))}
                  {Object.keys(appointments?.byEmployee || {}).length === 0 && <p className="text-muted">Nenhum dado</p>}
                </Card.Body>
              </Card>
            </Col>
            <Col md={6}>
              <Card className="shadow-sm">
                <Card.Header className="bg-white"><strong>Por Serviço</strong></Card.Header>
                <Card.Body>
                  {appointments?.byService && Object.entries(appointments.byService).map(([name, count]) => (
                    <div key={name} className="d-flex justify-content-between mb-2 pb-2 border-bottom">
                      <span>{name}</span>
                      <strong>{count}</strong>
                    </div>
                  ))}
                  {Object.keys(appointments?.byService || {}).length === 0 && <p className="text-muted">Nenhum dado</p>}
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
};
