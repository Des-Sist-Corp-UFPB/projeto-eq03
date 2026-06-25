import { describe, it, expect } from 'vitest';
import { maskCPF } from '../formatters';

describe('maskCPF', () => {
  it('should format and mask a valid 11-digit CPF with dots and dash', () => {
    expect(maskCPF('12345678900')).toBe('***.***.789-00');
    expect(maskCPF('123.456.789-00')).toBe('***.***.789-00');
  });

  it('should return empty string if input is empty, null or undefined', () => {
    expect(maskCPF('')).toBe('');
    expect(maskCPF(null)).toBe('');
    expect(maskCPF(undefined)).toBe('');
  });

  it('should return the original string if it is not a valid 11-digit CPF', () => {
    expect(maskCPF('123')).toBe('123');
    expect(maskCPF('123.456.78-0')).toBe('123.456.78-0');
  });
});
