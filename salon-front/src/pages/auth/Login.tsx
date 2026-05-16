import { useState } from 'react';
import { Form, Button, Alert } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../../hooks/useAuth';
import './Auth.css';

export const Login = () => {
  const { register, handleSubmit, formState: { errors } } = useForm();
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data: any) => {
    setIsLoading(true);
    setErrorMsg('');
    try {
      const response = await axios.post('http://localhost:8080/v1/auth/login', data);
      login(response.data.accessToken, response.data.refreshToken);
      navigate('/'); // Or redirect based on role
    } catch (err: any) {
      if (err.response?.data?.message) {
        setErrorMsg(err.response.data.message);
      } else {
        setErrorMsg('Erro ao realizar login. Tente novamente.');
      }
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
          <p className="subtitle">Fa�a login para gerenciar seus agendamentos</p>

          {errorMsg && <Alert variant="danger">{errorMsg}</Alert>}
          
          <Form onSubmit={handleSubmit(onSubmit)}>
            <Form.Group className="mb-3" controlId="email">
              <Form.Label>Email</Form.Label>
              <Form.Control 
                type="email" 
                placeholder="Seu email"
                {...register('email', { required: 'Email � obrigat�rio' })}
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
                {...register('password', { required: 'Senha � obrigat�ria' })}
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
              <span className="text-muted">N�o tem uma conta? </span>
              <Link to="/register" className="text-decoration-none fw-semibold">Cadastre-se</Link>
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
};
