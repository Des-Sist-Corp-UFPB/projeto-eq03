import { useState } from 'react';
import { Form, Button, Alert } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import './Auth.css';
import { getApiErrorMessage } from '../../utils/apiError';

interface LoginFormData {
  email: string;
  password: string;
}

export const Login = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormData>();
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    setErrorMsg('');
    try {
      const response = await api.post('/auth/login', data);
      // Passa true para redirect automaticamente (admin vai pro dashboard, outros continuam)
      login(response.data.accessToken, response.data.refreshToken, true);
      
      // Se chegou aqui sem redirecionar, é cliente
      const pending = localStorage.getItem('pending_appointment');
      navigate(pending ? '/appointment' : '/');
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao realizar login. Tente novamente.');
      setErrorMsg(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-image"></div>
        <div className="auth-form-container">
          <h2>Bem-vinda de volta</h2>
          <p className="subtitle">Faça login para gerenciar seus agendamentos</p>

          {errorMsg && <Alert variant="danger">{errorMsg}</Alert>}
          
          <Form onSubmit={handleSubmit(onSubmit)}>
            <Form.Group className="mb-3" controlId="email">
              <Form.Label>Email</Form.Label>
              <Form.Control 
                type="email" 
                placeholder="Seu email"
                {...register('email', { required: 'Email é obrigatório' })}
                isInvalid={!!errors.email}
              />
              <Form.Control.Feedback type="invalid">
                {errors.email?.message as string}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-4" controlId="password">
              <Form.Label>Senha</Form.Label>
              <Form.Control 
                type="password" 
                placeholder="Sua senha"
                {...register('password', { required: 'Senha é obrigatória' })}
                isInvalid={!!errors.password}
              />
              <Form.Control.Feedback type="invalid">
                {errors.password?.message as string}
              </Form.Control.Feedback>
            </Form.Group>

            <Button variant="primary" type="submit" className="w-100 mb-4" disabled={isLoading}>
              {isLoading ? 'Acessando...' : 'Entrar na minha conta'}
            </Button>
            
            <div className="text-center">
              <span className="text-muted">Não tem uma conta? </span>
              <Link to="/register" className="text-decoration-none fw-semibold">Cadastre-se</Link>
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
};
