import { Modal, Button } from 'react-bootstrap';

interface ConfirmDialogProps {
  show: boolean;
  onHide: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'primary' | 'danger' | 'warning';
  isProcessing?: boolean;
}

export const ConfirmDialog = ({
  show,
  onHide,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  variant = 'danger',
  isProcessing = false
}: ConfirmDialogProps) => {
  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>{message}</p>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide} disabled={isProcessing}>
          {cancelLabel}
        </Button>
        <Button variant={variant} onClick={onConfirm} disabled={isProcessing}>
          {isProcessing ? 'Processando...' : confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
