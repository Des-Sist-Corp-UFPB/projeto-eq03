import { useState, useEffect } from 'react';
import { Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { reportsApi } from './services/reports';
import type { FinancialReportResponse, AppointmentReportResponse } from './services/reports';
import { cashFlowApi } from '../cashflow/services/cashflow';
import type { CashFlowData } from '../cashflow/services/cashflow';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, Legend } from 'recharts';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { Download } from 'lucide-react';
import { useAlert } from '../../../hooks/useAlert';

export const Reports = () => {
  const [financial, setFinancial] = useState<FinancialReportResponse | null>(null);
  const [appointments, setAppointments] = useState<AppointmentReportResponse | null>(null);
  const [cashFlows, setCashFlows] = useState<CashFlowData[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const { error: showError } = useAlert();

  const loadReports = async () => {
    setIsLoading(true);
    try {
      const [finData, aptData, cfData] = await Promise.all([
        reportsApi.getFinancialReport(dateFrom || undefined, dateTo || undefined),
        reportsApi.getAppointmentReport(dateFrom || undefined, dateTo || undefined),
        cashFlowApi.findByPeriod(dateFrom || undefined, dateTo || undefined)
      ]);
      setFinancial(finData);
      setAppointments(aptData);
      setCashFlows(cfData);
    } catch (err) {
      await showError('Erro ao carregar relatórios');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
  }, [dateFrom, dateTo]);

  const generatePDF = () => {
    const doc = new jsPDF();
    doc.setFontSize(18);
    doc.text('Relatório Financeiro e Agendamentos', 14, 22);
    doc.setFontSize(11);
    doc.text(`Período: ${financial?.period || 'N/A'}`, 14, 30);

    // Financial Summary
    doc.setFontSize(14);
    doc.text('Resumo Financeiro', 14, 45);
    autoTable(doc, {
      startY: 50,
      head: [['Receitas', 'Despesas', 'Lucro Líquido']],
      body: [[
        `R$ ${financial?.totalIncome.toFixed(2) || '0.00'}`,
        `R$ ${financial?.totalExpense.toFixed(2) || '0.00'}`,
        `R$ ${financial?.netProfit.toFixed(2) || '0.00'}`
      ]],
    });

    // Appointments Summary
    const finalY = (doc as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY || 50;
    doc.text('Resumo de Agendamentos', 14, finalY + 15);
    autoTable(doc, {
      startY: finalY + 20,
      head: [['Total', 'Concluídos', 'Pendentes', 'Cancelados']],
      body: [[
        appointments?.totalAppointments || 0,
        appointments?.done || 0,
        appointments?.pending || 0,
        appointments?.cancelled || 0
      ]],
    });

    doc.save('relatorio-salao.pdf');
  };

  // Prepare chart data
  const chartDataEmployee = Object.entries(appointments?.byEmployee || {}).map(([name, count]) => ({
    name, Agendamentos: count
  }));

  const chartDataService = Object.entries(appointments?.byService || {}).map(([name, count]) => ({
    name, Agendamentos: count
  }));

  // Prepare revenue chart (group by date)
  const revenueByDate: Record<string, number> = {};
  cashFlows.forEach(cf => {
    if (cf.type === 'INCOME') {
      const d = cf.date.substring(0, 10);
      revenueByDate[d] = (revenueByDate[d] || 0) + cf.amount;
    }
  });
  
  const chartDataRevenue = Object.entries(revenueByDate)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, amount]) => ({
      Data: new Date(date).toLocaleDateString('pt-BR', {day: '2-digit', month: '2-digit'}),
      Receita: amount
    }));

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Dashboard & Relatórios</h2>
        <Button variant="danger" onClick={generatePDF} disabled={isLoading}>
          <Download size={18} className="me-2" />
          Exportar PDF
        </Button>
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
              <Card className="shadow-sm border-0 border-start border-success border-5 h-100">
                <Card.Body>
                  <Card.Subtitle className="text-muted mb-2">Total Receitas</Card.Subtitle>
                  <Card.Title className="fs-3 text-success">R$ {financial?.totalIncome.toFixed(2)}</Card.Title>
                </Card.Body>
              </Card>
            </Col>
            <Col md={4}>
              <Card className="shadow-sm border-0 border-start border-danger border-5 h-100">
                <Card.Body>
                  <Card.Subtitle className="text-muted mb-2">Total Despesas</Card.Subtitle>
                  <Card.Title className="fs-3 text-danger">R$ {financial?.totalExpense.toFixed(2)}</Card.Title>
                </Card.Body>
              </Card>
            </Col>
            <Col md={4}>
              <Card className={`shadow-sm border-0 border-start border-5 h-100 ${financial?.netProfit! >= 0 ? 'border-primary' : 'border-danger'}`}>
                <Card.Body>
                  <Card.Subtitle className="text-muted mb-2">Lucro Líquido</Card.Subtitle>
                  <Card.Title className={`fs-3 ${financial?.netProfit! >= 0 ? 'text-primary' : 'text-danger'}`}>
                    R$ {financial?.netProfit.toFixed(2)}
                  </Card.Title>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          {/* Line Chart para Receita */}
          <h4 className="mb-3">Evolução de Receitas</h4>
          <Card className="shadow-sm mb-5">
            <Card.Body>
              <div style={{ height: 300, width: '100%' }}>
                {chartDataRevenue.length > 0 ? (
                  <ResponsiveContainer>
                    <LineChart data={chartDataRevenue}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="Data" />
                      <YAxis />
                      <Tooltip formatter={(val: any) => `R$ ${Number(val).toFixed(2)}`} />
                      <Legend />
                      <Line type="monotone" dataKey="Receita" stroke="#198754" activeDot={{ r: 8 }} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="d-flex h-100 align-items-center justify-content-center text-muted">
                    Nenhuma receita no período selecionado.
                  </div>
                )}
              </div>
            </Card.Body>
          </Card>

          <h4 className="mb-3">Desempenho de Agendamentos ({appointments?.period})</h4>
          <Row className="g-4 mb-5">
            <Col md={3}>
              <Card className="text-center shadow-sm h-100">
                <Card.Body>
                  <h1 className="text-primary">{appointments?.totalAppointments}</h1>
                  <p className="text-muted mb-0">Total</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center shadow-sm h-100">
                <Card.Body>
                  <h1 className="text-success">{appointments?.done}</h1>
                  <p className="text-muted mb-0">Concluídos</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center shadow-sm h-100">
                <Card.Body>
                  <h1 className="text-warning">{appointments?.pending}</h1>
                  <p className="text-muted mb-0">Pendentes</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center shadow-sm h-100">
                <Card.Body>
                  <h1 className="text-danger">{appointments?.cancelled}</h1>
                  <p className="text-muted mb-0">Cancelados</p>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          <Row className="g-4 mb-4">
            <Col md={6}>
              <Card className="shadow-sm h-100">
                <Card.Header className="bg-white"><strong>Por Profissional</strong></Card.Header>
                <Card.Body>
                  <div style={{ height: 300, width: '100%' }}>
                    {chartDataEmployee.length > 0 ? (
                      <ResponsiveContainer>
                        <BarChart data={chartDataEmployee}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="name" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          <Bar dataKey="Agendamentos" fill="#0d6efd" />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <p className="text-muted">Nenhum dado</p>
                    )}
                  </div>
                </Card.Body>
              </Card>
            </Col>
            <Col md={6}>
              <Card className="shadow-sm h-100">
                <Card.Header className="bg-white"><strong>Por Serviço</strong></Card.Header>
                <Card.Body>
                  <div style={{ height: 300, width: '100%' }}>
                    {chartDataService.length > 0 ? (
                      <ResponsiveContainer>
                        <BarChart data={chartDataService} layout="vertical">
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis type="number" />
                          <YAxis dataKey="name" type="category" width={100} />
                          <Tooltip />
                          <Legend />
                          <Bar dataKey="Agendamentos" fill="#6f42c1" />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <p className="text-muted">Nenhum dado</p>
                    )}
                  </div>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
};
