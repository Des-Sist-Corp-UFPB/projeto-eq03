import { describe, it, expect } from 'vitest';
import {
  isTerminalStatus,
  isTerminalPaymentStatus,
  canCancel,
  getCancelBlockReason,
  canChangeStatus,
  getStatusChangeBlockReason,
  getValidStatusOptions,
  canChangePaymentStatus,
  getPaymentStatusChangeBlockReason,
  canGeneratePix,
} from '../appointmentRules';
import type { AppointmentForRules } from '../appointmentRules';

const apt = (
  status: string,
  paymentStatus: string | null = 'PENDING',
  pixQrCode: string | null = null
): AppointmentForRules => ({ status, paymentStatus, pixQrCode });

// ──────────────────────────────────────────────────────────────
// isTerminalStatus
// ──────────────────────────────────────────────────────────────
describe('isTerminalStatus', () => {
  it('should return true for CANCELLED', () => {
    expect(isTerminalStatus('CANCELLED')).toBe(true);
  });

  it('should return true for DONE', () => {
    expect(isTerminalStatus('DONE')).toBe(true);
  });

  it('should return false for CONFIRMED', () => {
    expect(isTerminalStatus('CONFIRMED')).toBe(false);
  });

  it('should return false for REQUESTED', () => {
    expect(isTerminalStatus('REQUESTED')).toBe(false);
  });

  it('should return false for PENDING', () => {
    expect(isTerminalStatus('PENDING')).toBe(false);
  });
});

// ──────────────────────────────────────────────────────────────
// isTerminalPaymentStatus
// ──────────────────────────────────────────────────────────────
describe('isTerminalPaymentStatus', () => {
  it('should return true for PAID', () => {
    expect(isTerminalPaymentStatus('PAID')).toBe(true);
  });

  it('should return true for CANCELLED', () => {
    expect(isTerminalPaymentStatus('CANCELLED')).toBe(true);
  });

  it('should return false for PENDING', () => {
    expect(isTerminalPaymentStatus('PENDING')).toBe(false);
  });

  it('should return false for null', () => {
    expect(isTerminalPaymentStatus(null)).toBe(false);
  });

  it('should return false for undefined', () => {
    expect(isTerminalPaymentStatus(undefined)).toBe(false);
  });
});

// ──────────────────────────────────────────────────────────────
// canCancel
// ──────────────────────────────────────────────────────────────
describe('canCancel', () => {
  it('should return true for CONFIRMED + PENDING payment', () => {
    expect(canCancel(apt('CONFIRMED', 'PENDING'))).toBe(true);
  });

  it('should return true for REQUESTED + no payment', () => {
    expect(canCancel(apt('REQUESTED', null))).toBe(true);
  });

  it('should return false if status is CANCELLED', () => {
    expect(canCancel(apt('CANCELLED', 'PENDING'))).toBe(false);
  });

  it('should return false if status is DONE', () => {
    expect(canCancel(apt('DONE', 'PENDING'))).toBe(false);
  });

  it('should return false if status is DECLINED', () => {
    expect(canCancel(apt('DECLINED', 'PENDING'))).toBe(false);
  });

  it('should return false if paymentStatus is PAID (even if CONFIRMED)', () => {
    expect(canCancel(apt('CONFIRMED', 'PAID'))).toBe(false);
  });

  it('should return false if paymentStatus is PAID and status is DONE', () => {
    expect(canCancel(apt('DONE', 'PAID'))).toBe(false);
  });
});

// ──────────────────────────────────────────────────────────────
// getCancelBlockReason
// ──────────────────────────────────────────────────────────────
describe('getCancelBlockReason', () => {
  it('should return null for cancelable appointment', () => {
    expect(getCancelBlockReason(apt('CONFIRMED', 'PENDING'))).toBeNull();
  });

  it('should return reason for CANCELLED status', () => {
    expect(getCancelBlockReason(apt('CANCELLED'))).toContain('cancelado');
  });

  it('should return reason for DONE status', () => {
    expect(getCancelBlockReason(apt('DONE'))).toContain('concluídos');
  });

  it('should return reason for DECLINED status', () => {
    expect(getCancelBlockReason(apt('DECLINED'))).toContain('recusada');
  });

  it('should return estorno reason for PAID payment', () => {
    expect(getCancelBlockReason(apt('CONFIRMED', 'PAID'))).toContain('estorno');
  });
});

// ──────────────────────────────────────────────────────────────
// canChangeStatus
// ──────────────────────────────────────────────────────────────
describe('canChangeStatus', () => {
  it('should return true for CONFIRMED + PENDING', () => {
    expect(canChangeStatus(apt('CONFIRMED', 'PENDING'))).toBe(true);
  });

  it('should return true for CONFIRMED + PAID (DONE is still allowed)', () => {
    expect(canChangeStatus(apt('CONFIRMED', 'PAID'))).toBe(true);
  });

  it('should return true for PENDING', () => {
    expect(canChangeStatus(apt('PENDING', 'PENDING'))).toBe(true);
  });

  it('should return false for CANCELLED (terminal)', () => {
    expect(canChangeStatus(apt('CANCELLED'))).toBe(false);
  });
});

// ──────────────────────────────────────────────────────────────
// getStatusChangeBlockReason
// ──────────────────────────────────────────────────────────────
describe('getStatusChangeBlockReason', () => {
  it('should return null for CONFIRMED', () => {
    expect(getStatusChangeBlockReason(apt('CONFIRMED'))).toBeNull();
  });

  it('should return reason for CANCELLED', () => {
    expect(getStatusChangeBlockReason(apt('CANCELLED'))).toContain('cancelados');
  });
});

// ──────────────────────────────────────────────────────────────
// getValidStatusOptions
// ──────────────────────────────────────────────────────────────
describe('getValidStatusOptions', () => {
  it('should return CONFIRMED and DONE options for CONFIRMED + PENDING payment', () => {
    const options = getValidStatusOptions(apt('CONFIRMED', 'PENDING'));
    const values = options.map((o) => o.value);
    expect(values).not.toContain('PENDING');
    expect(values).toContain('CONFIRMED');
    expect(values).toContain('DONE');
  });

  it('should return only current + DONE for CONFIRMED + PAID payment', () => {
    const options = getValidStatusOptions(apt('CONFIRMED', 'PAID'));
    const values = options.map((o) => o.value);
    expect(values).toContain('CONFIRMED');
    expect(values).toContain('DONE');
    expect(values).not.toContain('PENDING');
  });

  it('should return only current + DONE for CONFIRMED + CANCELLED payment', () => {
    const options = getValidStatusOptions(apt('CONFIRMED', 'CANCELLED'));
    const values = options.map((o) => o.value);
    expect(values).toContain('DONE');
    expect(values).not.toContain('PENDING');
  });
});

// ──────────────────────────────────────────────────────────────
// canChangePaymentStatus
// ──────────────────────────────────────────────────────────────
describe('canChangePaymentStatus', () => {
  it('should return true for CONFIRMED + PENDING', () => {
    expect(canChangePaymentStatus(apt('CONFIRMED', 'PENDING'))).toBe(true);
  });

  it('should return false for CANCELLED appointment', () => {
    expect(canChangePaymentStatus(apt('CANCELLED', 'PENDING'))).toBe(false);
  });

  it('should return false if paymentStatus is PAID', () => {
    expect(canChangePaymentStatus(apt('CONFIRMED', 'PAID'))).toBe(false);
  });

  it('should return false if paymentStatus is CANCELLED', () => {
    expect(canChangePaymentStatus(apt('CONFIRMED', 'CANCELLED'))).toBe(false);
  });

  it('should return false for DONE + PAID (double terminal)', () => {
    expect(canChangePaymentStatus(apt('DONE', 'PAID'))).toBe(false);
  });
});

// ──────────────────────────────────────────────────────────────
// getPaymentStatusChangeBlockReason
// ──────────────────────────────────────────────────────────────
describe('getPaymentStatusChangeBlockReason', () => {
  it('should return null for editable appointment', () => {
    expect(getPaymentStatusChangeBlockReason(apt('CONFIRMED', 'PENDING'))).toBeNull();
  });

  it('should return reason for CANCELLED appointment', () => {
    expect(getPaymentStatusChangeBlockReason(apt('CANCELLED'))).toContain('cancelados');
  });

  it('should return reason for PAID payment', () => {
    expect(getPaymentStatusChangeBlockReason(apt('CONFIRMED', 'PAID'))).toContain('confirmado');
  });

  it('should return reason for CANCELLED payment', () => {
    expect(getPaymentStatusChangeBlockReason(apt('CONFIRMED', 'CANCELLED'))).toContain('cancelado');
  });
});

// ──────────────────────────────────────────────────────────────
// canGeneratePix
// ──────────────────────────────────────────────────────────────
describe('canGeneratePix', () => {
  it('should return true for CONFIRMED + PENDING', () => {
    expect(canGeneratePix(apt('CONFIRMED', 'PENDING'))).toBe(true);
  });

  it('should return true for DONE + PENDING (pay after service)', () => {
    expect(canGeneratePix(apt('DONE', 'PENDING'))).toBe(true);
  });

  it('should return true for REQUESTED + PENDING', () => {
    expect(canGeneratePix(apt('REQUESTED', 'PENDING'))).toBe(true);
  });

  it('should return false if status is CANCELLED', () => {
    expect(canGeneratePix(apt('CANCELLED', 'PENDING'))).toBe(false);
  });

  it('should return false if paymentStatus is PAID', () => {
    expect(canGeneratePix(apt('CONFIRMED', 'PAID'))).toBe(false);
  });

  it('should return false for DONE + PAID (already paid after service)', () => {
    expect(canGeneratePix(apt('DONE', 'PAID'))).toBe(false);
  });
});
