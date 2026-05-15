import type { ReactNode } from 'react';
import { Table as BSTable, Pagination, Form, InputGroup } from 'react-bootstrap';
import { Search } from 'lucide-react';

interface Column<T> {
  key: keyof T | string;
  label: string;
  render?: (item: T) => ReactNode;
}

interface TableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (item: T) => string | number;
  searchTerm?: string;
  onSearchChange?: (term: string) => void;
  currentPage?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;
  emptyMessage?: string;
}

export function Table<T>({
  columns,
  data,
  keyExtractor,
  searchTerm,
  onSearchChange,
  currentPage = 1,
  totalPages = 1,
  onPageChange,
  emptyMessage = 'Nenhum registro encontrado.'
}: TableProps<T>) {
  return (
    <div className="table-container">
      {onSearchChange && (
        <div className="mb-3 d-flex justify-content-end">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text><Search size={18} /></InputGroup.Text>
            <Form.Control
              placeholder="Buscar..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
            />
          </InputGroup>
        </div>
      )}

      <BSTable striped bordered hover responsive>
        <thead className="table-dark">
          <tr>
            {columns.map((col, index) => (
              <th key={String(col.key) + index}>{col.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="text-center py-4 text-muted">
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((item) => (
              <tr key={keyExtractor(item)}>
                {columns.map((col, index) => (
                  <td key={String(col.key) + index} className="align-middle">
                    {col.render ? col.render(item) : String(item[col.key as keyof T] || '')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </BSTable>

      {totalPages > 1 && onPageChange && (
        <div className="d-flex justify-content-center mt-4">
          <Pagination>
            <Pagination.Prev 
              disabled={currentPage === 1} 
              onClick={() => onPageChange(currentPage - 1)} 
            />
            {Array.from({ length: totalPages }).map((_, i) => (
              <Pagination.Item 
                key={i + 1} 
                active={i + 1 === currentPage}
                onClick={() => onPageChange(i + 1)}
              >
                {i + 1}
              </Pagination.Item>
            ))}
            <Pagination.Next 
              disabled={currentPage === totalPages} 
              onClick={() => onPageChange(currentPage + 1)} 
            />
          </Pagination>
        </div>
      )}
    </div>
  );
}
