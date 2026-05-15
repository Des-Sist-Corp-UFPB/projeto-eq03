import { Outlet, Link } from 'react-router-dom';
import { Container, Navbar, Nav } from 'react-bootstrap';
import { useAuth } from '../hooks/useAuth';
import './Layouts.css';

export const DefaultLayout = () => {
  const { isAuthenticated, logout } = useAuth();

  return (
    <>
      <Navbar expand="lg" className="navbar">
        <Container>
          <Navbar.Brand as={Link} to="/">Cristiane Moura</Navbar.Brand>
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link as={Link} to="/services">Serviços</Nav.Link>
              <Nav.Link as={Link} to="/appointment">Agendamento</Nav.Link>
            </Nav>
            <Nav>
              {isAuthenticated ? (
                <>
                  <Nav.Link as={Link} to="/profile">Meu Perfil</Nav.Link>
                  <Nav.Link as={Link} to="/admin/dashboard">Admin</Nav.Link>
                  <Nav.Link onClick={logout}>Sair</Nav.Link>
                </>
              ) : (
                <>
                  <Nav.Link as={Link} to="/login">Entrar</Nav.Link>
                  <Nav.Link as={Link} to="/register">Cadastre-se</Nav.Link>
                </>
              )}
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
      <main>
        <Outlet />
      </main>
    </>
  );
};
