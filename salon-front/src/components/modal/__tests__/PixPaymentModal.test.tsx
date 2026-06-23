import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, render } from '@testing-library/react';
import { PixPaymentModal } from '../PixPaymentModal';

describe('PixPaymentModal Component', () => {
  const defaultProps = {
    show: true,
    onHide: vi.fn(),
    pixQrCode: '00020101021226870014br.gov.bcb.pix2565qr.mercadopago.com/pix/v2/foo-bar-id',
    serviceName: 'Corte de Cabelo Feminino',
    price: 85.5,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
      writable: true,
      configurable: true,
    });
  });

  it('does not render when show is false', () => {
    render(<PixPaymentModal {...defaultProps} show={false} />);
    expect(screen.queryByText('Pagamento via PIX')).not.toBeInTheDocument();
  });

  it('renders all elements correctly when show is true', () => {
    render(<PixPaymentModal {...defaultProps} />);

    expect(screen.getByText('Pagamento via PIX')).toBeInTheDocument();
    expect(screen.getByText('Corte de Cabelo Feminino')).toBeInTheDocument();
    expect(screen.getByText('R$ 85.50')).toBeInTheDocument();
    expect(screen.getByText(/Abra o aplicativo do seu banco/)).toBeInTheDocument();
    
    // Check if the copy-paste input contains the PIX code
    const textarea = screen.getByRole('textbox') as HTMLTextAreaElement;
    expect(textarea.value).toBe(defaultProps.pixQrCode);
  });

  it('does not render price section if price is not provided', () => {
    render(<PixPaymentModal {...defaultProps} price={null} />);

    expect(screen.getByText('Corte de Cabelo Feminino')).toBeInTheDocument();
    expect(screen.queryByText('R$')).not.toBeInTheDocument();
  });

  it('calls onHide when close buttons are clicked', () => {
    render(<PixPaymentModal {...defaultProps} />);

    const closeButtons = screen.getAllByRole('button');
    
    // The dialog has close icon button (X) and a big "Fechar" button
    const fecharButton = screen.getByRole('button', { name: 'Fechar' });

    fireEvent.click(fecharButton);
    expect(defaultProps.onHide).toHaveBeenCalledTimes(1);

    // Let's find the X button by selecting the icon container
    const xButton = closeButtons.find(btn => btn !== fecharButton && btn.textContent === '');
    if (xButton) {
      fireEvent.click(xButton);
      expect(defaultProps.onHide).toHaveBeenCalledTimes(2);
    }
  });

  it('copies the PIX code and updates button text when copy button is clicked', async () => {
    vi.useFakeTimers();
    render(<PixPaymentModal {...defaultProps} />);

    const copyBtn = screen.getByRole('button', { name: /Copiar código/i });
    expect(copyBtn).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(copyBtn);
    });

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(defaultProps.pixQrCode);
    expect(screen.getByText('Copiado!')).toBeInTheDocument();

    // Fast forward timers to check that it reverts to Copiar código
    act(() => {
      vi.advanceTimersByTime(2000);
    });

    expect(screen.getByText('Copiar código')).toBeInTheDocument();
    vi.useRealTimers();
  });
});
