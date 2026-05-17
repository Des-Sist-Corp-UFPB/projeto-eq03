import { useContext } from 'react';
import { AlertContext } from '../context/AlertContextBase';
import type { AlertType } from '../context/AlertContextBase';

interface UseAlertOptions {
  title?: string;
  type?: AlertType;
  confirmText?: string;
  cancelText?: string;
  isDangerous?: boolean;
}

export const useAlert = () => {
  const context = useContext(AlertContext);

  if (!context) {
    throw new Error('useAlert must be used within AlertProvider');
  }

  const alert = async (message: string, options?: UseAlertOptions) => {
    return context.showAlert({
      message,
      type: options?.type || 'info',
      title: options?.title,
      confirmText: options?.confirmText || 'OK',
    });
  };

  const confirm = async (
    message: string,
    onConfirm?: () => void | Promise<void>,
    options?: UseAlertOptions
  ) => {
    return context.showConfirm({
      message,
      type: options?.type || 'warning',
      title: options?.title || 'Confirmação',
      confirmText: options?.confirmText || 'Confirmar',
      cancelText: options?.cancelText || 'Cancelar',
      onConfirm,
      isDangerous: options?.isDangerous,
    });
  };

  const success = async (message: string, title?: string) => {
    return context.showAlert({
      message,
      type: 'success',
      title: title || 'Sucesso!',
      confirmText: 'OK',
    });
  };

  const error = async (message: string, title?: string) => {
    return context.showAlert({
      message,
      type: 'error',
      title: title || 'Erro!',
      confirmText: 'OK',
    });
  };

  return { alert, confirm, success, error };
};
