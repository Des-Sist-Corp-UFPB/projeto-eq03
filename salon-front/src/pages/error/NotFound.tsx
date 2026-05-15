import { Button, Container } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';

export const NotFound = () => {
  const navigate = useNavigate();

  return (
    <Container className="d-flex flex-column align-items-center justify-content-center" style={{ minHeight: '100vh' }}>
      <h1 className="display-1 fw-bold text-primary">404</h1>
      <h2 className="mb-4">Página não encontrada</h2>
      <p className="text-muted text-center mb-4">
        A página que você está procurando pode ter sido removida, <br />
        renomeada ou está temporariamente indisponível.
      </p>
      <Button variant="primary" onClick={() => navigate('/')}>
        Voltar para o Início
      </Button>
    </Container>
  );
};
