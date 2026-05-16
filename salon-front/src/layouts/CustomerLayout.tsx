import { Outlet, Navigate, NavLink, Link } from 'react-router-dom';
import { Container, Navbar, Nav } from 'react-bootstrap';
import { useAuth } from '../hooks/useAuth';
import './Layouts.css';

export const CustomerLayout = () => {
  const { isAuthenticated, isLoading, logout } = useAuth();

  if (isLoading) return <div className="loading-screen">Carregando...</div>;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="layout-wrapper">
      <Navbar expand="lg" className="custom-navbar" fixed="top">
        <Container>
          <Navbar.Brand as={Link} to="/" className="brand-logo">
            ✨ Salão Cristiane
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="mx-auto nav-links">
              <Nav.Link as={NavLink} to="/my-appointments">Meus Agendamentos</Nav.Link>
              <Nav.Link as={NavLink} to="/profile">Meu Perfil</Nav.Link>
            </Nav>
            <Nav.Link onClick={logout} className="logout-btn">Sair da Conta</Nav.Link>
          </Navbar.Collapse>
        </Container>
      </Navbar>
      <main className="main-content">
        <Container className="fade-in-up">
          <Outlet />
        </Container>
      </main>
    </div>
  );
};
