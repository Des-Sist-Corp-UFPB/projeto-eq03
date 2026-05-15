import { Outlet, Navigate, Link } from 'react-router-dom';
import { Container, Row, Col, Nav } from 'react-bootstrap';
import { useAuth } from '../hooks/useAuth';
import './AdminLayout.css';

export const AdminLayout = () => {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  if (isLoading) return <div>Carregando...</div>;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== 'ADMIN' && user?.role !== 'GERENTE_DE_ATENDIMENTO') {
    return <Navigate to="/" replace />;
  }

  return (
    <Container fluid className="px-0">
      <Row className="g-0">
        <Col md={2} className="admin-sidebar min-vh-100 p-3">
          <h4 className="text-white mb-4">Admin Salão</h4>
          <Nav className="flex-column">
            <Nav.Link as={Link} to="/admin/dashboard" className="text-white">Dashboard</Nav.Link>
            <Nav.Link as={Link} to="/admin/users" className="text-white">Clientes</Nav.Link>
            <Nav.Link as={Link} to="/admin/employees" className="text-white">Funcionárias</Nav.Link>
            <Nav.Link as={Link} to="/admin/services" className="text-white">Serviços</Nav.Link>
            <Nav.Link as={Link} to="/admin/products" className="text-white">Produtos</Nav.Link>
            <Nav.Link as={Link} to="/admin/appointments" className="text-white">Agendamentos</Nav.Link>
            <Nav.Link as={Link} to="/admin/cashflow" className="text-white">Fluxo de Caixa</Nav.Link>
            <Nav.Link as={Link} to="/admin/reports" className="text-white">Relatórios</Nav.Link>
            <Nav.Link onClick={logout} className="text-white mt-5 text-danger">Sair</Nav.Link>
          </Nav>
        </Col>
        <Col md={10} className="p-4">
          <Outlet />
        </Col>
      </Row>
    </Container>
  );
};
