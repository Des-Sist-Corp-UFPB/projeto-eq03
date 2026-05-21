import { Outlet, Navigate, Link } from 'react-router-dom';
import { Container, Row, Col, Nav } from 'react-bootstrap';
import { useAuth } from '../hooks/useAuth';
import './SysadminLayout.css';

export const SysadminLayout = () => {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  if (isLoading) return <div className="d-flex justify-content-center align-items-center vh-100">Carregando...</div>;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== 'SYSADMIN') {
    return <Navigate to="/" replace />;
  }

  return (
    <Container fluid className="px-0">
      <Row className="g-0 min-vh-100">
        <Col md={2} className="sysadmin-sidebar min-vh-100 p-3">
          <h4 className="text-white mb-4">SysAdmin Painel</h4>
          <Nav className="flex-column">
            <Nav.Link as={Link} to="/sysadmin/feature-flags" className="text-white">Feature Flags</Nav.Link>
            <Nav.Link as={Link} to="/sysadmin/audit" className="text-white">Auditoria</Nav.Link>
            <Nav.Link onClick={logout} className="text-white mt-5 text-danger fw-bold">Sair</Nav.Link>
          </Nav>
        </Col>
        <Col md={10} className="p-4 bg-light">
          <Outlet />
        </Col>
      </Row>
    </Container>
  );
};
