import { useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import { AlertContext } from './AlertContextBase';
import type { AlertConfig, AlertType } from './AlertContextBase';

interface AlertState extends AlertConfig {
  id: string;
  isVisible: boolean;
  isConfirm?: boolean;
}

export const AlertProvider = ({ children }: { children: ReactNode }) => {
  const [alert, setAlert] = useState<AlertState | null>(null);
  const [resolvePromise, setResolvePromise] = useState<((value: boolean) => void) | null>(null);

  const hideAlert = useCallback(() => {
    setAlert(null);
    setResolvePromise(null);
  }, []);

  const showAlert = useCallback((config: AlertConfig): Promise<boolean> => {
    return new Promise((resolve) => {
      setAlert({
        id: Date.now().toString(),
        isVisible: true,
        isConfirm: false,
        type: 'info',
        confirmText: 'OK',
        ...config,
      });
      setResolvePromise(() => resolve);
    });
  }, []);

  const showConfirm = useCallback((config: AlertConfig): Promise<boolean> => {
    return new Promise((resolve) => {
      setAlert({
        id: Date.now().toString(),
        isVisible: true,
        isConfirm: true,
        type: 'warning',
        confirmText: 'Confirmar',
        cancelText: 'Cancelar',
        ...config,
      });
      setResolvePromise(() => resolve);
    });
  }, []);

  const handleConfirm = useCallback(async () => {
    try {
      if (alert?.onConfirm) {
        await alert.onConfirm();
      }
    } finally {
      resolvePromise?.(true);
      hideAlert();
    }
  }, [alert, resolvePromise, hideAlert]);

  const handleCancel = useCallback(() => {
    if (alert?.onCancel) {
      alert.onCancel();
    }
    resolvePromise?.(false);
    hideAlert();
  }, [alert, resolvePromise, hideAlert]);

  return (
    <AlertContext.Provider
      value={{
        showAlert,
        showConfirm,
        hideAlert,
      }}
    >
      {children}
      {alert?.isVisible && (
        <AlertModal alert={alert} onConfirm={handleConfirm} onCancel={handleCancel} />
      )}
    </AlertContext.Provider>
  );
};

interface AlertModalProps {
  alert: AlertState;
  onConfirm: () => void;
  onCancel: () => void;
}

const AlertModal = ({ alert, onConfirm, onCancel }: AlertModalProps) => {
  const [isLoading, setIsLoading] = useState(false);

  const handleConfirmClick = async () => {
    setIsLoading(true);
    try {
      await onConfirm();
    } finally {
      setIsLoading(false);
    }
  };

  const getTypeColor = () => {
    const typeColorMap = {
      success: '#10b981',
      error: '#ef4444',
      warning: '#f59e0b',
      info: '#3b82f6',
    };
    return typeColorMap[alert.type || 'info'];
  };

  const getDangerousConfirmColor = () => {
    return alert.isDangerous ? '#dc2626' : '#3b82f6';
  };

  return (
    <div style={overlayStyles}>
      <div style={modalStyles}>
        {/* Icon */}
        <div style={{ ...iconContainerStyles, color: getTypeColor() }}>
          {getIcon(alert.type || 'info')}
        </div>

        {/* Title */}
        {alert.title && <h2 style={titleStyles}>{alert.title}</h2>}

        {/* Message */}
        <p style={messageStyles}>{alert.message}</p>

        {/* Buttons */}
        <div style={buttonContainerStyles}>
          {alert.isConfirm && (
            <button
              onClick={onCancel}
              disabled={isLoading}
              style={{
                ...buttonStyles,
                backgroundColor: '#f3f4f6',
                color: '#374151',
              }}
            >
              {alert.cancelText}
            </button>
          )}
          <button
            onClick={handleConfirmClick}
            disabled={isLoading}
            style={{
              ...buttonStyles,
              backgroundColor: getDangerousConfirmColor(),
              color: '#fff',
              flex: 1,
            }}
          >
            {isLoading ? '...' : alert.confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

const overlayStyles: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(0, 0, 0, 0.5)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 9999,
};

const modalStyles: React.CSSProperties = {
  backgroundColor: '#fff',
  borderRadius: '12px',
  padding: '32px',
  maxWidth: '400px',
  width: '90%',
  boxShadow: '0 20px 25px rgba(0, 0, 0, 0.15)',
  animation: 'slideIn 0.3s ease-out',
};

const iconContainerStyles: React.CSSProperties = {
  fontSize: '48px',
  marginBottom: '16px',
  textAlign: 'center',
};

const titleStyles: React.CSSProperties = {
  fontSize: '20px',
  fontWeight: '600',
  color: '#1f2937',
  marginBottom: '8px',
  margin: '0 0 8px 0',
};

const messageStyles: React.CSSProperties = {
  fontSize: '14px',
  color: '#6b7280',
  marginBottom: '24px',
  lineHeight: '1.5',
  margin: '0 0 24px 0',
};

const buttonContainerStyles: React.CSSProperties = {
  display: 'flex',
  gap: '12px',
  justifyContent: 'flex-end',
};

const buttonStyles: React.CSSProperties = {
  padding: '10px 16px',
  border: 'none',
  borderRadius: '6px',
  fontSize: '14px',
  fontWeight: '500',
  cursor: 'pointer',
  transition: 'all 0.2s ease',
  minWidth: '100px',
};

const getIcon = (type: AlertType) => {
  const icons: Record<AlertType, string> = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ⓘ',
  };
  return icons[type] ?? icons.info;
};
