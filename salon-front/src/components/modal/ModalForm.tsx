import { Modal, Button, Form } from 'react-bootstrap';
import { ReactNode } from 'react';

interface ModalFormProps {
  show: boolean;
  onHide: () => void;
  title: string;
  children: ReactNode;
  onSubmit: (e: React.FormEvent) => void;
  isSubmitting?: boolean;
  submitLabel?: string;
  cancelLabel?: string;
}

export const ModalForm = ({
  show,
  onHide,
  title,
  children,
  onSubmit,
  isSubmitting = false,
  submitLabel = 'Salvar',
  cancelLabel = 'Cancelar'
}: ModalFormProps) => {
  return (
    <Modal show={show} onHide={onHide} centered backdrop="static">
      <Form onSubmit={onSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>{title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {children}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide} disabled={isSubmitting}>
            {cancelLabel}
          </Button>
          <Button variant="primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : submitLabel}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
};
