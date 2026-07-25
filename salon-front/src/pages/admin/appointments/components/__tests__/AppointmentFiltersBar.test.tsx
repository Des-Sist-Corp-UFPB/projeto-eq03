import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import {
  AppointmentFiltersBar,
  emptyAppointmentFilters,
  countActiveFilters,
} from '../AppointmentFiltersBar';
import type { EmployeeData } from '../../../employees/services/employees';

const employees: EmployeeData[] = [
  { id: 1, userId: 10, name: 'Alice' },
  { id: 2, userId: 11, name: 'Bob' },
];

describe('AppointmentFiltersBar', () => {
  it('renders all filter fields', () => {
    render(
      <AppointmentFiltersBar
        filters={emptyAppointmentFilters}
        employees={employees}
        onChange={vi.fn()}
        onClear={vi.fn()}
      />
    );

    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('Profissional')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Buscar por nome...')).toBeInTheDocument();
    expect(screen.getByText('De')).toBeInTheDocument();
    expect(screen.getByText('Até')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
  });

  it('calls onChange with the changed field when a filter is edited', () => {
    const handleChange = vi.fn();
    render(
      <AppointmentFiltersBar
        filters={emptyAppointmentFilters}
        employees={employees}
        onChange={handleChange}
        onClear={vi.fn()}
      />
    );

    fireEvent.change(screen.getByPlaceholderText('Buscar por nome...'), {
      target: { value: 'Maria' },
    });
    expect(handleChange).toHaveBeenCalledWith({ clientName: 'Maria' });

    fireEvent.change(screen.getByText('Status').parentElement!.querySelector('select')!, {
      target: { value: 'DONE' },
    });
    expect(handleChange).toHaveBeenCalledWith({ status: 'DONE' });
  });

  it('calls onClear when the clear button is clicked', () => {
    const handleClear = vi.fn();
    render(
      <AppointmentFiltersBar
        filters={emptyAppointmentFilters}
        employees={employees}
        onChange={vi.fn()}
        onClear={handleClear}
      />
    );

    fireEvent.click(screen.getByText('Limpar Filtros'));
    expect(handleClear).toHaveBeenCalled();
  });

  it('does not show the active-filter badge when no filters are set', () => {
    render(
      <AppointmentFiltersBar
        filters={emptyAppointmentFilters}
        employees={employees}
        onChange={vi.fn()}
        onClear={vi.fn()}
      />
    );

    expect(screen.queryByText('1')).not.toBeInTheDocument();
  });

  it('shows the active-filter count badge when filters are applied', () => {
    render(
      <AppointmentFiltersBar
        filters={{ ...emptyAppointmentFilters, status: 'DONE', clientName: 'Maria' }}
        employees={employees}
        onChange={vi.fn()}
        onClear={vi.fn()}
      />
    );

    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('countActiveFilters counts only non-empty values', () => {
    expect(countActiveFilters(emptyAppointmentFilters)).toBe(0);
    expect(
      countActiveFilters({ ...emptyAppointmentFilters, status: 'DONE', employeeId: '3' })
    ).toBe(2);
  });
});
