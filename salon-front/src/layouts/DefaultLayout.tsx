import { Outlet, NavLink, Link } from 'react-router-dom';
import { Container, Navbar, Nav } from 'react-bootstrap';
import { useAuth } from '../hooks/useAuth';
import './Layouts.css';

export const DefaultLayout = () => {
  const { isAuthenticated, logout } = useAuth();

  return (
    <div className="layout-wrapper">
      <Navbar expand="lg" className="custom-navbar" fixed="top">
        <Container>
          <Navbar.Brand as={Link} to="/" className="brand-logo">
            <span className="brand-icon">✨</span> Salão Cristiane
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="mx-auto nav-links">
              <Nav.Link as={NavLink} to="/services" className={({ isActive }: any) => isActive ? "active-link" : ""}>Nossos Serviços</Nav.Link>
              <Nav.Link as={NavLink} to="/appointment" className={({ isActive }: any) => isActive ? "active-link" : ""}>Agendamento</Nav.Link>
            </Nav>
            <Nav className="nav-actions">
              {isAuthenticated ? (
                <>
                  <Nav.Link as={Link} to="/my-appointments" className="text-secondary fw-medium me-2">Meus Horários</Nav.Link>
                  <Nav.Link onClick={logout} className="logout-btn">Sair</Nav.Link>
                </>
              ) : (
                <>
                  <Nav.Link as={Link} to="/login" className="text-secondary fw-medium me-3">Entrar</Nav.Link>
                  <Nav.Link as={Link} to="/register" className="auth-btn">Criar Conta</Nav.Link>
                </>
              )}
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
};
