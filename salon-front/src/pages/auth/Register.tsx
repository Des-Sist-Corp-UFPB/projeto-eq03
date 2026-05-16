import { useState } from 'react';
import { Form, Button, Alert } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../../hooks/useAuth';
import './Auth.css';

export const Register = () => {
  const { register, handleSubmit, formState: { errors } } = useForm();
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data: any) => {
    setIsLoading(true);
    setErrorMsg('');
    try {
      const response = await axios.post('http://localhost:8080/v1/auth/register', data);
      login(response.data.accessToken, response.data.refreshToken);
      navigate('/');
    } catch (err: any) {
      if (err.response?.data?.message) {
        setErrorMsg(err.response.data.message);
      } else {
        setErrorMsg('Erro ao realizar cadastro. Tente novam�n.e.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-form-container" style={{ maxWidth: '550px' }}>
          <h2>Cadastre-se</h2>
          <p className="subtitle">Crie sua conta para agendar seus servi�os</p>

          {errorMsg && <Alert variant="danger">{errorMsg}</Alert>}
          
          <Form onSubmit={handleSubmit(onSubmit)}>
            <Form.Group className="mb-3" controlId="name">
              <Form.Label>Nome Completo</Form.Label>
              <Form.Control 
                type="text" 
                placeholder="Seu nome completo"
                {...register('name', { required: 'Nome � obrigat�rio', minLength: { value: 3, message: 'M�nimo 3 caracteres'} })}
                isInvalid={!!errors.name}
              />
              <Form.Control.Feedback type="invalid">
                {errors.name?.message as string}
              </Form.Control.Feedback>
            </Form.Group>

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

            <Form.Group className="mb-3" controlId="phone">
              <Form.Label>Telefone</Form.Label>
              <Form.Control 
                type="text" 
                placeholder="Seu telefone (opcional)"
                {...register('phone')}
              />
            </Form.Group>

            <Form.Group className="mb-4" controlId="password">
              <Form.Label>Senha</Form.Label>
              <Form.Control 
                type="password" 
                placeholder="Sua senha (m�n. 6 caracteres)"
                {...register('password', { required: 'Senha � obrigat�ria', minLength: { value: 6, message: 'M�nimo 6 caracteres'} })}
                isInvalid={!!errors.password}
              />
              <Form.Control.Feedback type="invalid">
                {errors.password?.message as string}
              </Form.Control.Feedback>
            </Form.Group>

            <Button variant="primary" type="submit" className="w-100 mb-4" disabled={isLoading}>
              {isLoading ? 'Cadastrando...' : 'Criar minha conta'}
            </Button>
            
            <div className="text-center">
              <span className="text-muted">J� tem uma conta? </span>
              <Link to="/login" className="text-decoration-none fw-semibold">Entre aqui</Link>
            </div>
          </Form>
        </div>
        <div className="auth-image"></div>
      </div>
    </div>
  );
};
