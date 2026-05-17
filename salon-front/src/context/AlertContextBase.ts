import { createContext } from 'react';

export type AlertType = 'success' | 'error' | 'warning' | 'info';

export interface AlertConfig {
  title?: string;
  message: string;
  type?: AlertType;
  confirmText?: string;
  cancelText?: string;
  onConfirm?: () => void | Promise<void>;
  onCancel?: () => void;
  isDangerous?: boolean;
}

export interface AlertContextType {
  showAlert: (config: AlertConfig) => Promise<boolean>;
  showConfirm: (config: AlertConfig) => Promise<boolean>;
  hideAlert: () => void;
}

export const AlertContext = createContext<AlertContextType | undefined>(undefined);
