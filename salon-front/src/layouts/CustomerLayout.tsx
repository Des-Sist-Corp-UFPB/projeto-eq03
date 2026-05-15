import { Outlet, Navigate, Link } from 'react-router-dom';
import { Container, Navbar, Nav } from 'react-bootstrap';
import { useAuth } from '../hooks/useAuth';
import './Layouts.css';

export const CustomerLayout = () => {
  const { isAuthenticated, isLoading, logout } = useAuth();

  if (isLoading) return <div>Carregando...</div>;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <>
      <Navbar expand="lg" className="navbar">
        <Container>
          <Navbar.Brand as={Link} to="/">Área do Cliente</Navbar.Brand>
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link as={Link} to="/my-appointments">Meus Agendamentos</Nav.Link>
              <Nav.Link as={Link} to="/profile">Perfil</Nav.Link>
            </Nav>
            <Nav>
              <Nav.Link onClick={logout}>Sair</Nav.Link>
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
      <main className="py-4">
        <Container>
          <Outlet />
        </Container>
      </main>
    </>
  );
};
