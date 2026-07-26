import { describe, it, expect } from 'vitest';
import { isValidCpf } from '../cpfValidator';

describe('isValidCpf', () => {
  it('returns true for a valid CPF with punctuation', () => {
    expect(isValidCpf('111.444.777-35')).toBe(true);
  });

  it('returns true for a valid CPF with only digits', () => {
    expect(isValidCpf('11144477735')).toBe(true);
  });

  it('returns false for null or undefined', () => {
    expect(isValidCpf(null)).toBe(false);
    expect(isValidCpf(undefined)).toBe(false);
  });

  it('returns false for wrong length', () => {
    expect(isValidCpf('123')).toBe(false);
    expect(isValidCpf('123456789012')).toBe(false);
  });

  it('returns false when all digits are equal', () => {
    expect(isValidCpf('11111111111')).toBe(false);
    expect(isValidCpf('00000000000')).toBe(false);
  });

  it('returns false when check digits are wrong', () => {
    expect(isValidCpf('111.444.777-36')).toBe(false);
  });
});
