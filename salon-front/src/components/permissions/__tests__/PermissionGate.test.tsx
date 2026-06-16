import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PermissionGate } from '../PermissionGate';
import { usePermission } from '../../../hooks/usePermission';

vi.mock('../../../hooks/usePermission', () => ({
  usePermission: vi.fn(),
}));

describe('PermissionGate', () => {
  it('should render children when usePermission returns true', () => {
    vi.mocked(usePermission).mockReturnValue(true);

    render(
      <PermissionGate method="GET" endpoint="/v1/users">
        <div data-testid="allowed-content">Allowed Content</div>
      </PermissionGate>
    );

    expect(screen.getByTestId('allowed-content')).toBeInTheDocument();
    expect(screen.getByTestId('allowed-content')).toHaveTextContent('Allowed Content');
  });

  it('should not render children when usePermission returns false', () => {
    vi.mocked(usePermission).mockReturnValue(false);

    render(
      <PermissionGate method="GET" endpoint="/v1/users">
        <div data-testid="allowed-content">Allowed Content</div>
      </PermissionGate>
    );

    expect(screen.queryByTestId('allowed-content')).not.toBeInTheDocument();
  });

  it('should render fallback when usePermission returns false and fallback is provided', () => {
    vi.mocked(usePermission).mockReturnValue(false);

    render(
      <PermissionGate
        method="GET"
        endpoint="/v1/users"
        fallback={<div data-testid="fallback-content">Denied</div>}
      >
        <div data-testid="allowed-content">Allowed Content</div>
      </PermissionGate>
    );

    expect(screen.queryByTestId('allowed-content')).not.toBeInTheDocument();
    expect(screen.getByTestId('fallback-content')).toBeInTheDocument();
    expect(screen.getByTestId('fallback-content')).toHaveTextContent('Denied');
  });
});
