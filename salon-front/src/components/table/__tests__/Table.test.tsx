import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Table } from '../Table';

interface TestItem {
  id: number;
  name: string;
  role: string;
}

const columns = [
  { key: 'name', label: 'Nome' },
  { key: 'role', label: 'Papel' },
  {
    key: 'actions',
    label: 'Ações',
    render: (item: TestItem) => <button data-testid={`btn-${item.id}`}>Action</button>,
  },
];

const mockData: TestItem[] = [
  { id: 1, name: 'Alice', role: 'ADMIN' },
  { id: 2, name: 'Bob', role: 'CLIENTE' },
];

describe('Table Component', () => {
  it('should render columns and row data correctly', () => {
    render(<Table columns={columns} data={mockData} keyExtractor={(item) => item.id} />);

    // Check headers
    expect(screen.getByText('Nome')).toBeInTheDocument();
    expect(screen.getByText('Papel')).toBeInTheDocument();
    expect(screen.getByText('Ações')).toBeInTheDocument();

    // Check cell values
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('ADMIN')).toBeInTheDocument();
    expect(screen.getByText('CLIENTE')).toBeInTheDocument();

    // Check custom renderer
    expect(screen.getByTestId('btn-1')).toBeInTheDocument();
    expect(screen.getByTestId('btn-2')).toBeInTheDocument();
  });

  it('should render empty message when data is empty', () => {
    render(
      <Table
        columns={columns}
        data={[]}
        keyExtractor={(item) => item.id}
        emptyMessage="Nenhum dado cadastrado"
      />
    );

    expect(screen.getByText('Nenhum dado cadastrado')).toBeInTheDocument();
  });

  it('should display search input and call onSearchChange when typed in', () => {
    const handleSearchChange = vi.fn();

    render(
      <Table
        columns={columns}
        data={mockData}
        keyExtractor={(item) => item.id}
        searchTerm=""
        onSearchChange={handleSearchChange}
      />
    );

    const input = screen.getByPlaceholderText('Buscar...');
    expect(input).toBeInTheDocument();

    fireEvent.change(input, { target: { value: 'Alice' } });
    expect(handleSearchChange).toHaveBeenCalledWith('Alice');
  });

  it('should render pagination buttons and call onPageChange', () => {
    const handlePageChange = vi.fn();

    render(
      <Table
        columns={columns}
        data={mockData}
        keyExtractor={(item) => item.id}
        currentPage={2}
        totalPages={3}
        onPageChange={handlePageChange}
      />
    );

    // Check page numbers
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();

    // Click "Anterior"
    const prevButton = screen.getByText('Anterior');
    fireEvent.click(prevButton);
    expect(handlePageChange).toHaveBeenCalledWith(1);

    // Click page 3 button
    const page3Button = screen.getByText('3');
    fireEvent.click(page3Button);
    expect(handlePageChange).toHaveBeenCalledWith(3);

    // Click "Próxima"
    const nextButton = screen.getByText('Próxima');
    fireEvent.click(nextButton);
    expect(handlePageChange).toHaveBeenCalledWith(3);
  });

  it('should disable prev button on first page and next button on last page', () => {
    const handlePageChange = vi.fn();

    const { rerender } = render(
      <Table
        columns={columns}
        data={mockData}
        keyExtractor={(item) => item.id}
        currentPage={1}
        totalPages={2}
        onPageChange={handlePageChange}
      />
    );

    expect(screen.getByText('Anterior')).toBeDisabled();
    expect(screen.getByText('Próxima')).not.toBeDisabled();

    // Rerender as last page
    rerender(
      <Table
        columns={columns}
        data={mockData}
        keyExtractor={(item) => item.id}
        currentPage={2}
        totalPages={2}
        onPageChange={handlePageChange}
      />
    );

    expect(screen.getByText('Anterior')).not.toBeDisabled();
    expect(screen.getByText('Próxima')).toBeDisabled();
  });

  it('should render empty string when value is missing/falsy for a column without render function', () => {
    const incompleteData = [{ id: 1, name: 'Alice', role: undefined as any }];
    render(<Table columns={columns} data={incompleteData} keyExtractor={(item) => item.id} />);
    // Column role should render empty string, so we check that 'Alice' is there and nothing crashes
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });
});
