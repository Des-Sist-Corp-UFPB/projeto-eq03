import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, act, render } from '@testing-library/react';
import { PixPaymentModal } from '../PixPaymentModal';
import { appointmentsApi } from '../../../pages/appointments/services/appointments';

vi.mock('../../../hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      email: 'client@salao.com',
      role: 'CLIENTE',
      userId: 5,
      permissions: [],
      cpf: '12345678909',
    },
    updateUserCpf: vi.fn(),
  }),
}));

vi.mock('../../../pages/appointments/services/appointments', () => ({
  appointmentsApi: {
    findById: vi.fn(),
  },
}));

describe('PixPaymentModal Component', () => {
  const defaultProps = {
    show: true,
    onHide: vi.fn(),
    onGeneratePix: vi.fn(),
    pixQrCode: '00020101021226870014br.gov.bcb.pix2565qr.mercadopago.com/pix/v2/foo-bar-id',
    serviceName: 'Corte de Cabelo Feminino',
    price: 85.5,
    clientHasSavedCpf: true,
    clientCpfMasked: '***.***.***-09',
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

  it('renders checkbox and allows submitting with saved CPF', async () => {
    const onGeneratePixMock = vi.fn().mockResolvedValue(undefined);
    render(
      <PixPaymentModal
        {...defaultProps}
        pixQrCode={null}
        onGeneratePix={onGeneratePixMock}
      />
    );

    // Render checkbox immediately from props
    const checkbox = screen.getByRole('checkbox', { name: /Usar CPF salvo/ });
    expect(checkbox).toBeInTheDocument();
    expect(checkbox).toBeChecked();

    const generateBtn = screen.getByRole('button', { name: 'Gerar PIX' });
    await act(async () => {
      fireEvent.click(generateBtn);
    });

    expect(onGeneratePixMock).toHaveBeenCalledWith({ useSavedCpf: true, cpf: undefined });
  });

  it('allows unchecking saved CPF and typing a new CPF', async () => {
    const onGeneratePixMock = vi.fn().mockResolvedValue(undefined);
    render(
      <PixPaymentModal
        {...defaultProps}
        pixQrCode={null}
        onGeneratePix={onGeneratePixMock}
      />
    );

    // Checkbox rendered from props
    const checkbox = screen.getByRole('checkbox', { name: /Usar CPF salvo/ });
    
    // Uncheck it
    await act(async () => {
      fireEvent.click(checkbox);
    });
    expect(checkbox).not.toBeChecked();

    // Now CPF input should be visible
    const cpfInput = screen.getByPlaceholderText('000.000.000-00');
    expect(cpfInput).toBeInTheDocument();

    await act(async () => {
      fireEvent.change(cpfInput, { target: { value: '09123456752' } }); // valid CPF
    });

    const generateBtn = screen.getByRole('button', { name: 'Gerar PIX' });
    await act(async () => {
      fireEvent.click(generateBtn);
    });

    expect(onGeneratePixMock).toHaveBeenCalledWith({ useSavedCpf: false, cpf: '09123456752' });
  });

  it('starts short polling when step is qr and appointmentId is provided', async () => {
    vi.useFakeTimers();
    const findByIdMock = vi.mocked(appointmentsApi.findById).mockResolvedValue({
      id: 1,
      paymentStatus: 'PENDING',
    } as any);

    render(
      <PixPaymentModal
        {...defaultProps}
        appointmentId={1}
      />
    );

    // Should poll after interval
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });

    expect(findByIdMock).toHaveBeenCalledWith(1);
    vi.useRealTimers();
  });

  it('stops polling and shows success message when payment status is PAID', async () => {
    vi.useFakeTimers();
    const onPaymentSuccessMock = vi.fn();
    const mockPaidAppointment = {
      id: 1,
      paymentStatus: 'PAID',
    };
    vi.mocked(appointmentsApi.findById).mockResolvedValue(mockPaidAppointment as any);

    render(
      <PixPaymentModal
        {...defaultProps}
        appointmentId={1}
        onPaymentSuccess={onPaymentSuccessMock}
      />
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });

    // Check that success message is displayed
    expect(screen.getByText('Pagamento confirmado com sucesso!')).toBeInTheDocument();

    // Advance 2 seconds to wait for modal close
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });

    expect(onPaymentSuccessMock).toHaveBeenCalledWith(mockPaidAppointment);
    expect(defaultProps.onHide).toHaveBeenCalled();
    vi.useRealTimers();
  });
});
