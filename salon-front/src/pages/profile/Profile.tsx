import { useState, useEffect } from 'react';
import { Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { profileApi } from './services/profile';
import { useAuth } from '../../hooks/useAuth';
import type { UserUpdateRequest } from '../admin/users/services/users';
import { Save, User as UserIcon } from 'lucide-react';

export const Profile = () => {
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  
  const { register, handleSubmit, setValue } = useForm<UserUpdateRequest>();

  useEffect(() => {
    const loadProfile = async () => {
      if (!user?.userId) return;
      
      try {
        const data = await profileApi.getProfileById(user.userId);
        setValue('name', data.name);
        setValue('email', data.email);
        setValue('phone', data.phone || '');
      } catch (error) {
        console.error('Erro ao carregar perfil', error);
        alert('Erro ao carregar os dados do perfil.');
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [user, setValue]);

  const onSubmit = async (data: UserUpdateRequest) => {
    if (!user?.userId) return;
    
    setIsSaving(true);
    try {
      // Only send password if it was filled
      const updateData = { ...data };
      if (!updateData.password) {
        delete updateData.password;
      }
      
      await profileApi.updateProfile(user.userId, updateData);
      alert('Perfil atualizado com sucesso!');
    } catch (error) {
      console.error('Erro ao atualizar perfil', error);
      alert('Erro ao atualizar perfil.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3">Carregando perfil...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h2 className="mb-4">Meu Perfil</h2>
      
      <Card className="shadow-sm border-0">
        <Card.Body className="p-4">
          <div className="d-flex align-items-center mb-4 pb-3 border-bottom">
            <div className="bg-primary bg-opacity-10 rounded-circle p-3 me-3 text-primary">
              <UserIcon size={32} />
            </div>
            <div>
              <h4 className="mb-0">{user?.email}</h4>
              <p className="text-muted mb-0">Atualize suas informações pessoais</p>
            </div>
          </div>

          <Form onSubmit={handleSubmit(onSubmit)}>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Nome Completo</Form.Label>
                  <Form.Control type="text" {...register('name', { required: true })} />
                </Form.Group>
              </Col>
              
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Telefone</Form.Label>
                  <Form.Control type="tel" {...register('phone')} placeholder="(11) 99999-9999" />
                </Form.Group>
              </Col>
              
              <Col md={12}>
                <Form.Group className="mb-3">
                  <Form.Label>Email</Form.Label>
                  <Form.Control type="email" {...register('email', { required: true })} disabled />
                  <Form.Text className="text-muted">
                    O email não pode ser alterado, pois é usado para login.
                  </Form.Text>
                </Form.Group>
              </Col>
              
              <Col md={6}>
                <Form.Group className="mb-4">
                  <Form.Label>Nova Senha</Form.Label>
                  <Form.Control type="password" {...register('password')} placeholder="Deixe em branco para não alterar" />
                </Form.Group>
              </Col>
            </Row>

            <div className="d-flex justify-content-end">
              <Button variant="primary" type="submit" disabled={isSaving}>
                {isSaving ? <Spinner size="sm" className="me-2" /> : <Save size={18} className="me-2" />}
                Salvar Alterações
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
};
